package nl.nn.adapterframework.webcontrol.api;

import org.junit.Test;
import org.junit.Assert;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URL;

public class ChromeTest extends ChromeTestBase {

	public ChromeTest(WebDriver driver) throws Exception {
		super(driver);
	}
	
	@Test
	public void SampleSauceTest() throws Exception {
		driver.get("http://ibis4example.ibissource.org/iaf/gui/#/status");
		Assert.assertEquals("IAF | Adapter Status", driver.getTitle());
		Assert.assertTrue(true);
		driver.quit();
	}
}