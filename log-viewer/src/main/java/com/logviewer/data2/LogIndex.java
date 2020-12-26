package com.logviewer.data2;

import java.io.IOException;
import java.util.function.Predicate;

public class LogIndex {

//    private static final Logger LOG = LoggerFactory.getLogger(LogIndex.class);

    private FindFirstProcessor firstRecord;
    private FindFirstProcessor lastRecord;

    private long size = 0;

//    private Cache<Long, FindFirstProcessor> cache = CacheBuilder.newBuilder()
//            .weigher((Weigher<Long, FindFirstProcessor>) (key, value) -> {
//                return (8 + 12) + (12 + recordSize(value.firstRecordWithoutTime) + recordSize(value.result));
//            })
//            .maximumWeight(700 * 1024)
//            .build();

//    private static int recordSize(@Nullable Record record) {
//        return record == null ? 0 : record.getSizeBytes();
//    }

    public synchronized Record findRecordBound(long time, boolean lastBound, Snapshot buffer) throws IOException, LogCrashedException {
        FindFirstProcessor firstRecord;
        FindFirstProcessor lastRecord;

//        Cache<Long, FindFirstProcessor> cache;

        boolean useCache;

        if (size > buffer.getSize()) { // Find without cache
            firstRecord = null;
            lastRecord = null;
            useCache = false;
//            cache = null;
        }
        else {
            firstRecord = this.firstRecord;
            lastRecord = this.lastRecord;
            useCache = true;
//            cache = this.cache;

            if (size != buffer.getSize()) {
                if (firstRecord != null) {
                    if (firstRecord.result == null || firstRecord.result.getEnd() >= size)
                        firstRecord = null;
                }

                this.size = buffer.getSize();
                this.firstRecord = firstRecord;
                this.lastRecord = lastRecord = null;
            }
        }

        if (firstRecord == null) {
            firstRecord = new FindFirstProcessor();
            buffer.processRecords(0, false, firstRecord);

            if (useCache)
                this.firstRecord = firstRecord;
        }

        if (firstRecord.result == null)
            return null;

        if (lastBound) {
            if (time < firstRecord.result.getTime())
                return null;
        }
        else {
            if (time <= firstRecord.result.getTime())
                return firstRecord.result;
        }

        if (lastRecord == null) {
            lastRecord = new FindFirstProcessor();
            buffer.processRecordsBack(buffer.getSize(), false, lastRecord);

            assert lastRecord.result != null;

            if (useCache)
                this.lastRecord = lastRecord;
        }

        if (lastBound) {
            if (time >= lastRecord.result.getTime())
                return lastRecord.result;
        }
        else {
            if (time > lastRecord.result.getTime())
                return null;
        }

        Record low = firstRecord.result;
        Record high = lastRecord.result;

        while (high.getStart() - low.getEnd() > 8 * 1024) {
            long mid = (low.getEnd() + high.getStart()) >>> 1;

            FindFirstProcessor processor;

            processor = new FindFirstProcessor();
            buffer.processRecords(mid, false, processor);
            assert processor.result != null;

//            if (cache == null) {
//                processor = new FindFirstProcessor();
//                buffer.processRecords(mid, false, processor);
//                assert processor.result != null;
//            }
//            else {
//                try {
//                    processor = cache.get(mid, () -> {
//                        search++;
//                        FindFirstProcessor p = new FindFirstProcessor();
//                        buffer.processRecords(mid, false, p);
//                        assert p.result != null;
//                        return p;
//                    });
//                } catch (ExecutionException e) {
//                    throw new RuntimeException(e.getCause());
//                }
//            }

            assert processor.result.getStart() > low.getStart();
            assert processor.result.getEnd() < high.getEnd();

            if (low.getTime() > processor.result.getTime())
                throw new IOException("Incorrect record order: " + low.getMessage() + " > " + processor.result.getMessage());

            if (high.getTime() < processor.result.getTime())
                throw new IOException("Incorrect record order: " + high.getMessage() + " < " + processor.result.getMessage());
            
            if (lastBound) {
                if (time >= processor.result.getTime())
                    low = processor.result;
                else
                    high = processor.result;
            }
            else {
                if (time > processor.result.getTime())
                    low = processor.result;
                else
                    high = processor.result;
            }
        }

        Record[] res = new Record[1];

        if (lastBound) {
            buffer.processRecordsBack(high.getStart(), true, r -> {
                if (r.hasTime() && r.getTime() <= time) {
                    res[0] = r;
                    return false;
                }

                return true;
            });
        }
        else {
            buffer.processRecords(low.getEnd(), true, r -> {
                if (r.hasTime() && r.getTime() >= time) {
                    res[0] = r;
                    return false;
                }

                return true;
            });
        }

        assert res[0] != null;

        return res[0];
    }

//    private static int req = 0;
//    private static int search = 0;

    private static class FindFirstProcessor implements Predicate<Record> {

        private Record firstRecordWithoutTime;
        private Record result;

        @Override
        public boolean test(Record t) {
            if (result != null)
                throw new IllegalStateException();

            if (t.hasTime()) {
                result = t;

                return false;
            }
            else {
                if (firstRecordWithoutTime == null)
                    firstRecordWithoutTime = t;
                return true;
            }
        }
    }

}
