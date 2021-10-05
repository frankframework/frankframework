package nl.nn.adapterframework.test;

import javax.xml.parsers.SAXParserFactory;

import org.junit.Test;

public class EnvironmentTest {
	@Test
	public void getJRE() {
		String javaVersion = System.getProperty("java.version");
		System.out.println(String.format("Java version: %s", javaVersion));
	}

	@Test
	public void getSAXImplementation() {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		System.out.println(String.format("SAXParserFactory implementation: %s", factory.getClass().getName()));
	}
}

