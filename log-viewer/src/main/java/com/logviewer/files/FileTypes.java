package com.logviewer.files;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class FileTypes {

    public static final FileType LOG = new FileType("log",
            Pattern.compile(".*\\.log(?:\\.(?:\\d+|\\d\\d\\d\\d-\\d\\d-\\d\\d))?", Pattern.CASE_INSENSITIVE),
            "assets/file-types/text.png");

    public static final FileType OUT = new FileType("out",
            Pattern.compile(".*\\.out(?:\\.\\d+)?"),
            "assets/file-types/text.png");

    public static final FileType TEXT = new FileType("text",
            Pattern.compile(".*\\.txt\\~?", Pattern.CASE_INSENSITIVE),
            "assets/file-types/text.png");

    public static final FileType JAVA = new FileType("java",
            Pattern.compile(".*\\.java"),
            "assets/file-types/java.png");

    public static final FileType JAVA_SCRIPT = new FileType("javaScript",
            Pattern.compile(".*\\.js", Pattern.CASE_INSENSITIVE),
            "assets/file-types/javaScript.png");

    public static final FileType TYPE_SCRIPT = new FileType("typeScript",
            Pattern.compile(".*\\.ts", Pattern.CASE_INSENSITIVE),
            "assets/file-types/typeScript.png");

    public static final FileType JSON = new FileType("json",
            Pattern.compile(".*\\.json", Pattern.CASE_INSENSITIVE),
            "assets/file-types/json.png");

    public static final FileType JSP = new FileType("jsp",
            Pattern.compile(".*\\.jsp", Pattern.CASE_INSENSITIVE),
            "assets/file-types/jsp.png");

    public static final FileType HTML = new FileType("html",
            Pattern.compile(".*\\.x?html?", Pattern.CASE_INSENSITIVE),
            "assets/file-types/html.png");

    public static final FileType JSPX = new FileType("jspx",
            Pattern.compile(".*\\.jspx", Pattern.CASE_INSENSITIVE),
            "assets/file-types/jspx.png");

    public static final FileType XML = new FileType("xml",
            Pattern.compile(".*\\.xml", Pattern.CASE_INSENSITIVE),
            "assets/file-types/xml.png");

    public static final FileType TGZ = new FileType("tgz",
            Pattern.compile(".*\\.(tgz|tar\\.gz)", Pattern.CASE_INSENSITIVE),
            "assets/file-types/archive.png");

    public static final FileType ZIP = new FileType("zip",
            Pattern.compile(".*\\.(jar|zip|war)", Pattern.CASE_INSENSITIVE),
            "assets/file-types/archive.png");

    public static final FileType PROPS = new FileType("properties",
            Pattern.compile(".*\\.properties", Pattern.CASE_INSENSITIVE),
            "assets/file-types/properties.png");

    public static final FileType UNKNOWN = new FileType("unknown",
            Pattern.compile(".*"),
            "assets/file-types/unknown.png");


    private static FileType[] ALL_TYPES = new FileType[]{LOG, OUT, TEXT, JAVA, JAVA_SCRIPT, TYPE_SCRIPT, JSON, JSP, HTML,
            JSPX, XML, PROPS, ZIP, TGZ, UNKNOWN};

    static {
        Set<String> ids = new HashSet<>();
        for (FileType type : ALL_TYPES) {
            assert ids.add(type.getTypeId());
        }
    }

    @Nonnull
    public static FileType detectType(@Nonnull String path) {
        for (FileType type : ALL_TYPES) {
            if (type.getPattern().matcher(path).matches())
                return type;
        }

        return UNKNOWN;
    }
}
