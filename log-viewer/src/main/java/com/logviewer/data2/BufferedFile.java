package com.logviewer.data2;

import com.logviewer.utils.Utils;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

import static com.logviewer.data2.ParserConfig.WINDOW_SIZE;
import static com.logviewer.data2.ParserConfig.WINDOW_SIZE_BITS;

public class BufferedFile {

    private final SeekableByteChannel channel;

    private static final int ARRAY_LENGTH = WINDOW_SIZE * 2;

    private byte[] array = new byte[ARRAY_LENGTH];

    private final ByteBuffer buf = ByteBuffer.wrap(array);

    private long loadedPage;
    private int loadedPageCount;

    private final long size;

    public BufferedFile(SeekableByteChannel channel, long size) {
        this.channel = channel;
        this.size = size;
    }

    public boolean search(long position, SearchConsumer consumer) throws IOException {
        if (position < 0)
            throw new IllegalArgumentException();

        if (position > size)
            throw new EOFException();

        while (true) {
            if (position == size) {
                consumer.end(position);
                return true;
            }

            long page = position >>> WINDOW_SIZE_BITS;
            loadOnePage(page);

            long winOffset = loadedPage << WINDOW_SIZE_BITS;
            long winEnd = Math.min(winOffset + WINDOW_SIZE * loadedPageCount, size);

            assert winOffset <= position && position < winEnd;
            if (!consumer.data(array, (int)(position - winOffset), (int)(winEnd - position), position))
                return false;

            position = winEnd;
        }
    }

    public boolean searchBack(long position, SearchConsumer consumer) throws IOException {
        if (position < 0)
            throw new IllegalArgumentException();

        if (position > size)
            throw new EOFException();

        while (position > 0) {
            long page = (position - 1) >>> WINDOW_SIZE_BITS;
            loadOnePage(page);

            long winOffset = loadedPage << WINDOW_SIZE_BITS;
            long winEnd = Math.min(winOffset + WINDOW_SIZE * loadedPageCount, size);

            assert winOffset <= (position - 1) && (position - 1) < winEnd;
            if (!consumer.data(array, 0, (int)(position - winOffset), winOffset))
                return false;

            position = winOffset;
        }

        consumer.end(0);

        return true;
    }

    private void loadOnePage(long page) throws IOException {
        if (loadedPageCount == 1) {
            if (loadedPage == page)
                return;

            if (loadedPage + 1 == page) {
                loadSecondPage();
                return;
            }
        }
        else if (loadedPageCount == 2 && (loadedPage == page || loadedPage + 1 == page)) {
            return;
        }

        long winPos = page << WINDOW_SIZE_BITS;

        buf.position(0);
        buf.limit((int) Math.min(size - winPos, WINDOW_SIZE));

        channel.position(winPos);

        Utils.readFully(channel, buf);

        this.loadedPage = page;
        loadedPageCount = 1;
    }

    private void loadTwoPages(long page) throws IOException {
        long winPos = page << WINDOW_SIZE_BITS;

        buf.position(0);
        buf.limit((int) Math.min(size - winPos, WINDOW_SIZE * 2));
        assert buf.limit() > WINDOW_SIZE;

        channel.position(winPos);

        Utils.readFully(channel, buf);

        this.loadedPage = page;
        loadedPageCount = 2;
    }

    private void loadSecondPage() throws IOException {
        assert loadedPageCount == 1;

        long winPos = (loadedPage + 1) << WINDOW_SIZE_BITS;

        buf.limit(WINDOW_SIZE + (int) Math.min(size - winPos, WINDOW_SIZE));
        buf.position(WINDOW_SIZE);

        channel.position(winPos);

        Utils.readFully(channel, buf);

        loadedPageCount = 2;
    }

