package com.logviewer.web;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Log-viewer should be compatible with both "javax.servlet.*" and "jakarta.servlet.*" servlets. This interface is
 * an abstraction containing common methods from "javax.servlet.http.HttpServletResponse" and "jakarta.servlet.http.HttpServletResponse"
 * @see javax.servlet.http.HttpServletResponse
 * @see jakarta.servlet.http.HttpServletResponse
 */
public interface LvServletResponse {
    void setStatus(int status);

    void setHeader(String name, String value);

    void setDateHeader(String name, long valueMs);

    void setContentType(String contentType);

    void setCharacterEncoding(String charset);

    PrintWriter getWriter() throws IOException;

    OutputStream getOutputStream() throws IOException;

    boolean isCommitted();

    void reset();

    void sendRedirect(String location) throws IOException;

    void sendError(int sc, String msg) throws IOException;
}
