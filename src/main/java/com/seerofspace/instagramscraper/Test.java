package com.seerofspace.instagramscraper;

import org.openqa.selenium.WebDriver;

import com.seerofspace.instagramscraper.WebDriverSetup.DriverType;

public class Test {
	
	public static void main(String[] args) {
		
		WebDriver driver = WebDriverSetup.getDriver(DriverType.FIREFOX, false);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			driver.quit();
			System.out.println("Driver stopped");
		}));
		driver.get("https://www.google.com/");
		driver.get("https://www.whatismybrowser.com/detect/what-is-my-user-agent/");
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		
	}
	
}