    public ByteBuffer read(long position, long length) throws IOException {
        if (length > WINDOW_SIZE || position < 0 || length < 0)
            throw new IllegalArgumentException();

        if (length == 0)
            return Utils.EMPTY_BYTE_BUFFER;

        if (position + length > size)
            throw new EOFException();

        long startPage = position >>> WINDOW_SIZE_BITS;
        long endPage = (position + length - 1) >>> WINDOW_SIZE_BITS;

        if (startPage == endPage) {
            loadOnePage(startPage);
        }
        else {
            assert startPage + 1 == endPage;

            if (loadedPageCount == 0) {
                loadTwoPages(startPage);
            }
            else if (loadedPageCount == 1) {
                if (loadedPage == startPage) {
                    loadSecondPage();
                }
                else {
                    loadTwoPages(startPage);
                }
            }
            else if (loadedPageCount == 2) {
                if (loadedPage != startPage) {
                    loadTwoPages(startPage);
                }
            }
            else {
                assert false;
            }
        }

        long winPos = loadedPage << WINDOW_SIZE_BITS;
        int newPosition = (int) (position - winPos);
        buf.limit((int) (newPosition + length));
        buf.position(newPosition);
        return buf;
    }

    public boolean loadPrevLine(Line line) throws IOException {
        return loadPrevLine(line, line.getStart());
    }

    public boolean loadPrevLine(Line line, long prevLineStart) throws IOException {
        if (prevLineStart == 0)
            return false;

        class PrevLineSearcher extends Search10Consumer {
            private boolean was10;
            private boolean was13;

            private long firstNonLBPosition = -1;

            @Override
            public int searchIndex(byte[] buf, int bufOffset, int length, long position) {
                if (firstNonLBPosition == -1) {
                    assert length > 0;

                    if (!was10 && buf[bufOffset + length - 1] == 10) {
                        was10 = true;
                        length--;

                        if (length == 0)
                            return -1;
                    }

                    if (!was13 && buf[bufOffset + length - 1] == 13) {
                        was13 = true;
                        length--;

                        if (length == 0)
                            return -1;
                    }

                    if (!was10 && buf[bufOffset + length - 1] == 10) {
                        was10 = true;
                        length--;

                        if (length == 0)
                            return -1;
                    }

                    firstNonLBPosition = position + length;
                }

                for (int i = bufOffset + length - 1; i >= bufOffset; i--) {
                    if (buf[i] == 10) {
                        return i;
                    }
                }
                return -1;
            }
        }

        PrevLineSearcher searcher = new PrevLineSearcher();

        searchBack(prevLineStart, searcher);

        if (searcher.firstNonLBPosition == -1) {
            assert searcher.end == 0;

            line.start = 0;
            line.end = 0;
            line.buf = Utils.EMPTY_BYTE_ARRAY;
            line.bufOffset = 0;
            line.dataLength = 0;
            return true;
        }

        line.start = searcher.res == -1 ? 0 : searcher.res + 1;
        line.end = searcher.firstNonLBPosition;

        long readLength = Math.min(line.end - line.start, ParserConfig.MAX_LINE_LENGTH);

        ByteBuffer byteBuffer = read(line.start, readLength);

        line.buf = byteBuffer.array();
        line.bufOffset = byteBuffer.position();
        line.dataLength = byteBuffer.remaining();
        line.trim13();

        return true;
    }

    public boolean loadNextLine(Line line, long prevLineEnd) throws IOException {
        class NextLineSearcher extends Search10Consumer {
            private boolean was10;
            private boolean was13;

            private long firstNonLBPosition = -1;

            @Override
            public int searchIndex(byte[] buf, int bufOffset, int length, long position) {
                if (firstNonLBPosition == -1) {
                    assert length > 0;

                    if (!was10 && buf[bufOffset] == 10) {
                        was10 = true;
                        bufOffset++;
                        length--;
                        position++;

                        if (length == 0)
                            return -1;
                    }

                    if (!was13 && buf[bufOffset] == 13) {
                        was13 = true;
                        bufOffset++;
                        length--;
                        position++;

                        if (length == 0)
                            return -1;
                    }

                    if (!was10 && buf[bufOffset] == 10) {
                        was10 = true;
                        bufOffset++;
                        length--;
                        position++;

                        if (length == 0)
                            return -1;
                    }

                    firstNonLBPosition = position;
                }

                return super.searchIndex(buf, bufOffset, length, position);
            }
        }

        NextLineSearcher searcher = new NextLineSearcher();

        search(prevLineEnd, searcher);

        if (searcher.firstNonLBPosition == -1) {
            assert searcher.end != -1;
            if (searcher.end == prevLineEnd)
                return false;

            line.start = searcher.end;
            line.end = searcher.end;
            line.buf = Utils.EMPTY_BYTE_ARRAY;
            line.bufOffset = 0;
            line.dataLength = 0;
            return true;
        }

        long lineEnd;
        if (searcher.res == -1) {
            assert searcher.end != -1;
            lineEnd = searcher.end;
        }
        else {
            lineEnd = searcher.res;
        }

        line.start = searcher.firstNonLBPosition;
        line.end = lineEnd;

        long readLength = Math.min(lineEnd - line.start, ParserConfig.MAX_LINE_LENGTH);

        ByteBuffer byteBuffer = read(line.start, readLength);

        line.buf = byteBuffer.array();
        line.bufOffset = byteBuffer.position();
        line.dataLength = byteBuffer.remaining();
        line.trim13();
        
        return true;
    }

