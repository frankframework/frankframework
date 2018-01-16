package nl.nn.adapterframework.webcontrol.api;

import java.net.MalformedURLException;
import java.net.URL;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

public abstract class ChromeTestBase {
	private static WebDriver driver;
	public static final String USERNAME = "Baswat";
	public static final String AUTOMATE_KEY = "d835004c-97a1-4e52-b63f-daa5b1d3d3fd";
	private static String URL = "https://"+ USERNAME + ":" + AUTOMATE_KEY +  "@ondemand.saucelabs.com:443/wd/hub";
	
	public void setup() throws Exception {
		DesiredCapabilities caps = DesiredCapabilities.chrome();
		caps.setCapability("platform", "Windows");
		caps.setCapability("version", "43.0");

		WebDriver driver = new RemoteWebDriver(new URL(URL), caps);
	}
}