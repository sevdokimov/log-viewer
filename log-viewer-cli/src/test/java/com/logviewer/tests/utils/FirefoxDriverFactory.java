package com.logviewer.tests.utils;

import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.function.Supplier;

public class FirefoxDriverFactory implements Supplier<RemoteWebDriver> {
    @Override
    public RemoteWebDriver get() {
        FirefoxOptions options = new FirefoxOptions();
        return new FirefoxDriver(options);
    }
}
