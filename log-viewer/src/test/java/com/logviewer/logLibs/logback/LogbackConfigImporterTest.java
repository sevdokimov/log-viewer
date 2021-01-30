package com.logviewer.logLibs.logback;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class LogbackConfigImporterTest {

    @Test
    public void patchProcessId() {
        String pid = LogbackConfigImporter.getProcessId();
        assert pid != null && pid.length() >= 3;

        checkNotChanged("%logger{" + pid + "}");
        checkNotChanged("%logger{" + pid + "} " + pid);
        checkNotChanged(pid + " + %logger " + pid);
        checkNotChanged("%logger 1" + pid);
        checkNotChanged("%logger " + pid + " %"); // invalid pattern

        assertThat(LogbackConfigImporter.patchPatternWithProcessId("%logger " + pid + "%d"), is("%logger %processId%d"));
    }

    private void checkNotChanged(String pattern) {
        String res = LogbackConfigImporter.patchPatternWithProcessId(pattern);
        assertThat(res, is(pattern));
    }
}