package com.seerofspace.instagramscraper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
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
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import io.github.bonigarcia.wdm.WebDriverManager;

public class Main {
	
	private static boolean stopped;
	private static WebDriver driver;
	private static boolean headless;
	
	public static void main(String[] args) {
		//args = new String[] {"-u", "https://www.instagram.com/p/CaNa1Sbuw1g/"};
		//args = "-u https://www.instagram.com/p/Ca3Gn7kFr9K/ https://www.instagram.com/p/Ca3G7T0Lo8u/ https://www.instagram.com/p/Ca3HjszgwrK/ https://www.instagram.com/p/Ca6YeSoOykL/".split(" ");
		
		Options options = new Options();
		options.addOption("h", "not-headless", false, "start web driver without headless mode");
		options.addOption("n", "no-dir", false, "do not create a seperate directory for the downloads");
		//options.addRequiredOption("u", "url", true, "url to instagram post");
		options.addOption(Option.builder("u").longOpt("urls").hasArgs().required().desc("urls to instagram posts").build());
		CommandLineParser parser = new DefaultParser();
		CommandLine line;
		try {
			line = parser.parse(options, args);
		} catch (ParseException e2) {
			e2.printStackTrace();
			return;
		}
		List<String> urls = Arrays.asList(line.getOptionValues("urls"));
		//String url = line.getOptionValue('u') + "?__a=1";
		headless = !line.hasOption("not-headless");
		boolean createDir = !line.hasOption("no-dir");
		stopped = false;
		
		System.out.println("Starting driver");
		driver = getChromeDriver(headless);
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
			if(createDir && mediaList.size() > 1) {
				new File(filename).mkdir();
				filename = filename + File.separator + filename;
			}
			Iterator<MediaElement> mediaIter = mediaList.iterator();
			for(int i = 0; mediaIter.hasNext(); i++) {
				MediaElement me = mediaIter.next();
				File file;
				if(me.type == MediaElement.mediaType.VIDEO) {
					file = new File(filename + i + ".mp4");
				} else {
					file = new File(filename + i + ".jpg");
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
	
	public static WebDriver getChromeDriver(boolean headless) {
		WebDriverManager.getInstance().setup();
		ChromeOptions ops = new ChromeOptions();
		ops.addArguments("window-size=1200,800");
		ops.addArguments("--mute-audio");
		ops.addArguments("--log-level=3");
		ops.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.82 Safari/537.36");
		if(headless) {
			ops.addArguments("headless");
		}
		try {
			File profile = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			if(profile.isFile()) {
				ops.addArguments("--user-data-dir=" + new File(profile.getParentFile(), "profile").getAbsolutePath());
			} else {
				ops.addArguments("--user-data-dir=" + new File("profile").getAbsolutePath());
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return new ChromeDriver(ops);
	}
	
	public static void stop() {
		if(!stopped) {
			driver.close();
			driver.quit();
			stopped = true;
			System.out.println("Driver stopped");
		}
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
			driver = getChromeDriver(false);
			stopped = false;
			headless = false;
		}
		String instaURL = "https://www.instagram.com/";
		driver.get(instaURL);
		System.out.println("Fetching: " + instaURL);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e2) {
			e2.printStackTrace();
		}
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
