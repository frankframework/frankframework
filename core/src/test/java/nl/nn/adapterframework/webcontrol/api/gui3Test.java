package nl.nn.adapterframework.webcontrol.api;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;
import java.util.Date;
import java.io.File;
import java.net.URL;

import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.*;

import static org.openqa.selenium.OutputType.*;

public class gui3Test {
	private static WebDriver driver;
	public static final String USERNAME = "Baswat";
	public static final String AUTOMATE_KEY = "d835004c-97a1-4e52-b63f-daa5b1d3d3fd";
	private static String URL = "http://"+ USERNAME + ":" + AUTOMATE_KEY +  "@localhost:4445/wd/hub"; 
//	private static String URL = "https://localhost:4445";
//	private static String URL = "https://"+ USERNAME + ":" + AUTOMATE_KEY +  "@ondemand.saucelabs.com:443/wd/hub";
	
    @Before
    public void setUp() throws Exception {
    	DesiredCapabilities caps = DesiredCapabilities.chrome();
    	caps.setCapability("platform", "Windows 8.1");
    	caps.setCapability("version", "45.0");
	    caps.setCapability("tunnel-identifier", System.getProperty("TRAVIS_JOB_NUMBER"));

	    
	    driver = new RemoteWebDriver(new URL(URL), caps);
//	    if(System.getProperty("TRAVIS_JOB_NUMBER") != null) {
//	    	driver = new RemoteWebDriver(new URL(URL), caps);
//	    }
//	    else {
//	    	System.setProperty("webdriver.chrome.driver", "C:/Data/Vergaarbak/chromedriver_win32/chromedriver.exe");
//			driver = new ChromeDriver();
//	    }

    	//System.setProperty("webdriver.chrome.driver", "C:/Data/Vergaarbak/chromedriver_win32/chromedriver.exe");
    	driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
    }
    
    @Test
    public void gui3Test() {
    	driver.get("http://localhost:8080/Angular/");
    	Assert.assertEquals("IAF | Adapter Status", driver.getTitle());
        driver.findElement(By.linkText("Logging")).click();
        driver.findElement(By.linkText("Test Pipeline")).click();
        driver.findElement(By.linkText("Test serviceListener")).click();
        driver.findElement(By.linkText("Webservices")).click();
        driver.findElement(By.linkText("Scheduler")).click();
        driver.findElement(By.linkText("Environment Variables")).click();
        driver.findElement(By.linkText("JDBC")).click();
        driver.findElement(By.linkText("Browse Tables")).click();
        driver.findElement(By.linkText("Information")).click();
        driver.findElement(By.cssSelector("button.btn.btn-white")).click();
    }
    
    @After
    public void tearDown() {
        driver.quit();
    }
    
    public static boolean isAlertPresent(FirefoxDriver wd) {
        try {
        	driver.switchTo().alert();
            return true;
        } catch (NoAlertPresentException e) {
            return false;
        }
    }
}
