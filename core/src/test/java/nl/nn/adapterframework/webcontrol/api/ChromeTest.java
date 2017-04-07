package nl.nn.adapterframework.webcontrol.api;

import java.util.concurrent.TimeUnit;
 
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;


public class ChromeTest {
	private static ChromeDriver driver;
	WebElement element;
	 
	@BeforeClass
	public static void openBrowser(){
		System.setProperty("webdriver.chrome.driver", "C:/Data/Vergaarbak/chromedriver_win32/chromedriver.exe");
		driver = new ChromeDriver();
	    driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
	}
	 
	@Test
	public void ibis4example (){
	    System.out.println("Starting test " + new Object(){}.getClass().getEnclosingMethod().getName());
	    driver.get("http://ibis4example.ibissource.org/Angular/#/status");
	    Assert.assertEquals("IAF | Adapter Status", driver.getTitle());
	    Assert.assertTrue(true);
	    System.out.println("Ending test " + new Object(){}.getClass().getEnclosingMethod().getName());
	}
	
	@AfterClass
	public static void closeBrowser(){
	    driver.quit();
	}
}