package com.seerofspace.instagramscraper;

import java.io.File;
import java.net.URISyntaxException;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxDriverLogLevel;
import org.openqa.selenium.firefox.FirefoxOptions;

import io.github.bonigarcia.wdm.WebDriverManager;

public class WebDriverSetup {
	
	private static final String USERAGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.82 Safari/537.36";
	private static final int WIDTH = 1200;
	private static final int HEIGHT = 800;
	
	public enum DriverType {
		CHROME, FIREFOX
	}
	
	public static WebDriver getChromeDriver(boolean headless) {
		//WebDriverManager.getInstance().setup();
		WebDriverManager.chromedriver().setup();
		ChromeOptions ops = new ChromeOptions();
		//ops.addArguments("window-size=1200,800");
		ops.addArguments(String.format("window-size=%d,%d", WIDTH, HEIGHT));
		ops.addArguments("--mute-audio");
		ops.addArguments("--log-level=3");
		ops.addArguments("user-agent=" + USERAGENT);
		ops.setHeadless(headless);
		
		File profile = new File(getExeDir(), "chrome-profile");
		ops.addArguments("--user-data-dir=" + profile.getAbsolutePath());
		return new ChromeDriver(ops);
	}
	
	public static WebDriver getFirefoxDriver(boolean headless) {
		WebDriverManager.firefoxdriver().setup();
		FirefoxOptions ops = new FirefoxOptions();
		ops.addArguments("-width=1200");
		ops.addArguments("-height=800");
		ops.setHeadless(headless);
		ops.setLogLevel(FirefoxDriverLogLevel.FATAL);
		ops.addPreference("media.volume_scale", "0.0");
		ops.addPreference("general.useragent.override", USERAGENT);
		ops.addPreference("devtools.jsonview.enabled", false);
		
		//File profile = new File(getWorkingDir(), "firefox-profile");
		//profile.mkdir();
		//FirefoxProfile firefoxProfile = new FirefoxProfile(profile);
		//FirefoxProfile firefoxProfile = new FirefoxProfile();
		//firefoxProfile.setPreference("media.volume_scale", "0.0");
		//firefoxProfile.setPreference("general.useragent.override", USERAGENT);
		//ops.setProfile(firefoxProfile);
		return new FirefoxDriver(ops);
	}
	
	public static File getExeDir() {
		File workingDir = null;
		try {
			File file = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			if(file.isFile()) {
				workingDir = file.getParentFile();
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return workingDir;
	}
	
	public static WebDriver getDriver(DriverType type, boolean headless) {
		WebDriver driver = null;
		switch(type) {
			case CHROME: driver = getChromeDriver(headless); break;
			case FIREFOX: driver = getFirefoxDriver(headless); break;
		}
		return driver;
	}
	
}
