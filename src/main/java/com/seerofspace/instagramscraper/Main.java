package com.seerofspace.instagramscraper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import com.seerofspace.instagramscraper.WebDriverSetup.DriverType;

public class Main {
	
	private static boolean stopped;
	private static WebDriver driver;
	private static boolean headless;
	private static DriverType type;
	
	public static void main(String[] args) {
		//args = "-u https://www.instagram.com/p/Ca3Gn7kFr9K/ https://www.instagram.com/p/Ca3G7T0Lo8u/ https://www.instagram.com/p/Ca3HjszgwrK/ https://www.instagram.com/p/Ca6YeSoOykL/".split(" ");
		
		Options options = new Options();
		options.addOption("h", "not-headless", false, "start web driver without headless mode");
		options.addOption("n", "no-dir", false, "do not create a seperate directory for the downloads");
		options.addOption("w", "web-driver", true, "valid options: chrome (default), firefox");
		options.addOption("o", "output-dir", true, "output directory");
		options.addOption(Option.builder("u").longOpt("urls").hasArgs().required().desc("urls to instagram posts").build());
		CommandLineParser parser = new DefaultParser();
		CommandLine line;
		try {
			line = parser.parse(options, args);
		} catch (ParseException e1) {
			e1.printStackTrace();
			return;
		}
		List<String> urls = Arrays.asList(line.getOptionValues("urls"));
		headless = !line.hasOption("not-headless");
		boolean createDir = !line.hasOption("no-dir");
		switch(line.getOptionValue("web-driver", "chrome")) {
			case "chrome": type = DriverType.CHROME; break;
			case "firefox": type = DriverType.FIREFOX; break;
			default: type = DriverType.CHROME; System.out.println("Error: invalid web-driver option"); break;
		}
		String outputPath = line.getOptionValue("output-dir", "");
		File outputDir = null;
		if(!outputPath.isEmpty()) {
			outputDir = new File(outputPath);
		}
		stopped = false;
		
		System.out.println("Driver starting");
		driver = getDriver();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			stop();
		}));
		
		for(String url : urls) {
			url = url + "?__a=1";
			System.out.println("Fetching: " + url);
			driver.get(url);
			//waitFor(1000);
			List<WebElement> elements = driver.findElements(By.tagName("pre"));
			if(elements.isEmpty() || new JSONObject(elements.get(0).getText()).optJSONArray("items") == null) {
				login();
				System.out.println("Fetching: " + url);
				driver.get(url);
				elements = driver.findElements(By.tagName("pre"));
			}
			String json = elements.get(0).getText();
			
			List<MediaElement> mediaList = new LinkedList<>();
			JSONObject jsonRoot = new JSONObject(json);
			JSONObject jsonItems = jsonRoot.getJSONArray("items").getJSONObject(0);
			JSONArray jsonMedia = jsonItems.optJSONArray("carousel_media");
			Iterable<Object> jsonIter;
			if(jsonMedia == null) {
				jsonIter = Arrays.asList(new Object[]{jsonItems});
			} else {
				jsonIter = jsonMedia;
			}
			for(Object obj : jsonIter) {
				try {
					JSONObject jsonObj = (JSONObject) obj;
					MediaElement me = new MediaElement();
					if(jsonObj.optJSONArray("video_versions") != null) {
						me.url = jsonObj.getJSONArray("video_versions").getJSONObject(0).getString("url");
						me.type = MediaElement.mediaType.VIDEO;
					} else {
						me.url = jsonObj.getJSONObject("image_versions2")
								.getJSONArray("candidates").getJSONObject(0).getString("url");
						me.type = MediaElement.mediaType.IMAGE;
					}
					mediaList.add(me);
					System.out.println(me.url);
				} catch(org.json.JSONException e1) {
					e1.printStackTrace();
				}
			}
			String username = jsonItems.getJSONObject("user").getString("username");
			String code = jsonItems.getString("code");
			
			String filename = String.format("%s-%s", username, code);
			if(outputDir != null) {
				outputDir.mkdir();
			}
			if(createDir && mediaList.size() > 1) {
				new File(outputDir, filename).mkdir();
				filename = filename + File.separator + filename;
			}
			Iterator<MediaElement> mediaIter = mediaList.iterator();
			for(int i = 0; mediaIter.hasNext(); i++) {
				MediaElement me = mediaIter.next();
				File file;
				if(me.type == MediaElement.mediaType.VIDEO) {
					file = new File(outputDir, filename + i + ".mp4");
				} else {
					file = new File(outputDir, filename + i + ".jpg");
				}
				System.out.println("Downloading: " + file.getName());
				download(me.url, file);
			}
		}
		
		stop();
		System.out.println("Finished");
	}
	
	private static class MediaElement {
		public static enum mediaType {
			VIDEO, IMAGE
		}
		public mediaType type ;
		public String url;
	}
	
	public static void stop() {
		if(!stopped) {
			//driver.close();
			driver.quit();
			stopped = true;
			System.out.println("Driver stopped");
		}
	}
	
	private static WebDriver getDriver(boolean headless) {
		return WebDriverSetup.getDriver(type, headless);
	}
	
	private static WebDriver getDriver() {
		return getDriver(headless);
	}
	
	public static void download(String url, File file) {
		try {
			ReadableByteChannel rbc = Channels.newChannel(new URL(url).openStream());
			FileOutputStream fos = new FileOutputStream(file);
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			rbc.close();
			fos.close();
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	private static void login() {
		System.out.println("Error: Login Needed");
		if(headless) {
			System.out.println("Restarting web driver without headless mode");
			stop();
			driver = getDriver(false);
			stopped = false;
			headless = false;
			System.out.println("Driver starting");
		}
		String instaURL = "https://www.instagram.com/";
		driver.get(instaURL);
		System.out.println("Fetching: " + instaURL);
		waitFor(1000);
		System.out.println("Waiting for login");
		while(!driver.findElements(By.xpath("//form[@id='loginForm']")).isEmpty()) {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		System.out.println("Login successful");
	}
	
	@SuppressWarnings("unused")
	private static void screenshot() {
		System.out.println("Screenshotting");
		try {
			FileUtils.copyFile(((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE), new File("screenshot.png"));
		} catch (WebDriverException | IOException e1) {
			e1.printStackTrace();
		}
	}
	
	public static void waitFor(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
}
