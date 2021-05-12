package com.logviewer.tests.pages;

import com.logviewer.tests.web.AbstractWebTestCase;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public interface ChooserPage {

    By CHOOSER = By.tagName("lv-navigator");

    static WebElement findFile(String name) {
        return AbstractWebTestCase.driver.findElement(By.xpath("//lv-navigator//table[contains(@class,'file-list')]//td[contains(@class, 'name')][normalize-space(.) = '" + name + "']"));
    }

}
