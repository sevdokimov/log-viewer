package com.logviewer.web;

import javax.swing.text.html.FormSubmitEvent;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Endpoint {
    FormSubmitEvent.MethodType method() default FormSubmitEvent.MethodType.GET;
}
