package com.seerofspace.instagramscraper;

import java.net.URISyntaxException;

public class Test {
	
	public static void main(String[] args) {
		/*
		WebDriver driver = Main.getChromeDriver(false);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			driver.quit();
			System.out.println("Driver stopped");
		}));
		driver.get("https://www.google.com/");
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		*/
		try {
			System.out.println(ClassLoader.getSystemClassLoader().getResource(".").toURI());
			System.out.println(Test.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
	
}
