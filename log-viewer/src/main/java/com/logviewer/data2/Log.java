package com.logviewer.data2;

import com.logviewer.api.LvFileAccessManager;
import com.logviewer.filters.RecordPredicate;
import com.logviewer.utils.*;
import com.logviewer.web.session.*;
import com.logviewer.web.session.tasks.SearchPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;

import static com.logviewer.files.FileTypes.GZ;

public class Log implements LogView {

    private static final Logger LOG = LoggerFactory.getLogger(Log.class);

    public static Function<String, String> DEFAULT_ID_GENERATOR = path -> {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            Utils.putUnencodedChars(digest, Utils.LOCAL_HOST_NAME);
            digest.update((byte) '|');
            Utils.putUnencodedChars(digest, path);

            long hash = ByteBuffer.wrap(digest.digest()).getLong();

            return Long.toHexString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    };

    public static Function<String, String> LOG_ID_GENERATOR = DEFAULT_ID_GENERATOR;

    public static final long CHANGE_NOTIFICATION_TIMEOUT = 50;

    private final Object logChangedTaskKey = new Object();

    private Path file;

    private final String id;

    private final LogFormat format;

    private final Charset encoding;

    private long cachedHashTimestamp;
    private String cachedHash;

    private final ExecutorService executor;

    @Autowired
    private LvTimer timer;
    @Autowired
    private FileWatcherService fileWatcherService;
    @Autowired
    private LvFileAccessManager accessManager;
    @Value("${log-viewer.parser.max-unparsable-block-size:2097152}") // 2Mb
    private long unparsableBlockMaxSize;
    @Value("${log-viewer.unpack-archive:false}")
    private boolean unpackArchive;

    private final MultiListener<Consumer<FileAttributes>> changeListener = new MultiListener<>(this::createFileListener);

    private LogIndex logIndex;

    public Log(@NonNull Path path, @NonNull LogFormat format, @NonNull ExecutorService executor) {
        file = path;
        this.format = LvGsonUtils.copy(format);
        this.executor = executor;

        encoding = this.format.getCharset() == null ? Charset.defaultCharset() : this.format.getCharset();

        id = LOG_ID_GENERATOR.apply(path.toString());
    }

    @Override
    public String getId() {
        return id;
    }

    public Path getFile() {
        return file;
    }

    @Override
    public LogPath getPath() {
        return new LogPath(null, file.toString());
    }

    @Override
    public String getHostname() {
        return Utils.LOCAL_HOST_NAME;
    }

