package nl.nn.adapterframework.webcontrol.api;

import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URL;

public class SampleSauceTest {

	private static WebDriver driver;
	public static final String USERNAME = "Baswat";
	public static final String AUTOMATE_KEY = "d835004c-97a1-4e52-b63f-daa5b1d3d3fd";
//	private static String URL = "http://"+ USERNAME + ":" + AUTOMATE_KEY +  "@localhost:4445/wd/hub"; 
		private static String URL = "https://"+ USERNAME + ":" + AUTOMATE_KEY +  "@ondemand.saucelabs.com:443/wd/hub";

	@Test
	public void SampleSauceTest() throws Exception {

		DesiredCapabilities caps = DesiredCapabilities.chrome();
		caps.setCapability("platform", "Windows XP");
		caps.setCapability("version", "43.0");

		WebDriver driver = new RemoteWebDriver(new URL(URL), caps);

		/**
		 * Goes to Sauce Lab's guinea-pig page and prints title
		 */

		driver.get("https://saucelabs.com/test/guinea-pig");
		System.out.println("title of page is: " + driver.getTitle());
		
		driver.quit();
	}
}