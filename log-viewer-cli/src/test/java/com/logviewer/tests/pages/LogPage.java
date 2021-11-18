package com.logviewer.tests.pages;

import org.openqa.selenium.By;

public interface LogPage {

    By MENU = By.id("menu-icon");

    By MESSAGE_NO_LOG_PATH = By.id("no-log-paths");

    By RECORDS = By.xpath("//div[@id='records']/div[@class='record']");

    By ADD_FILTER_BUTTON = By.cssSelector(".add-filter-menu .add-filter-button");

    interface FilterPanel {
        By INPUT = By.id("filterInput");
        By HIDE_UNMATCHED = By.id("hide-unmatched");
    }

    interface Menu {
        By ITEM_ADD_LOG = By.id("add-log-menu-item");
        By DOWNLOAD_LOG = By.id("download-menu-item");
    }

}
