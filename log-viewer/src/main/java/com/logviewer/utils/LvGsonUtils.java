package com.logviewer.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import com.logviewer.data2.LogFormat;
import com.logviewer.filters.*;
import com.logviewer.formats.RegexLogFormat;
import com.logviewer.formats.SimpleLogFormat;
import com.logviewer.logLibs.LoggerLibSupport;
import com.logviewer.logLibs.nginx.NginxLogFormat;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.nio.charset.Charset;

public class LvGsonUtils {

    public static final Gson GSON;

    static {
        GsonBuilder gsonBuilder = new GsonBuilder();

        gsonBuilder.setPrettyPrinting();

        RuntimeTypeAdapterFactory<LogFormat> logFormatFactory = RuntimeTypeAdapterFactory.of(LogFormat.class)
                .registerSubtype(RegexLogFormat.class)
                .registerSubtype(NginxLogFormat.class)
                .registerSubtype(SimpleLogFormat.class);

        LoggerLibSupport.getSupportedLogLibs().flatMap(LoggerLibSupport::getFormatClasses).forEach(logFormatFactory::registerSubtype);

        gsonBuilder.registerTypeAdapterFactory(logFormatFactory);

        gsonBuilder.registerTypeAdapterFactory(RuntimeTypeAdapterFactory.of(RecordPredicate.class)
                .registerSubtype(TestPredicate.class)
                .registerSubtype(NotPredicate.class)
                .registerSubtype(DatePredicate.class)
                .registerSubtype(ExceptionOnlyPredicate.class)
                .registerSubtype(SubstringPredicate.class)
                .registerSubtype(ThreadPredicate.class)
                .registerSubtype(FieldArgPredicate.class)
                .registerSubtype(JsPredicate.class)
                .registerSubtype(FieldValueSetPredicate.class)
                .registerSubtype(CompositeRecordPredicate.class));

        gsonBuilder.registerTypeAdapter(Charset.class, new TypeAdapter<Charset>() {
            @Override
            public void write(JsonWriter out, Charset value) throws IOException {
                if (value == null) {
                    out.nullValue();
                } else {
                    out.value(value.name());
                }
            }

            @Override
            public Charset read(JsonReader in) throws IOException {
                String name = in.nextString();
                return name == null ? null : Charset.forName(name);
            }
        });

        GSON = gsonBuilder.create();
    }

    private LvGsonUtils() {

    }

    public static <T> T copy(@NonNull T object) {
        String str = GSON.toJson(object);
        return (T)GSON.fromJson(str, object.getClass());
    }
}
