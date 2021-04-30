package nl.nn.adapterframework.configuration.digester;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Properties;

import javax.xml.validation.ValidatorHandler;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationDigester;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.xml.XmlWriter;

public class ConfigurationDigesterTest {

	@Test
	public void testNewCanonicalizer() throws Exception {
		XmlWriter writer = new XmlWriter();
		ConfigurationDigester digester = new ConfigurationDigester();
		ContentHandler handler = digester.getCanonicalizedConfiguration(writer, ConfigurationUtils.FRANK_CONFIG_XSD, new XmlErrorHandler());

		Resource resource = Resource.getResource("/Digester/SimpleConfiguration/Configuration.xml");
		XmlUtils.parseXml(resource, handler);
		String result = writer.toString();
		String expected = TestFileUtils.getTestFile("/Digester/Resolved/SimpleConfiguration.xml");
		MatchUtils.assertXmlEquals(expected, result);
	}

	//Both OLD and NEW configuration parsers should output the same!!
	@Test
	public void testConfigurationPreParser() throws Exception {
		ConfigurationDigester digester = new ConfigurationDigester();
		Resource resource = Resource.getResource("/Digester/SimpleConfiguration/Configuration.xml");
		Properties properties = new Properties();
		properties.setProperty("HelloWorld.active", "false");
		properties.setProperty("HelloBeautifulWorld.active", "!false");
		Configuration configuration = new TestConfiguration();
		String result = digester.resolveEntitiesAndProperties(configuration, resource, properties, true);

		String expected = TestFileUtils.getTestFile("/Digester/Loaded/SimpleConfiguration.xml");
		MatchUtils.assertXmlEquals(expected, result);

		String original = TestFileUtils.getTestFile("/Digester/Original/SimpleConfiguration.xml");
		MatchUtils.assertXmlEquals(original, configuration.getOriginalConfiguration());
	}

	//Both OLD and NEW configuration parsers should output the same!!
	@Test
	public void testOldSchoolConfigurationParser() throws Exception {
		ConfigurationDigester digester = new ConfigurationDigester();
		Resource resource = Resource.getResource("/Digester/SimpleConfiguration/Configuration.xml");
		Properties properties = new Properties();
		properties.setProperty("HelloWorld.active", "false");
		properties.setProperty("HelloBeautifulWorld.active", "!false");
		Configuration configuration = new TestConfiguration();
		String result = digester.resolveEntitiesAndProperties(configuration, resource, properties, false);

		//Unfortunately we need to cleanup the result a bit...
		result = result.replaceAll("(</?module>)", "");//Remove the modules root tag
		result = result.replaceAll("(</?exits>)", "");//Remove the exits tag
		result = result.replace("<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">", "").replace("</root>", "");//Remove the root tag

		String expected = TestFileUtils.getTestFile("/Digester/Loaded/SimpleConfiguration.xml");

		result = MatchUtils.xmlPretty(result, true);
		expected = MatchUtils.xmlPretty(expected, true);

		Diff diff = XMLUnit.compareXML(expected, result); //We need to use XML Compare as the order is different in the old canonical xslt
		assertTrue(diff.toString(), diff.similar());

		String original = TestFileUtils.getTestFile("/Digester/Original/SimpleConfiguration.xml");
		MatchUtils.assertXmlEquals(original, configuration.getOriginalConfiguration());
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
	public void testFixedValueAttributeResolverWithFrankConfig() throws Exception {
		URL schemaURL = TestFileUtils.getTestFileURL(ConfigurationUtils.FRANK_CONFIG_XSD);
		ValidatorHandler validatorHandler = XmlUtils.getValidatorHandler(schemaURL);

		XmlWriter writer = new XmlWriter();
		validatorHandler.setContentHandler(writer);
		validatorHandler.setErrorHandler(new XmlErrorHandler());

		Resource resource = Resource.getResource("/Digester/PreParsedConfiguration.xml");
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
