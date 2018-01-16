package nl.nn.adapterframework.webcontrol.api;

import org.junit.Test;
import org.junit.Assert;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URL;

public class ChromeTest extends ChromeTestBase {
	private static WebDriver driver;
	public static final String USERNAME = "Baswat";
	public static final String AUTOMATE_KEY = "d835004c-97a1-4e52-b63f-daa5b1d3d3fd";
	private static String URL = "https://"+ USERNAME + ":" + AUTOMATE_KEY +  "@ondemand.saucelabs.com:443/wd/hub";

	public ChromeTest() throws Exception {
		super();
	}
	
	@Test
	public void SampleSauceTest() throws Exception {
		driver.get("http://ibis4example.ibissource.org/iaf/gui/#/status");
		Assert.assertEquals("IAF | Adapter Status", driver.getTitle());
		Assert.assertTrue(true);
		driver.quit();
	}
}