package org.frankframework.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;

public class TestPropertyInheritanceWithSpring {
	private static final String APPLICATION = "Application";
	private static final String CONFIGURATION = "Configuration";

	private ClassPathXmlApplicationContext createContext(ApplicationContext parent, PropertiesPropertySource propertySource) {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();
		applicationContext.setParent(parent);

		MutablePropertySources propertySources = applicationContext.getEnvironment().getPropertySources();
		if(parent != null) {
			propertySources.addBefore(APPLICATION, propertySource);
		} else {
			propertySources.addLast(propertySource);
		}
		applicationContext.refresh();

		return applicationContext;
	}

	@Test
	public void testInheritance() {
		System.setProperty("testSystemProperty1", "sysProp1");
		System.setProperty("testSystemProperty2", "sysProp2");
		Properties globalProperties = new Properties();
		globalProperties.setProperty("one", "1");
		globalProperties.setProperty("two", "2");
		globalProperties.setProperty("three", "3");
		globalProperties.setProperty("testSystemProperty3", "globalProp3");

		PropertiesPropertySource globalPropertySource = new PropertiesPropertySource(APPLICATION, globalProperties);
		ClassPathXmlApplicationContext applicationContext = createContext(null, globalPropertySource);

		Properties localProperties = new Properties();
		localProperties.setProperty("one", "1");
		localProperties.setProperty("three", "6");
		localProperties.setProperty("testSystemProperty2", "localProp2");

		PropertiesPropertySource localPropertySource = new PropertiesPropertySource(CONFIGURATION, localProperties);
		ClassPathXmlApplicationContext configurationContext = createContext(applicationContext, localPropertySource);

		MutablePropertySources sources = configurationContext.getEnvironment().getPropertySources();
		assertEquals(4, sources.size());

		PropertySourcesPropertyResolver p = new PropertySourcesPropertyResolver(sources);
		assertEquals("1", p.getProperty("one"));
		assertEquals("2", p.getProperty("two"));
		assertEquals("6", p.getProperty("three"));
		assertEquals("sysProp1", p.getProperty("testSystemProperty1"));
		assertEquals("sysProp2", p.getProperty("testSystemProperty2"));
		assertEquals("globalProp3", p.getProperty("testSystemProperty3"));
	}
}
