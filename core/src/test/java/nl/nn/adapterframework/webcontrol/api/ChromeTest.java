import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
 
import java.net.URL;
 
public class ChromeTest {
 
  public static final String USERNAME = "Baswat";
  public static final String ACCESS_KEY = "d835004c-97a1-4e52-b63f-daa5b1d3d3fd";
  public static final String URL = "https://" + USERNAME + ":" + ACCESS_KEY + "@ondemand.saucelabs.com:443/wd/hub";
 
  public static void main(String[] args) throws Exception {
 
    DesiredCapabilities caps = DesiredCapabilities.chrome();
    caps.setCapability("platform", "Windows XP");
    caps.setCapability("version", "43.0");
 
    WebDriver driver = new RemoteWebDriver(new URL(URL), caps);
 
    /**
     * Goes to Sauce Lab's guinea-pig page and prints title
     */
 
    driver.get("http://ibis4example.ibissource.org/iaf/gui/#/status");
    System.out.println("title of page is: " + driver.getTitle());
 
    driver.quit();
  }
}