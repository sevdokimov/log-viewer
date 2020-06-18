package com.logviewer.tests.web;

import com.logviewer.TestUtils;
import com.logviewer.api.LvFormatRecognizer;
import com.logviewer.api.LvPathResolver;
import com.logviewer.config.LvTestConfig;
import com.logviewer.data2.LogFormat;
import com.logviewer.data2.LogPath;
import com.logviewer.data2.LogReader;
import com.logviewer.mocks.TestFormatRecognizer;
import org.junit.Test;
import org.openqa.selenium.WebElement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.util.Collection;

public class ExceptionOnInitialization extends AbstractWebTestCase {

    @Test
    public void exceptionOnInitialization() {
        LogFormat badLogFormat = new BrokenFormat();

        ctx.getBean(TestFormatRecognizer.class).setFormat(badLogFormat);

        openLog("1-7.log");

        WebElement errorElement = driver.findElementByClassName("io-error");
        assert errorElement.getText().contains("java.lang.RuntimeException: Problem!!!");
    }

    public static class BrokenFormat implements LogFormat {

        @Override
        public LogReader createReader() {
            throw new RuntimeException("Problem!!!");
        }

        @Override
        public FieldDescriptor[] getFields() {
            throw new RuntimeException("Problem!!!");
        }

        @Nullable
        @Override
        public Charset getCharset() {
            throw new RuntimeException("Problem!!!");
        }

        @Override
        public boolean hasFullDate() {
            throw new RuntimeException("Problem!!!");
        }
    }

    @Configuration
    public static class MyConfig extends LvTestConfig {
        @Bean
        public LvPathResolver pathResolver() {
            return new LvPathResolver() {
                @Nullable
                @Override
                public Collection<LogPath> resolvePath(@Nonnull String pathFromHttpParameter) {
                    throw new RuntimeException("Problem!!!");
                }
            };
        }

        @Bean
        public LvFormatRecognizer formatRecognizer() {
            return path -> TestUtils.MULTIFILE_LOG_FORMAT;
        }
    }
}
