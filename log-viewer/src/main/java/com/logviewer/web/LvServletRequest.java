package com.logviewer.web;

import java.io.IOException;
import java.io.InputStream;

/**
 * Log-viewer should be compatible with both "javax.servlet.*" and "jakarta.servlet.*" servlets. This interface is
 * an abstraction containing common methods from "javax.servlet.http.HttpServletRequest" and "jakarta.servlet.http.HttpServletRequest"
 * @see javax.servlet.http.HttpServletRequest
 * @see jakarta.servlet.http.HttpServletRequest
 */
public interface LvServletRequest {

    String getContextPath();

    String getServletPath();

    String getRequestURI();

    String getParameter(String name);

    String[] getParameterValues(String name);

    long getDateHeader(String name);

    String getHeader(String name);

    String getMethod();

    InputStream getInputStream() throws IOException;

    String getQueryString();

    Object startAsync();

    java.security.Principal getUserPrincipal();
}
