package com.logviewer.services;

import org.junit.Before;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class LvLogManagerTest {

    @Before
    public void before() {
        LvFileAccessManagerImpl.ROOT_PROVIDER = () -> Collections.singletonList(Paths.get("/"));
    }

    @Test
    public void testRootMatching() {
        LvFileAccessManagerImpl lm = create("/");
        assertEquals(Arrays.asList(Paths.get("/")), lm.getRoots());

        assert lm.isDirectoryVisible(Paths.get("/"));
        assert lm.isDirectoryVisible(Paths.get("/aaa"));
        assert lm.isDirectoryVisible(Paths.get("/zzz/aaa"));

        assert lm.checkAccess(Paths.get("/aaa/b.log")) == null;
        assert lm.checkAccess(Paths.get("/aaa.log")) == null;
    }

    @Test
    public void testFixedPatternMatching() {
        LvFileAccessManagerImpl lm = create("/aaa/b.log");
        assertEquals(Arrays.asList(Paths.get("/aaa")), lm.getRoots());

        assert lm.isDirectoryVisible(Paths.get("/"));
        assert lm.isDirectoryVisible(Paths.get("/aaa"));
        assert !lm.isDirectoryVisible(Paths.get("/zzz"));
        assert !lm.isDirectoryVisible(Paths.get("/zzz/aaa"));
        assert !lm.isDirectoryVisible(Paths.get("/aaa/bbb"));
        assert !lm.isDirectoryVisible(Paths.get("/aaa/b.log"));

        assert lm.checkAccess(Paths.get("/aaa/b.log")) == null;
        assert lm.checkAccess(Paths.get("/aaa")) != null;
        assert lm.checkAccess(Paths.get("/")) != null;
        assert lm.checkAccess(Paths.get("/aaa/z.log")) != null;
        assert lm.checkAccess(Paths.get("/ttt/b.log")) != null;
        assert lm.checkAccess(Paths.get("/aaa/ttt/b.log")) != null;
    }

    @Test
    public void testAsteriskPatternMatching() {
        LvFileAccessManagerImpl lm = create("/aaa/*.log");
        assertEquals(Arrays.asList(Paths.get("/aaa")), lm.getRoots());

        assert lm.isDirectoryVisible(Paths.get("/"));
        assert lm.isDirectoryVisible(Paths.get("/aaa"));
        assert !lm.isDirectoryVisible(Paths.get("/zzz"));
        assert !lm.isDirectoryVisible(Paths.get("/zzz/aaa"));
        assert !lm.isDirectoryVisible(Paths.get("/aaa/bbb"));
        assert !lm.isDirectoryVisible(Paths.get("/aaa/b.log"));

        assert lm.checkAccess(Paths.get("/aaa/b.log")) == null;
        assert lm.checkAccess(Paths.get("/aaa")) != null;
        assert lm.checkAccess(Paths.get("/")) != null;
        assert lm.checkAccess(Paths.get("/aaa/z.log")) == null;
        assert lm.checkAccess(Paths.get("/aaa/foo.log")) == null;
        assert lm.checkAccess(Paths.get("/ttt/foo.log")) != null;
        assert lm.checkAccess(Paths.get("/aaa/ttt/b.log")) != null;
    }

    @Test
    public void testOpenDirectory() {
        LvFileAccessManagerImpl lm = create("/aaa/bbb/");
        assertEquals(Arrays.asList(Paths.get("/aaa/bbb")), lm.getRoots());

        assert lm.isDirectoryVisible(Paths.get("/"));
        assert lm.isDirectoryVisible(Paths.get("/aaa"));
        assert !lm.isDirectoryVisible(Paths.get("/zzz"));
        assert !lm.isDirectoryVisible(Paths.get("/zzz/aaa"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/bbb"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/bbb/ccc"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/bbb/ccc/fff"));

        assert lm.checkAccess(Paths.get("/aaa/bbb/b.log")) == null;
        assert lm.checkAccess(Paths.get("/aaa/bbb/ccc/v.log")) == null;
        assert lm.checkAccess(Paths.get("/aaa/bbb/ccc/ddddd/v.log")) == null;
        assert lm.checkAccess(Paths.get("/aaa")) != null;
        assert lm.checkAccess(Paths.get("/")) != null;
        assert lm.checkAccess(Paths.get("/aaa/z.log")) != null;
        assert lm.checkAccess(Paths.get("/aaa/bbb")) != null;
        assert lm.checkAccess(Paths.get("/ttt/bbb/foo.log")) != null;
        assert lm.checkAccess(Paths.get("/aaa/ttt/b.log")) != null;
    }

    @Test
    public void testDoubleAsteriskEnd() {
        LvFileAccessManagerImpl lm = create("/aaa/bbb/**");
        assertEquals(Arrays.asList(Paths.get("/aaa/bbb")), lm.getRoots());

        assert lm.isDirectoryVisible(Paths.get("/"));
        assert lm.isDirectoryVisible(Paths.get("/aaa"));
        assert !lm.isDirectoryVisible(Paths.get("/zzz"));
        assert !lm.isDirectoryVisible(Paths.get("/zzz/aaa"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/bbb"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/bbb/ccc"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/bbb/ccc/fff"));

        assert lm.checkAccess(Paths.get("/aaa/bbb/b.log")) == null;
        assert lm.checkAccess(Paths.get("/aaa/bbb/ccc/v.log")) == null;
        assert lm.checkAccess(Paths.get("/aaa/bbb/ccc/ddddd/v.log")) == null;
        assert lm.checkAccess(Paths.get("/aaa")) != null;
        assert lm.checkAccess(Paths.get("/")) != null;
        assert lm.checkAccess(Paths.get("/aaa/z.log")) != null;
        assert lm.checkAccess(Paths.get("/aaa/bbb")) != null;
        assert lm.checkAccess(Paths.get("/ttt/bbb/foo.log")) != null;
        assert lm.checkAccess(Paths.get("/aaa/ttt/b.log")) != null;
    }
    
    @Test
    public void testDoubleSlash() {
        LvFileAccessManagerImpl lm = create("/aaa/bbb//ccc/*.log");
        assertEquals(Arrays.asList(Paths.get("/aaa/bbb/ccc")), lm.getRoots());
        assert lm.isDirectoryVisible(Paths.get("/aaa/bbb/ccc"));
        assert lm.isDirectoryVisible(Paths.get("/aaa////bbb////ccc"));

        assert lm.checkAccess(Paths.get("/aaa////bbb////ccc/f.log")) == null;
        assert lm.checkAccess(Paths.get("/aaa/bbb/ccc/f.log")) == null;
    }

    @Test
    public void testAsteriskInDirectory() {
        LvFileAccessManagerImpl lm = create("/aaa/b*/ccc/*");
        assertEquals(Arrays.asList(Paths.get("/aaa")), lm.getRoots());

        assert lm.isDirectoryVisible(Paths.get("/"));
        assert lm.isDirectoryVisible(Paths.get("/aaa"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/bbb"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/b33"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/b33/ccc"));
        assert !lm.isDirectoryVisible(Paths.get("/aaa/zzz/ccc"));
        assert !lm.isDirectoryVisible(Paths.get("/aaa/b33/b33/ccc"));
        assert !lm.isDirectoryVisible(Paths.get("/aaa/fff"));
        assert !lm.isDirectoryVisible(Paths.get("/aaa/bbb/xxx"));
        assert !lm.isDirectoryVisible(Paths.get("/aaa/b33/ccc/ddd"));
        assert !lm.isDirectoryVisible(Paths.get("/aaa/b33/ccc/ddd/kkk"));
        assert !lm.isDirectoryVisible(Paths.get("/zzz"));
        assert !lm.isDirectoryVisible(Paths.get("/zzz/fff"));

        assert lm.checkAccess(Paths.get("/aaa/bbb/ccc/b.log")) == null;
        assert lm.checkAccess(Paths.get("/aaa/b33/ccc/v.log")) == null;
        assert lm.checkAccess(Paths.get("/aaa/b33/b33/ccc/v.log")) != null;
        assert lm.checkAccess(Paths.get("/aaa/b33/v.log")) != null;
        assert lm.checkAccess(Paths.get("/aaa/b33/ccc/ddd/v.log")) != null;
        assert lm.checkAccess(Paths.get("/aaa/zzz/ccc/v.log")) != null;
        assert lm.checkAccess(Paths.get("/aaa/b/ccc/v.log")) == null;
        assert lm.checkAccess(Paths.get("/aaaz/b/ccc/v.log")) != null;
        assert lm.checkAccess(Paths.get("/v.log")) != null;
    }

    @Test
    public void testOptionalDir() {
        LvFileAccessManagerImpl lm = create("/aaa/**/ccc/*");
        assertEquals(Arrays.asList(Paths.get("/aaa")), lm.getRoots());

        assert lm.isDirectoryVisible(Paths.get("/"));
        assert lm.isDirectoryVisible(Paths.get("/aaa"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/bbb"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/b33"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/b33/ccc"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/b33/b33/ccc"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/b33/g/e/y/ccc"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/b33/g/e/y/ccc/y"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/f/h/e/3/s/ccc"));
        assert !lm.isDirectoryVisible(Paths.get("/zzz"));
        assert !lm.isDirectoryVisible(Paths.get("/zzz/fff"));

        assert lm.checkAccess(Paths.get("/aaa/bbb/ccc/b.log")) == null;
        assert lm.checkAccess(Paths.get("/aaa/ccc/v.log")) == null;
        assert lm.checkAccess(Paths.get("/aaa/bbb/ccc/ff/b.log")) != null;
        assert lm.checkAccess(Paths.get("/aaa/b33/b33/ccc/v.log")) == null;
        assert lm.checkAccess(Paths.get("/v.log")) != null;
    }

    @Test
    public void testDoubleAsteriskInDirectory() {
        LvFileAccessManagerImpl lm = create("/aaa/b**/ccc/*");
        assertEquals(Arrays.asList(Paths.get("/aaa")), lm.getRoots());

        assert lm.isDirectoryVisible(Paths.get("/"));
        assert lm.isDirectoryVisible(Paths.get("/aaa"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/bbb"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/b33"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/b33/ccc"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/b33/b33/ccc"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/b33/g/e/y/ccc"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/b33/g/e/y/ccc/y"));
        assert !lm.isDirectoryVisible(Paths.get("/aaa/f/h/e/3/s/ccc"));
        assert !lm.isDirectoryVisible(Paths.get("/aaa/f"));
        assert !lm.isDirectoryVisible(Paths.get("/zzz"));
        assert !lm.isDirectoryVisible(Paths.get("/zzz/fff"));

        assert lm.checkAccess(Paths.get("/aaa/bbb/ccc/b.log")) == null;
        assert lm.checkAccess(Paths.get("/aaa/b33/ccc/v.log")) == null;
        assert lm.checkAccess(Paths.get("/aaa/b33/b33/ccc/v.log")) == null;
        assert lm.checkAccess(Paths.get("/aaa/b33/f/h/b33/ccc/v.log")) == null;

        assert lm.checkAccess(Paths.get("/aaa/f33/f/h/b33/ccc/v.log")) != null;
        assert lm.checkAccess(Paths.get("/aaa/f33/f/h/b33/v.log")) != null;
        assert lm.checkAccess(Paths.get("/aaa/bccc/v.log")) != null;
        assert lm.checkAccess(Paths.get("/aaa/b33/v.log")) != null;
        assert lm.checkAccess(Paths.get("/aaa/b33/ttt/v.log")) != null;
        assert lm.checkAccess(Paths.get("/v.log")) != null;
    }

    @Test
    public void testExtension() {
        LvFileAccessManagerImpl lm = create("**.log");
        assertEquals(Arrays.asList(Paths.get("/")), lm.getRoots());

        assert lm.isDirectoryVisible(Paths.get("/"));
        assert lm.isDirectoryVisible(Paths.get("/aaa"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/bbb"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/b33"));

        assert lm.checkAccess(Paths.get("/aaa/bbb/ccc/b.log")) == null;
        assert lm.checkAccess(Paths.get("/aaa/v.log")) == null;
        assert lm.checkAccess(Paths.get("/v.log")) == null;
        assert lm.checkAccess(Paths.get("/aaa/bbb/ccc/b.txt")) != null;
        assert lm.checkAccess(Paths.get("/aaa/v.txt")) != null;
        assert lm.checkAccess(Paths.get("/v.txt")) != null;
    }

    @Test
    public void testExtension2() {
        LvFileAccessManagerImpl lm = create("*.log");
        assertEquals(Arrays.asList(Paths.get("/")), lm.getRoots());

        assert lm.isDirectoryVisible(Paths.get("/"));
        assert lm.isDirectoryVisible(Paths.get("/aaa"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/bbb"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/b33"));

        assert lm.checkAccess(Paths.get("/aaa/bbb/ccc/b.log")) == null;
        assert lm.checkAccess(Paths.get("/aaa/v.log")) == null;
        assert lm.checkAccess(Paths.get("/v.log")) == null;
        assert lm.checkAccess(Paths.get("/aaa/bbb/ccc/b.txt")) != null;
        assert lm.checkAccess(Paths.get("/aaa/v.txt")) != null;
        assert lm.checkAccess(Paths.get("/v.txt")) != null;
    }

    @Test
    public void testExtension3() {
        LvFileAccessManagerImpl lm = create("**/*.log");
        assertEquals(Arrays.asList(Paths.get("/")), lm.getRoots());

        assert lm.isDirectoryVisible(Paths.get("/"));
        assert lm.isDirectoryVisible(Paths.get("/aaa"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/bbb"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/b33"));

        assert lm.checkAccess(Paths.get("/aaa/bbb/ccc/b.log")) == null;
        assert lm.checkAccess(Paths.get("/aaa/v.log")) == null;
        assert lm.checkAccess(Paths.get("/v.log")) == null;
        assert lm.checkAccess(Paths.get("/aaa/bbb/ccc/b.txt")) != null;
        assert lm.checkAccess(Paths.get("/aaa/v.txt")) != null;
        assert lm.checkAccess(Paths.get("/v.txt")) != null;
    }

    @Test
    public void testExtension4() {
        LvFileAccessManagerImpl lm = create("*/*.log");
        assertEquals(Arrays.asList(Paths.get("/")), lm.getRoots());

        assert lm.isDirectoryVisible(Paths.get("/"));
        assert lm.isDirectoryVisible(Paths.get("/aaa"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/bbb"));
        assert lm.isDirectoryVisible(Paths.get("/aaa/b33"));

        assert lm.checkAccess(Paths.get("/aaa/bbb/ccc/b.log")) == null;
        assert lm.checkAccess(Paths.get("/aaa/v.log")) == null;
    }

    @Test
    public void relativePath() {
        LvFileAccessManagerImpl lm = create("aaa/*/l*.log");
        assertEquals(Arrays.asList(Paths.get("/")), lm.getRoots());

        assert lm.isDirectoryVisible(Paths.get("/"));
        assert lm.isDirectoryVisible(Paths.get("/s"));
        assert lm.isDirectoryVisible(Paths.get("/s/fsd/ds/sdfs"));

        assert lm.checkAccess(Paths.get("/aaa/bbb/l.log")) == null;
        assert lm.checkAccess(Paths.get("/aaa/bbb/lll.log")) == null;
        assert lm.checkAccess(Paths.get("/aaaaa/bbb/lll.log")) != null;
        assert lm.checkAccess(Paths.get("/aaa/lll.log")) != null;
        assert lm.checkAccess(Paths.get("/asd/aaaa/aaa/bbb/lll.log")) == null;
        assert lm.checkAccess(Paths.get("/asd/aaaa/aaa/bbb/v.log")) != null;
    }

    @Test
    public void relativePath2() {
        LvFileAccessManagerImpl lm = create("**/aaa/*/l*.log");
        assertEquals(Arrays.asList(Paths.get("/")), lm.getRoots());

        assert lm.isDirectoryVisible(Paths.get("/"));
        assert lm.isDirectoryVisible(Paths.get("/s"));
        assert lm.isDirectoryVisible(Paths.get("/s/fsd/ds/sdfs"));

        assert lm.checkAccess(Paths.get("/aaa/bbb/l.log")) == null;
        assert lm.checkAccess(Paths.get("/aaa/bbb/lll.log")) == null;
        assert lm.checkAccess(Paths.get("/aaaaa/bbb/lll.log")) != null;
        assert lm.checkAccess(Paths.get("/aaa/lll.log")) != null;
        assert lm.checkAccess(Paths.get("/asd/aaaa/aaa/bbb/lll.log")) == null;
        assert lm.checkAccess(Paths.get("/asd/aaaa/aaa/bbb/v.log")) != null;
    }

    @Test
    public void testRelative6() {
        LvFileAccessManagerImpl lm = create("a*/ccc/*");
        assertEquals(Arrays.asList(Paths.get("/")), lm.getRoots());

        assert lm.isDirectoryVisible(Paths.get("/"));
        assert lm.isDirectoryVisible(Paths.get("/s"));
        assert lm.isDirectoryVisible(Paths.get("/s/fsd/ds/sdfs"));

        assert lm.checkAccess(Paths.get("/aaa/ccc/l.log")) == null;
        assert lm.checkAccess(Paths.get("/foo/aaa/ccc/l.log")) == null;
        assert lm.checkAccess(Paths.get("/foo/bar/aaa/ccc/l.log")) == null;
        assert lm.checkAccess(Paths.get("/ccc/l.log")) != null;
        assert lm.checkAccess(Paths.get("/aaa/ccc/ddd/l.log")) != null;
        assert lm.checkAccess(Paths.get("/bbb/ccc/l.log")) != null;
        assert lm.checkAccess(Paths.get("/aaa/bbb/l.log")) != null;
    }

    private LvFileAccessManagerImpl create(String ... pattern) {
        return new LvFileAccessManagerImpl(Stream.of(pattern).map(PathPattern::fromPattern).collect(Collectors.toList()));
    }

    @Test
    public void testRootSelection1() {
        LvFileAccessManagerImpl lm = create("a*/ccc/*", "/rrr/a.txt", "/rrr/b.txt");
        assertEquals(Arrays.asList(Paths.get("/")), lm.getRoots());
    }

    @Test
    public void testRootSelection11() {
        LvFileAccessManagerImpl lm = create("*/ccc/*", "/rrr/a.txt", "/rrr/b.txt");
        assertEquals(Arrays.asList(Paths.get("/")), lm.getRoots());
    }

    @Test
    public void testRootSelection12() {
        LvFileAccessManagerImpl lm = create("ccc/*", "/rrr/a.txt", "/rrr/b.txt");
        assertEquals(Arrays.asList(Paths.get("/")), lm.getRoots());
    }

    @Test
    public void testRootSelection2() {
        LvFileAccessManagerImpl lm = create("/rrr/a.txt", "/rrr/b.txt");
        assertEquals(Arrays.asList(Paths.get("/rrr")), lm.getRoots());
    }

    @Test
    public void testRootSelection3() {
        LvFileAccessManagerImpl lm = create("/rrr/a.txt", "/rrr/foo/b.txt");
        assertEquals(Arrays.asList(Paths.get("/rrr")), lm.getRoots());
    }

    @Test
    public void testRootSelection4() {
        LvFileAccessManagerImpl lm = create("/rrr/*", "/rrr/foo/b.txt");
        assertEquals(Arrays.asList(Paths.get("/rrr")), lm.getRoots());
    }

    @Test
    public void testRootSelection5() {
        LvFileAccessManagerImpl lm = create("/rrr/bbb/*", "/rrr/bbb/b.txt");
        assertEquals(Arrays.asList(Paths.get("/rrr/bbb")), lm.getRoots());
    }

}