    public boolean loadNextLine(Line line) throws IOException {
        return loadNextLine(line, line.end);
    }

    public void loadLine(Line line, long position) throws IOException {
        long lineEnd;

        Search10Consumer forwardSearch = new Search10Consumer();
        search(position, forwardSearch);

        if (forwardSearch.res != -1) {
            lineEnd = forwardSearch.res;
        }
        else {
            lineEnd = forwardSearch.end;
        }

        IndexSearchConsumer backSearch = new IndexSearchConsumer() {
            @Override
            public int searchIndex(byte[] buf, int bufOffset, int length, long position) {
                for (int i = bufOffset + length - 1; i >= bufOffset; i--) {
                    if (buf[i] == 10) {
                        return i;
                    }
                }
                return -1;
            }
        };

        searchBack(position, backSearch);

        line.start = backSearch.res == -1 ? 0 : backSearch.res + 1;
        line.end = lineEnd;

        long readLength = Math.min(lineEnd - line.start, ParserConfig.MAX_LINE_LENGTH);

        ByteBuffer byteBuffer = read(line.start, readLength);

        line.buf = byteBuffer.array();
        line.bufOffset = byteBuffer.position();
        line.dataLength = byteBuffer.remaining();
        line.trim13();
    }

    public interface SearchConsumer {
        boolean data(byte[] buf, int bufOffset, int length, long position);

        default void end(long position) {

        }
    }

    public static class Search10Consumer extends IndexSearchConsumer {
        @Override
        public int searchIndex(byte[] buf, int bufOffset, int length, long position) {
            for (int i = bufOffset, end = bufOffset + length; i < end; i++) {
                if (buf[i] == 10) {
                    return i;
                }
            }
            return -1;
        }
    }

    public static abstract class IndexSearchConsumer implements SearchConsumer {

        long res = -1;
        long end = -1;

        public final boolean data(byte[] buf, int bufOffset, int length, long position) {
            int res = searchIndex(buf, bufOffset, length, position);

            if (res >= 0) {
                this.res = res - bufOffset + position;
                return false;
            }

            return true;
        }

        public abstract int searchIndex(byte[] buf, int bufOffset, int length, long position);

        @Override
        public void end(long position) {
            this.end = position;
        }
    }

    public static class Line {
        private long start;
        private long end;

        private byte[] buf = Utils.EMPTY_BYTE_ARRAY;
        private int bufOffset;
        private int dataLength;

        public Line() {

        }

        public Line(String s) {
            buf = s.getBytes();
            start = 0;
            end = buf.length;
            dataLength = buf.length;
        }

        public long getStart() {
            return start;
        }

        public long getEnd() {
            return end;
        }

        public byte[] getBuf() {
            return buf;
        }

        public int getBufOffset() {
            return bufOffset;
        }

        public int getDataLength() {
            return dataLength;
        }

        private void trim13() {
            if (dataLength > 0 && buf[bufOffset + dataLength - 1] == 13) {
                end--;
                dataLength--;
            }
        }

        @Override
        public String toString() {
            return new String(buf, bufOffset, dataLength) + " [" + start + ", " + end + ']';
        }
    }
}
