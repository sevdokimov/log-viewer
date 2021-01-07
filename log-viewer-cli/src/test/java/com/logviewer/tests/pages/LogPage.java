package com.logviewer.tests.pages;

import org.openqa.selenium.By;

public interface LogPage {

    By MENU = By.id("menu-icon");

    By MESSAGE_NO_LOG_PATH = By.id("no-log-paths");

    By RECORDS = By.id("no-log-paths");

    interface Menu {
        By ITEM_ADD_LOG = By.id("add-log-menu-item");
    }

}
