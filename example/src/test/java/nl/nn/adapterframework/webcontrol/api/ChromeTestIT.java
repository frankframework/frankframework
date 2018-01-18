package nl.nn.adapterframework.webcontrol.api;

import org.junit.Test;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class ChromeTestIT extends ChromeTestBase {

	@Test
	public void PageTitle() throws Exception {
		Assert.assertEquals("IAF | Adapter Status", driver.getTitle());
	}
	
	@Test
	public void NavigateToConfiguration() throws Exception {
		String selector = "#side-menu li:nth-child(3) a";
		
		waitUntilVisible(By.cssSelector(selector));
		driver.findElement(By.cssSelector(selector)).click();

		Assert.assertEquals("IAF | Configurations", driver.getTitle());
	}
}