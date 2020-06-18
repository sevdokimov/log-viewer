package com.logviewer.data2;

import org.junit.After;
import org.junit.Test;

import java.io.EOFException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class BufferedFileTest {

    private SeekableByteChannel channel;

    @After
    public void done() throws IOException {
        if (channel != null)
            channel.close();
    }

    @Test
    public void testEmptyFile() throws IOException, URISyntaxException {
        BufferedFile file = bufferedFile("/testdata/buffered-file/empty.log");

        ByteBuffer buf = file.read(0, 0);
        assert !buf.hasRemaining();

        BufferedFile.Line line = new BufferedFile.Line();

        file.loadLine(line, 0);
        checkLine(line, 0, 0, "");

        assert !file.loadNextLine(line);
        assert !file.loadPrevLine(line);

        try {
            file.loadLine(line, 100);
            assert false;
        } catch (EOFException ignored) {
            
        }
    }

    @Test
    public void testSingleLineFile() throws IOException, URISyntaxException {
        BufferedFile file = bufferedFile("/testdata/buffered-file/single-line.log");

        BufferedFile.Line line = new BufferedFile.Line();

        file.loadLine(line, 0);
        checkLine(line, 0, 3, "abc");

        file.loadLine(line, 3);
        checkLine(line, 0, 3, "abc");

        assert !file.loadNextLine(line);
        assert !file.loadPrevLine(line);

        try {
            file.loadLine(line, 100);
            assert false;
        } catch (EOFException ignored) {

        }
    }

    @Test
    public void testF() throws IOException, URISyntaxException {
        BufferedFile file = bufferedFile("/testdata/buffered-file/a__bc_edf_.log");

        BufferedFile.Line line = new BufferedFile.Line();

        file.loadLine(line, 0);
        checkLine(line, 0, 1, "a");
        file.loadLine(line, 1);
        checkLine(line, 0, 1, "a");

        assert !file.loadPrevLine(line);

        assert file.loadNextLine(line);
        checkLine(line, 3, 5, "bc");
        assert file.loadPrevLine(line);
        checkLine(line, 0, 1, "a");

        // 2

        file.loadLine(line, 3);
        checkLine(line, 3, 5, "bc");
        file.loadLine(line, 5);
        checkLine(line, 3, 5, "bc");

        assert file.loadNextLine(line);
        checkLine(line, 6, 9, "edf");
        assert file.loadPrevLine(line);
        checkLine(line, 3, 5, "bc");

        // 3
        file.loadLine(line, 10);
        checkLine(line, 10, 10, "");
        assert !file.loadNextLine(line);
        assert file.loadPrevLine(line);
        checkLine(line, 6, 9, "edf");
    }

    private void test3LineSeparators(BufferedFile file, int separatorLength) throws IOException {
        BufferedFile.Line line = new BufferedFile.Line();

        for (int i = 0; i <= 3; i++) {
            file.loadLine(line, i * separatorLength);
            checkLine(line, i * separatorLength, i * separatorLength, "");
        }

        file.loadLine(line, 0);
        for (int i = 1; i <= 3; i++) {
            assert file.loadNextLine(line);
            checkLine(line, i * separatorLength, i * separatorLength, "");
        }
        assert !file.loadNextLine(line);

        for (int i = 2; i >= 0; i--) {
            assert file.loadPrevLine(line);
            checkLine(line, i * separatorLength, i * separatorLength, "");
        }
        assert !file.loadPrevLine(line);

        try {
            file.loadLine(line, 100);
            assert false;
        } catch (EOFException ignored) {

        }
    }

    @Test
    public void testN3() throws IOException, URISyntaxException {
        BufferedFile file = bufferedFile("/testdata/buffered-file/n3.log");
        test3LineSeparators(file, 1);
    }

    @Test
    public void testRN3() throws IOException, URISyntaxException {
        BufferedFile file = bufferedFile("/testdata/buffered-file/rn3.log");
        test3LineSeparators(file, 2);

        BufferedFile.Line line = new BufferedFile.Line();
        file.loadLine(line, 1);
        checkLine(line, 0, 0, "");

        file.loadLine(line, 3);
        checkLine(line, 2, 2, "");
    }

    private void checkLine(BufferedFile.Line line, long start, long end, String data) {
        assertEquals(start, line.getStart());
        assertEquals(end, line.getEnd());
        assertEquals(data, new String(line.getBuf(), line.getBufOffset(), line.getDataLength()));
    }

    private BufferedFile bufferedFile(String name) throws URISyntaxException, IOException {
        Path path = Paths.get(getClass().getResource(name).toURI());

        assert channel == null || !channel.isOpen();
        channel = Files.newByteChannel(path);
        return new BufferedFile(channel, Files.size(path));
    }

}
