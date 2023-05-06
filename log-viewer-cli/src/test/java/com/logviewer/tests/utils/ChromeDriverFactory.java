package com.logviewer.tests.utils;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v112.emulation.Emulation;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ChromeDriverFactory implements Supplier<RemoteWebDriver> {
    @Override
    public RemoteWebDriver get() {
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});

        Map<String, Object> perf = new HashMap<>();
        perf.put("profile.default_content_settings.popups", 0);
        perf.put("download.default_directory", WebTestUtils.getDownloadDirectory().toString());

        options.setExperimentalOption("prefs", perf);

        ChromeDriver res = new ChromeDriver(options);

        DevTools devTools = res.getDevTools();
        devTools.createSession();
        devTools.send(Emulation.setTimezoneOverride("Europe/Moscow"));

        return res;
    }
}
