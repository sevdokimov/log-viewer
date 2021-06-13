package com.logviewer.utils;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class GsonNanosecondsAdapter extends TypeAdapter<Long> {
    /**
     * Length of timestamp string. It's a string like 01623601564799000000
     */
    private static final int TIME_LENGTH = 1 + 13 + 6;

    @Override
    public void write(JsonWriter out, Long nanoseconds) throws IOException {
        if (nanoseconds == null || nanoseconds <= 0) {
            out.nullValue();
            return;
        }

        String str = Long.toString(nanoseconds);

        if (str.length() < TIME_LENGTH) {
            StringBuilder sb = new StringBuilder();

            for (int i = str.length(); i < TIME_LENGTH; i++) {
                sb.append('0');
            }

            sb.append(str);
            str = sb.toString();
        }

        assert str.length() == TIME_LENGTH;

        out.value(str);
    }

    @Override
    public Long read(JsonReader in) throws IOException {
        JsonToken peek = in.peek();
        if (peek == JsonToken.NULL) {
            in.nextNull();
            return 0L;
        }

        String str = in.nextString();
        return Long.parseLong(str);
    }
}
