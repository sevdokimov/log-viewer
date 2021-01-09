package com.logviewer.logLibs.nginx;

import com.logviewer.data2.FieldTypes;
import org.springframework.lang.NonNull;

public class NginxUriNode extends NginxStretchNode {

    public NginxUriNode(@NonNull String fieldName) {
        super(fieldName, FieldTypes.URI, true, 1);
    }

    @Override
    protected boolean isAcceptableSymbol(char a) {
        if (Character.isLetterOrDigit(a))
            return true;

        switch (a) {
            case '-':
            case '.':
            case '_':
            case '~':
            case ':':
            case '/':
            case '?':
            case '#':
            case '[':
            case ']':
            case '@':
            case '!':
            case '$':
            case '&':
            case '\'':
            case '(':
            case ')':
            case '*':
            case '+':
            case ',':
            case ';':
            case '%':
            case '=':
                return true;
            default:
                return false;
        }
    }

    @Override
    public NginxStretchNode clone() {
        return new NginxUriNode(getFieldName());
    }
}