    @Override
    public LogFormat getFormat() {
        return format;
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    private LogRecord createUnparsedRecord(BufferedFile buf, long start, long end) throws IOException {
        long readLength = Math.min(end - start, ParserConfig.MAX_LINE_LENGTH);

        ByteBuffer b = buf.read(start, readLength);

        String text = Utils.toString(b, encoding);

        return LogRecord.createUnparsedRecord(text, 0, start, end, readLength < end - start).setLogId(id);
    }

    public Snapshot createSnapshot() {
        while (true) {
            try {
                return new LogSnapshot();
            } catch (LogCrashedException ignored) {

            }
        }
    }

    private void decompressAndCopyGZipFile() throws IOException {

        File tempFile = File.createTempFile("log-viewer-", "-" + file.getName(file.getNameCount() - 1) + ".tmp");
        tempFile.deleteOnExit();

        try (GZIPInputStream gis = new GZIPInputStream(
                Files.newInputStream(file.toFile().toPath()));
             FileOutputStream fos = new FileOutputStream(tempFile)) {

            byte[] buffer = new byte[1024 * 10];
            int len;
            while ((len = gis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }

        this.file = tempFile.toPath();
    }

    public class LogSnapshot implements Snapshot {

        private final long size;
        private final long lastModification;
        private final IOException error;
        private final String hash;

        private SeekableByteChannel channel;
        private BufferedFile buf;

        private final LogIndex logIndex;

//        private Exception stacktrace = new Exception();

        LogSnapshot() throws LogCrashedException {
            long size = 0;
            long lastModification = 0;
            IOException error = null;
            String hash = null;
            LogIndex logIndex = null;

            synchronized (Log.this) {
                boolean success = false;

                try {
                    if (!file.isAbsolute())
                        throw new NoSuchFileException(file.toString());

                    if (!accessManager.isFileVisible(file)) {
                        throw new DirectoryNotVisibleException(file.toString(), accessManager.errorMessage(file));
                    }

                    BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);

                    if (!attrs.isRegularFile())
                        throw new IOException("Not a file");

                    size = attrs.size();
                    lastModification = attrs.lastModifiedTime().toMillis();

                    if (cachedHashTimestamp == lastModification) {
                        hash = cachedHash;
                    } else {
                        hash = calculateHash(size);

                        if (!hash.equals(cachedHash)) {
                            Log.this.logIndex = new LogIndex();
                        }

                        cachedHash = hash;
                        cachedHashTimestamp = lastModification;
                    }

                    logIndex = Log.this.logIndex;

                    success = true;
                } catch (IOException e) {
                    error = e;
                } finally {
                    if (!success)
                        Utils.closeQuietly(this);
                }
            }

            this.size = size;
            this.lastModification = lastModification;
            this.error = error;
            this.hash = hash;
            this.logIndex = logIndex;
        }

        @Override
        public long getSize() {
            return size;
        }

        public long getLastModification() {
            return lastModification;
        }

        private SeekableByteChannel getChannel() throws IOException {
            if (channel == null) {
                if (error != null)
                    throw new IOException(error);

                if (unpackArchive && GZ.getPattern().matcher(file.toString()).matches())
                    decompressAndCopyGZipFile();

                channel = Files.newByteChannel(file, StandardOpenOption.READ);
            }

            buf = new BufferedFile(channel, size);

            return channel;
        }

        private BufferedFile getBuffer() throws IOException {
            getChannel();

            return buf;
        }

        private void assertUnparsedBlockSize(long blockStart, long blockEnd) throws IncorrectFormatException {
            assert blockStart <= blockEnd;

            if (blockEnd - blockStart > unparsableBlockMaxSize)
                throw new IncorrectFormatException(file.toString(), blockStart, blockEnd, format);
        }

        private long findUnparsedEnd(BufferedFile buf, LogReader tmpReader, long lastLineEnd) throws IOException {
            long position = lastLineEnd;

            BufferedFile.Line line = new BufferedFile.Line();

            while (true) {
                if (!buf.loadNextLine(line, position))
                    break;

                if (tmpReader.parseRecord(line))
                    break;

                position = line.getEnd();

                assertUnparsedBlockSize(lastLineEnd, position);
            }

            return position;
        }

        private long findParsedBefore(BufferedFile buf, LogReader reader, BufferedFile.Line line, long lastLineStart) throws IOException {
            long position = lastLineStart;

            while (true) {
                if (!buf.loadPrevLine(line, position)) {
                    reader.clear();
                    return position;
                }

                if (reader.parseRecord(line))
                    return position;

                position = line.getStart();

                assertUnparsedBlockSize(position, lastLineStart);
            }
        }

        private void appendTail(BufferedFile buf, LogReader reader, long unparsedStart, long unparsedEnd) throws IOException {
            long tailLength = unparsedEnd - unparsedStart;
            long readLength = Math.min(tailLength, ParserConfig.MAX_LINE_LENGTH);

            ByteBuffer b = buf.read(unparsedStart, readLength);

            reader.appendTail(b.array(), b.position(), b.remaining(), tailLength);
        }

        @Override
        public boolean processRecordsBack(long position, boolean fromPrevLine, Predicate<LogRecord> consumer) throws IOException {
            if (position < 0)
                throw new IllegalArgumentException();

            if (error != null)
                throw error;

            if (position > size)
                position = size;

            if (size == 0)
                return true;

            BufferedFile buf = getBuffer();

            BufferedFile.Line firstLine = new BufferedFile.Line();

            if (fromPrevLine) {
                if (!buf.loadPrevLine(firstLine, position))
                    return true;
            } else {
                buf.loadLine(firstLine, position);
            }

            LogReader reader = format.createReader();
            LogReader tmpReader = format.createReader();

            long lastProcessedLineStart;

            if (!reader.parseRecord(firstLine)) {
                long unparsedEnd = findUnparsedEnd(buf, tmpReader, firstLine.getEnd());

                BufferedFile.Line line = new BufferedFile.Line();

                long unparsedStart = findParsedBefore(buf, reader, line, firstLine.getStart());
                if (!reader.hasParsedRecord()) {
                    return consumer.test(createUnparsedRecord(buf, unparsedStart, unparsedEnd));
                }

                lastProcessedLineStart = line.getStart();

                if (reader.canAppendTail()) {
                    appendTail(buf, reader, line.getEnd(), unparsedEnd);
                } else {
                    if (!consumer.test(createUnparsedRecord(buf, unparsedStart, unparsedEnd)))
                        return false;
                }
            } else {
                lastProcessedLineStart = firstLine.getStart();

                if (reader.canAppendTail()) {
                    long unparsedEnd = findUnparsedEnd(buf, tmpReader, firstLine.getEnd());

                    if (unparsedEnd != firstLine.getEnd()) {
                        appendTail(buf, reader, firstLine.getEnd(), unparsedEnd);
                    }
                }
            }

            if (!consumer.test(reader.buildRecord().setLogId(id)))
                return false;

            BufferedFile.Line line = new BufferedFile.Line();

            while (true) {
                if (!buf.loadPrevLine(line, lastProcessedLineStart))
                    return true;

                if (!reader.parseRecord(line)) {
                    long unparsedEnd = line.getEnd();

                    long unparsedStart = findParsedBefore(buf, reader, line, line.getStart());

                    if (!reader.hasParsedRecord()) {
                        return consumer.test(createUnparsedRecord(buf, unparsedStart, unparsedEnd));
                    }

                    if (reader.canAppendTail()) {
                        appendTail(buf, reader, line.getEnd(), unparsedEnd);
                    } else {
                        if (!consumer.test(createUnparsedRecord(buf, unparsedStart, unparsedEnd)))
                            return false;
                    }
                }

                if (!consumer.test(reader.buildRecord().setLogId(id)))
                    return false;

                lastProcessedLineStart = line.getStart();
            }
        }

        @Override
        public boolean processRecords(long position, boolean fromNextLine, Predicate<LogRecord> consumer) throws IOException {
            if (position < 0)
                throw new IllegalArgumentException();

            if (error != null)
                throw error;

            if (position > size)
                return true;


            if (size == 0)
                return true;

            BufferedFile buf = getBuffer();

            BufferedFile.Line line = new BufferedFile.Line();

            if (fromNextLine) {
                if (!buf.loadNextLine(line, position))
                    return true;
            } else {
                buf.loadLine(line, position);
            }

            LogReader reader = format.createReader();
            LogReader forwardReader = format.createReader();

            long selectedLineEnd = line.getEnd();

            if (!reader.parseRecord(line)) {
                long unparsedBlockStart = findParsedBefore(buf, reader, line, line.getStart());

                if (reader.hasParsedRecord()) {
                    long parsedLineEnd = line.getEnd();

                    long p = selectedLineEnd;

                    long unparsedEnd;

                    while (true) {
                        if (!buf.loadNextLine(line, p)) {
                            unparsedEnd = p;
                            break;
                        }

                        if (forwardReader.parseRecord(line)) {
                            unparsedEnd = p;
                            break;
                        }

                        p = line.getEnd();
                    }

                    if (reader.canAppendTail()) {
                        appendTail(buf, reader, parsedLineEnd, unparsedEnd);

                        if (!consumer.test(reader.buildRecord().setLogId(id)))
                            return false;
                    } else {
                        if (!consumer.test(createUnparsedRecord(buf, unparsedBlockStart, unparsedEnd)))
                            return false;
                    }

                    if (!forwardReader.hasParsedRecord()) {
                        return true; // End of file.
                    }

                    LogReader tmp = reader;
                    reader = forwardReader;
                    forwardReader = tmp;
                } else {
                    long p = selectedLineEnd;

                    while (true) {
                        if (!buf.loadNextLine(line, p)) {
                            // Log has no parsed lines
                            return consumer.test(createUnparsedRecord(buf, unparsedBlockStart, p));
                        }

                        if (reader.parseRecord(line)) {
                            if (!consumer.test(createUnparsedRecord(buf, unparsedBlockStart, p)))
                                return false;

                            break;
                        }

                        p = line.getEnd();

                        assertUnparsedBlockSize(unparsedBlockStart, p);
                    }
                }
            }


            while (true) {
                assert reader.hasParsedRecord();

                long parsedLineEnd = line.getEnd();
                long unparsedStart = -1;

                while (true) {
                    long prevEnd = line.getEnd();

                    boolean nexRecordParsed = false;
                    boolean hasNext;

                    hasNext = buf.loadNextLine(line);

                    if (hasNext) {
                        nexRecordParsed = forwardReader.parseRecord(line);
                    }

                    if (hasNext && !nexRecordParsed) {
                        if (unparsedStart == -1)
                            unparsedStart = line.getStart();

                        continue;
                    }

                    if (parsedLineEnd < prevEnd) {
                        assert unparsedStart != -1;

                        if (reader.canAppendTail()) {
                            appendTail(buf, reader, parsedLineEnd, prevEnd);

                            if (!consumer.test(reader.buildRecord().setLogId(id)))
                                return false;
                        } else {
                            if (!consumer.test(reader.buildRecord().setLogId(id)))
                                return false;

                            if (!consumer.test(createUnparsedRecord(buf, unparsedStart, prevEnd)))
                                return false;
                        }
                    } else {
                        if (!consumer.test(reader.buildRecord().setLogId(id)))
                            return false;
                    }

                    if (!hasNext)
                        return true;

                    break;
                }

                LogReader tmp = reader;
                reader = forwardReader;
                forwardReader = tmp;
            }
        }

        @Override
        public boolean processFromTimeBack(long timestampNanos, Predicate<LogRecord> consumer) throws IOException {
            if (error != null)
                throw error;

            Utils.assertValidTimestamp(timestampNanos);

            LogRecord record = logIndex.findRecordBound(timestampNanos, true, this);
            if (record == null)
                return true;

            if (!consumer.test(record))
                return false;

            return processRecordsBack(record.getStart(), true, consumer);
        }

        @Override
        public boolean processFromTime(long timestampNanos, Predicate<LogRecord> consumer) throws IOException {
            if (error != null)
                throw error;

            Utils.assertValidTimestamp(timestampNanos);

            LogRecord record = logIndex.findRecordBound(timestampNanos, false, this);
            if (record == null)
                return true;

            if (!consumer.test(record))
                return false;

            return processRecords(record.getEnd(), true, consumer);
        }

        @Override
        public Exception getError() {
            return error;
        }

        @Override
        public Log getLog() {
            return Log.this;
        }

        private String calculateHash(long fileSize) throws LogCrashedException, IOException {
            int hashSize = hashSize(fileSize);
            ByteBuffer buf = ByteBuffer.allocate(hashSize);

            try {
                getChannel().position(0);
                Utils.readFully(channel, buf);

                CRC32 crc = new CRC32();
                crc.update(buf.array());

                int hash = (int) crc.getValue();
                long hashWithLength = (hash & 0xffff_ffffL) | ((long) hashSize << 32);

                return Long.toHexString(hashWithLength);
            } catch (EOFException e) {
                throw new LogCrashedException();
            }
        }

        private int hashSize(long fileSize) {
            return (int) (Math.min(fileSize, 0xff) & 0xff);
        }

        @Override
        public boolean isValidHash(@NonNull String hash) {
            long tLong = Long.parseUnsignedLong(hash, 16);

            int hashSize = (int) ((tLong >>> 32) & 0xff);

            if (hashSize == hashSize(size)) { // Compare size of block used to has calculation.
                return hash.equals(this.hash);
            }

            try {
                return calculateHash(hashSize).equals(hash);
            } catch (LogCrashedException | IOException e) {
                return false;
            }
        }

        @Override
        public String getHash() {
            return hash;
        }

        @Override
        public void close() {
            if (channel != null) {
                Utils.closeQuietly(channel);
                channel = null;
            }
        }

        @Override
        protected void finalize() {
            if (channel != null) {
                Utils.closeQuietly(channel);
                LOG.error("Unclosed Log.Snapshot");
            }
        }
    }

    @Override
    public LogProcess loadRecords(RecordPredicate filter, int recordCountLimit,
                                  @Nullable Position start, boolean backward, String hash, long sizeLimit,
                                  @NonNull LogDataListener loadListener) {
        return new LocalFileRecordLoader(this::createSnapshot, executor, loadListener, start, filter, backward,
                recordCountLimit, sizeLimit, hash);
    }

    @Override
    public LogProcess createRecordSearcher(@NonNull Position start, boolean backward, RecordPredicate recordPredicate,
                                           @Nullable String hash, int recordCount, @NonNull SearchPattern searchPattern,
                                           @NonNull Consumer<SearchResult> listener) {
        return new LocalFileRecordSearcher(this::createSnapshot, executor, start, backward, recordPredicate, hash,
                recordCount, searchPattern, listener);
    }

    private void notifyLogChanged() {
        FileAttributes attr;

        try {
            attr = FileAttributes.fromPath(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (LOG.isDebugEnabled())
            LOG.debug("Sending notification about log changing {}", file);

        for (Consumer<FileAttributes> listener : changeListener.getListeners()) {
            try {
                listener.accept(attr);
            } catch (Throwable e) {
                LOG.error("Failed to notify listener", e);
            }
        }
    }

    private Destroyer createFileListener() {
        try {
            return fileWatcherService.watchDirectory(file.toAbsolutePath().getParent(), files -> {
                if (files.contains(file)) {
                    boolean scheduled = timer.scheduleTask(logChangedTaskKey, this::notifyLogChanged,
                            CHANGE_NOTIFICATION_TIMEOUT);

                    if (LOG.isDebugEnabled())
                        LOG.debug("Scheduled notification about log changes {}, [new timer task={}]", file, scheduled);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Destroyer addChangeListener(Consumer<FileAttributes> changeListener) {
        return this.changeListener.addListener(changeListener);
    }

    @Override
    public CompletableFuture<Throwable> tryRead() {
        try (Snapshot snapshot = createSnapshot()) {
            return CompletableFuture.completedFuture(snapshot.getError());
        }
    }

    @Override
    public String toString() {
        return file.toString();
    }

    /**
     * VisibleForTesting
     */
    public static void setLogIdGenerator(Function<String, String> logIdGenerator) {
        LOG_ID_GENERATOR = logIdGenerator;
    }
}
