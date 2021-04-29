package nl.nn.adapterframework.configuration.digester;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.util.Properties;

import javax.xml.validation.ValidatorHandler;

import org.junit.Test;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import nl.nn.adapterframework.configuration.ConfigurationDigester;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.xml.XmlWriter;

public class ConfigurationDigesterTest {

	@Test
	public void simpleConfigurationResolver() throws Exception {
		XmlWriter writer = new XmlWriter();
		ConfigurationDigester digester = new ConfigurationDigester();
		ContentHandler handler = digester.getCanonicalizedConfiguration(writer, ConfigurationUtils.FRANK_CONFIG_XSD, new XmlErrorHandler());

		Resource resource = Resource.getResource("/Digester/SimpleConfiguration/Configuration.xml");
		XmlUtils.parseXml(resource, handler);
		String result = writer.toString();
		String expected = TestFileUtils.getTestFile("/Digester/resolvedConfiguration.xml");
		MatchUtils.assertXmlEquals(expected, result);
	}

	@Test
	public void normalConfigurationResolver() throws Exception {
		ConfigurationDigester digester = new ConfigurationDigester();
		Resource resource = Resource.getResource("/Digester/SimpleConfiguration/Configuration.xml");
		Properties properties = new Properties();
		properties.setProperty("HelloWorld.active", "false");
		properties.setProperty("HelloBeautifulWorld.active", "!false");
		String result = digester.resolveEntitiesAndProperties(resource, properties);

		String expected = TestFileUtils.getTestFile("/Digester/resolvedConfiguration2.xml");
		MatchUtils.assertXmlEquals(expected, result);
	}

	@Test
	public void simpleXsdWithDefaultAndFixedAttributed() throws Exception {
		URL schemaURL = TestFileUtils.getTestFileURL("/Digester/resolveDefaultAttribute.xsd");
		ValidatorHandler validatorHandler = XmlUtils.getValidatorHandler(schemaURL);

		XmlWriter writer = new XmlWriter();
		validatorHandler.setContentHandler(writer);
		validatorHandler.setErrorHandler(new XmlErrorHandler());

		Resource resource = Resource.getResource("/Digester/resolveAttributes.xml");
		XmlUtils.parseXml(resource, validatorHandler);

		assertEquals("<note one=\"1\" two=\"2\"/>", writer.toString().trim());
	}

	@Test
	public void frankConfigXsdWithFixedAttributed() throws Exception {
		URL schemaURL = TestFileUtils.getTestFileURL(ConfigurationUtils.FRANK_CONFIG_XSD);
		ValidatorHandler validatorHandler = XmlUtils.getValidatorHandler(schemaURL);

		XmlWriter writer = new XmlWriter();
		validatorHandler.setContentHandler(writer);
		validatorHandler.setErrorHandler(new XmlErrorHandler());

		Resource resource = Resource.getResource("/Digester/SimpleConfiguration/PreParsedConfiguration.xml");
		assertNotNull(resource);
		XmlUtils.parseXml(resource, validatorHandler);

		String expected = TestFileUtils.getTestFile("/Digester/resolvedPreParsedConfiguration.xml");
		assertEquals(expected, writer.toString().trim());
	}

	private class XmlErrorHandler implements ErrorHandler {
		@Override
		public void warning(SAXParseException exception) throws SAXParseException {
			System.err.println("Warning at line,column ["+exception.getLineNumber()+","+exception.getColumnNumber()+"]: " + exception.getMessage());
		}
		@Override
		public void error(SAXParseException exception) throws SAXParseException {
			System.err.println("Error at line,column ["+exception.getLineNumber()+","+exception.getColumnNumber()+"]: " + exception.getMessage());
		}
		@Override
		public void fatalError(SAXParseException exception) throws SAXParseException {
			System.err.println("FatalError at line,column ["+exception.getLineNumber()+","+exception.getColumnNumber()+"]: " + exception.getMessage());
		}
	}
}
