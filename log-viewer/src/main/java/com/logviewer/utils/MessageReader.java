package com.logviewer.utils;

import com.logviewer.data2.net.server.Message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

public class MessageReader {

    public static final int MAX_MESSAGE_SIZE = 5*1024*1024;

    private final ByteBuffer countBuffer = ByteBuffer.allocate(4);
    private ByteBuffer buffer;
    private boolean readingCount = true;

    public ByteBuffer getCurrentBuffer() {
        return readingCount ? countBuffer : buffer;
    }

    public Object onReceive() throws IOException, ClassNotFoundException {
        if (readingCount) {
            if (countBuffer.position() < 4) {
                return null;
            }

            int packageSize = countBuffer.getInt(0);
            if (packageSize > MAX_MESSAGE_SIZE) {
                throw new IOException("Message too big: " + packageSize);
            }

            readingCount = false;
            buffer = ByteBuffer.allocate(packageSize);
            return null;
        }
        else {
            if (buffer.hasRemaining())
                return null;

            Object res;

            try (ObjectInputStream oIn = new ObjectInputStream(new ByteArrayInputStream(buffer.array()))) {
                res = oIn.readObject();
            }

            readingCount = true;
            buffer = null;
            countBuffer.position(0);

            return res;
        }
    }

    public static void serializeMessages(OpenByteArrayOutputStream bOut, Message message) throws IOException {
        int start = bOut.size();

        bOut.write(0);
        bOut.write(0);
        bOut.write(0);
        bOut.write(0);

        try (ObjectOutputStream objOut = new ObjectOutputStream(bOut)) {
            objOut.writeObject(message);
        }

        int messageSize = bOut.size() - 4 - start;

        if (messageSize > MAX_MESSAGE_SIZE)
            throw new IllegalArgumentException("Message too big: " + messageSize);

        ByteBuffer.wrap(bOut.getBuffer(), 0, bOut.size()).putInt(start, messageSize);
    }
}
