package org.frankframework.eureka;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.web.client.HttpClientErrorException;
import org.junit.jupiter.api.Order;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

@WireMockTest(httpPort = 8080) 
@TestMethodOrder(OrderAnnotation.class)
public class EurekaProxyTest {
    //The objective of this test is to check if a service contains homePageUrl and if it can register to Eureka.
    private EurekaProxy eurekaProxy;
    private EurekaConfig config;

    @BeforeEach
    public void setup(){
        stubFor(get(urlEqualTo("/health"))
                .willReturn(aResponse().withStatus(200).withBody("UP")));
        eurekaProxy = new EurekaProxy();
        config = new EurekaConfig();
    }

    @Test
    @Order(1)
    public void testAddToEureka(){
        config.setAppName("MY-SERVICE");
        boolean result = eurekaProxy.registerService(config);
        assertTrue(result);
    }

    @Test
    @Order(2)
    //GetServiceByName returns the service only 
    public void testGetServiceByName() throws InterruptedException {
        int maxAttempts = 10;
        int sleepMillis = 1000;
        boolean found = false;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String result = eurekaProxy.getServiceByName("MY-SERVICE");
                assertNotNull(result); // Eureka returned something
                assertTrue(result.contains("<homePageUrl>"));
                assertTrue(result.contains("http://localhost:8080"));
                found = true;
                break;
            } catch (HttpClientErrorException.NotFound e) {
                System.out.printf("Attempt %d: %s not found, retrying...\n", attempt, "MY-SERVICE");
                Thread.sleep(sleepMillis);
            }
        }
    }

    @Test
    @Order(3)
    //Now we get the homepageurl
    public void testGetHomePageURL() {
        String result = eurekaProxy.getElement("MY-SERVICE", "homePageUrl");
        assertNotNull(result);
        assertTrue(result.contains("http://localhost:8080"));
        assertTrue(!result.contains("<homePageUrl>"));
    }

    @Test
    @Order(4)
    //De-register the service
    public void deregisterService(){
        // get
        String instanceID = eurekaProxy.getElement("MY-SERVICE", "instanceId");
        boolean result = eurekaProxy.deregisterService("MY-SERVICE", instanceID);
        assertTrue(result);
    }
}