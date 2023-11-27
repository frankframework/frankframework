package nl.nn.adapterframework.configuration.digester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.StringWriter;
import java.net.URL;
import java.util.Properties;

import javax.xml.validation.ValidatorHandler;

import org.junit.jupiter.api.Test;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationDigester;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.PropertyLoader;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.xml.XmlWriter;

public class ConfigurationDigesterTest {
	private static final String FRANK_CONFIG_XSD = "/xml/xsd/FrankConfig-compatibility.xsd";

	private static final String STUB4TESTTOOL_CONFIGURATION_KEY = "stub4testtool.configuration";
	private static final String STUB4TESTTOOL_VALIDATORS_DISABLED_KEY = "validators.disabled";

	@Test
	public void testNewCanonicalizer() throws Exception {
		XmlWriter writer = new XmlWriter();
		ConfigurationDigester digester = new ConfigurationDigester();
		ContentHandler handler = digester.getConfigurationCanonicalizer(writer, FRANK_CONFIG_XSD, new XmlErrorHandler());

		Resource resource = Resource.getResource("/Digester/SimpleConfiguration/Configuration.xml");
		XmlUtils.parseXml(resource, handler);
		String result = writer.toString();
		String expected = TestFileUtils.getTestFile("/Digester/Canonicalized/SimpleConfiguration.xml");
		MatchUtils.assertXmlEquals(expected, result);
	}

	//Both OLD and NEW configuration parsers should set the same values for 'loadedConfiguration': properties resolved, secrets hidden
	//The new configuration parser returns the configuration with all property not yet resolved
	@Test
	public void testNewConfigurationPreParser() throws Exception {
		ConfigurationDigester digester = new ConfigurationDigester();
		digester.setConfigurationWarnings( new ConfigurationWarnings() );
		Resource resource = Resource.getResource("/Digester/SimpleConfiguration/Configuration.xml");
		PropertyLoader properties = new PropertyLoader("Digester/ConfigurationDigesterTest.properties");
		properties.setProperty("HelloWorld.active", "false");
		properties.setProperty("HelloBeautifulWorld.active", "!false");
		properties.setProperty("digester.property", "[ >\"< ]"); // new style non-escaped property values
		properties.setProperty("secret", "GEHEIM");
		properties.setProperty("properties.hide", "secret");
		Configuration configuration = new TestConfiguration();

		XmlWriter loadedConfigWriter = new XmlWriter();
		digester.parseAndResolveEntitiesAndProperties(loadedConfigWriter, configuration, resource, properties);
		String result = loadedConfigWriter.toString();
		String expected = TestFileUtils.getTestFile("/Digester/Loaded/SimpleConfigurationUnresolved.xml");
		MatchUtils.assertXmlEquals(expected, result);

		String storedResult = configuration.getLoadedConfiguration();
		String storedExpected = TestFileUtils.getTestFile("/Digester/Loaded/SimpleConfigurationResolvedAndHidden.xml");
		MatchUtils.assertXmlEquals(storedExpected, storedResult);

		loadedConfigWriter = new XmlWriter();
		properties.setProperty(STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		digester.parseAndResolveEntitiesAndProperties(loadedConfigWriter, configuration, resource, properties);
		String stubbedExpected = TestFileUtils.getTestFile("/Digester/Loaded/SimpleConfigurationStubbed.xml");
		MatchUtils.assertXmlEquals(stubbedExpected, loadedConfigWriter.toString());

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
		URL schemaURL = TestFileUtils.getTestFileURL(FRANK_CONFIG_XSD);
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

	@Test
	public void stub4testtoolTest() throws Exception {
		String baseDirectory = "/ConfigurationUtils/stub4testtool/FullAdapter";

		StringWriter target = new StringWriter();
		XmlWriter xmlWriter = new XmlWriter(target);

		Properties properties = new Properties();
		properties.setProperty(STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		properties.setProperty(STUB4TESTTOOL_VALIDATORS_DISABLED_KEY, Boolean.toString(false));

		String originalConfiguration = TestFileUtils.getTestFile(baseDirectory + "/original.xml");

		ConfigurationDigester digester = new ConfigurationDigester();
		ContentHandler filter = digester.getStub4TesttoolContentHandler(xmlWriter, properties);

		XmlUtils.parseXml(originalConfiguration, filter);

		String actual = target.toString();

		String expectedConfiguration = TestFileUtils.getTestFile(baseDirectory + "/expected.xml");
		MatchUtils.assertXmlEquals(expectedConfiguration, actual);
	}

	@Test
	public void stub4testtoolEsbJmsListenerTest() throws Exception {
		String baseDirectory = "/ConfigurationUtils/stub4testtool/EsbJmsListener";

		StringWriter target = new StringWriter();

		XmlWriter xmlWriter = new XmlWriter(target) {

			@Override
			public void startElement(String uri, String localName, String qName, Attributes attributes)throws SAXException {
				if(attributes != null && attributes.getValue("className") != null) {
					assertFalse(attributes.getValue("className").contains("EsbJmsListener"));
				}
				super.startElement(uri, localName, qName, attributes);
			}

			@Override
			public void comment(char[] ch, int start, int length) throws SAXException {
				if(!new String(ch).startsWith("<receiver name='receiver' transactionAttribute='Required' transactionTimeout=")) {
					fail("Digester should have commented out the receiver that has EsbJmsListener");
				}
				super.comment(ch, start, length);
			}
		};

		Properties properties = new Properties();
		properties.setProperty(STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		properties.setProperty(STUB4TESTTOOL_VALIDATORS_DISABLED_KEY, Boolean.toString(false));

		String originalConfiguration = TestFileUtils.getTestFile(baseDirectory + "/original.xml");

		ConfigurationDigester digester = new ConfigurationDigester();
		ContentHandler filter = digester.getStub4TesttoolContentHandler(xmlWriter, properties);

		XmlUtils.parseXml(originalConfiguration, filter);

		String actual = target.toString();

		String expectedConfiguration = TestFileUtils.getTestFile(baseDirectory + "/expected.xml");
		MatchUtils.assertXmlEquals(null, expectedConfiguration, actual, false, true);
	}

	private static class XmlErrorHandler implements ErrorHandler {
		@Override
		public void warning(SAXParseException exception) {
			System.err.println("Warning at line,column ["+exception.getLineNumber()+","+exception.getColumnNumber()+"]: " + exception.getMessage());
		}
		@Override
		public void error(SAXParseException exception) {
			System.err.println("Error at line,column ["+exception.getLineNumber()+","+exception.getColumnNumber()+"]: " + exception.getMessage());
		}
		@Override
		public void fatalError(SAXParseException exception) {
			System.err.println("FatalError at line,column ["+exception.getLineNumber()+","+exception.getColumnNumber()+"]: " + exception.getMessage());
		}
	}
}
