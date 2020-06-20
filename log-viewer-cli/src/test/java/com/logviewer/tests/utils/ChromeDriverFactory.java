package com.logviewer.tests.utils;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.function.Supplier;

public class ChromeDriverFactory implements Supplier<RemoteWebDriver> {
    @Override
    public RemoteWebDriver get() {
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        return new ChromeDriver(options);
    }
}
