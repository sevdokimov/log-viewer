package com.logviewer.files;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FileTypesTest {

    @Test
    public void fileTypeDetection() {
        assertEquals(FileTypes.LOG, FileTypes.detectType("/var/log/syslog"));
        assertEquals(FileTypes.LOG, FileTypes.detectType("/var/log/syslog.1"));
        assertEquals(FileTypes.LOG, FileTypes.detectType("/var/log/syslog.100"));
        assertEquals(FileTypes.UNKNOWN, FileTypes.detectType("/var/syslog"));
        assertEquals(FileTypes.UNKNOWN, FileTypes.detectType("/var/log/zzzz"));
        
        assertEquals(FileTypes.LOG, FileTypes.detectType("aaaa/aaa.log"));
        assertEquals(FileTypes.LOG, FileTypes.detectType("aaaa/aaa.log.1"));
        assertEquals(FileTypes.LOG, FileTypes.detectType("aaaa/aaa.log.10"));
        assertEquals(FileTypes.LOG, FileTypes.detectType("aaaa/aaa.log.54"));
        assertEquals(FileTypes.LOG, FileTypes.detectType("aaaa/aaa.log.1990-11-01"));
        assertEquals(FileTypes.LOG, FileTypes.detectType("aaaa/aaa.log.2012-11-21"));
        assertEquals(FileTypes.LOG, FileTypes.detectType("aaaa/aaa.log.2012.11.21"));

        assertEquals(FileTypes.UNKNOWN, FileTypes.detectType("aaaa/aaa.log.2012-11.21"));

        assertEquals(FileTypes.UNKNOWN, FileTypes.detectType("aaaa/aaa.zzz"));
        assertEquals(FileTypes.UNKNOWN, FileTypes.detectType("aaaa/aaa.log.z"));

        assertEquals(FileTypes.GZ, FileTypes.detectType("aaaa/aaa.log.gz"));

        assertEquals(FileTypes.JAVA, FileTypes.detectType("/aaa.java"));
    }
}