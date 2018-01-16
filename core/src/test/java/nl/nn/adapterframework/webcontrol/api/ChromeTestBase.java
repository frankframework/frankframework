package nl.nn.adapterframework.webcontrol.api;

import java.net.MalformedURLException;
import java.net.URL;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

public abstract class ChromeTestBase {
	public static WebDriver driver;
	private static final String USERNAME = "Baswat";
	private static final String AUTOMATE_KEY = "d835004c-97a1-4e52-b63f-daa5b1d3d3fd";
	private static String URL = "https://"+ USERNAME + ":" + AUTOMATE_KEY +  "@ondemand.saucelabs.com:443/wd/hub";
	
	public ChromeTestBase(WebDriver driver) throws Exception {
		DesiredCapabilities caps = DesiredCapabilities.chrome();
		caps.setCapability("platform", "Windows");
		caps.setCapability("version", "43.0");

		driver = new RemoteWebDriver(new URL(URL), caps);
	}
}
