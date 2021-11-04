package com.logviewer.web;

import javax.swing.text.html.FormSubmitEvent;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A method nnotated this annotation may have one of the following signature:
 *
 * foo()
 * foo(T requestBody)
 * foo(HttpServletRequest request)
 * foo(HttpServletRequest request, HttpServletResponse response)
 * foo(HttpServletRequest request, HttpServletResponse response, T requestBody)
 *
 * @see AbstractRestRequestHandler#process(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Endpoint {
    FormSubmitEvent.MethodType[] method() default FormSubmitEvent.MethodType.GET;
}
