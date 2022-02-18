package nl.nn.adapterframework.configuration.digester;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.StringWriter;
import java.net.URL;
import java.util.Properties;

import javax.xml.validation.ValidatorHandler;

import org.junit.Test;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationDigester;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.StringResolver;
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
		ContentHandler handler = digester.getCanonicalizedConfiguration(writer, FRANK_CONFIG_XSD, new XmlErrorHandler());

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
		Resource resource = Resource.getResource("/Digester/SimpleConfiguration/Configuration.xml");
		Properties properties = new Properties();
		properties.setProperty("HelloWorld.active", "false");
		properties.setProperty("HelloBeautifulWorld.active", "!false");
		properties.setProperty("digester.property", "[ >\"< ]"); // new style non-escaped property values
		properties.setProperty("secret", "GEHEIM");
		properties.setProperty("properties.hide", "secret");
		Configuration configuration = new TestConfiguration();

		String result = digester.resolveEntitiesAndProperties(configuration, resource, properties, true);
		String expected = TestFileUtils.getTestFile("/Digester/Loaded/SimpleConfigurationUnresolved.xml");
		MatchUtils.assertXmlEquals(expected, result);

		String storedResult = configuration.getLoadedConfiguration();
		String storedExpected = TestFileUtils.getTestFile("/Digester/Loaded/SimpleConfigurationResolvedAndHidden.xml");
		MatchUtils.assertXmlEquals(storedExpected, storedResult);

		properties.setProperty(STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		String stubbedResult = digester.resolveEntitiesAndProperties(configuration, resource, properties, true);
		String stubbedExpected = TestFileUtils.getTestFile("/Digester/Loaded/SimpleConfigurationStubbed.xml");
		MatchUtils.assertXmlEquals(stubbedExpected, stubbedResult);

	}

	//Both OLD and NEW configuration parsers should set the same values for 'loadedConfiguration': properties resolved, secrets hidden
	//The old configuration parser returns the configuration with all property references resolved
	@Test
	public void testOldSchoolConfigurationParser() throws Exception {
		ConfigurationDigester digester = new ConfigurationDigester();
		AppConstants.getInstance().put("properties.hide", "secret");
		Resource resource = Resource.getResource("/Digester/SimpleConfiguration/Configuration.xml");
		Properties properties = new Properties();
		properties.setProperty("HelloWorld.active", "false");
		properties.setProperty("HelloBeautifulWorld.active", "!false");
		properties.setProperty("digester.property", "[ &gt;&quot;&lt; ]"); // old style escaped property values
		properties.setProperty("secret", "GEHEIM");
		properties.setProperty("properties.hide", "secret");
		Configuration configuration = new TestConfiguration();

		String result = digester.resolveEntitiesAndProperties(configuration, resource, properties, false);
		//Unfortunately we need to cleanup the result a bit...
		result = cleanupOldStyleResult(result);
		String expected = TestFileUtils.getTestFile("/Digester/Loaded/SimpleConfigurationUnresolved.xml");
		expected = StringResolver.substVars(expected, properties);
		MatchUtils.assertXmlSimilar(expected, result);

		String storedResult = configuration.getLoadedConfiguration();
		//Unfortunately we need to cleanup the result a bit...
		storedResult = cleanupOldStyleResult(storedResult);
		String storedExpected = TestFileUtils.getTestFile("/Digester/Loaded/SimpleConfigurationResolvedAndHidden.xml");
		MatchUtils.assertXmlSimilar(storedExpected, storedResult);

		digester = new ConfigurationDigester() {
			@Override
			protected boolean isConfigurationStubbed(ClassLoader classLoader) {
				return true;
			}
		};
		properties.setProperty(STUB4TESTTOOL_CONFIGURATION_KEY, "true");
		String stubbedResult = digester.resolveEntitiesAndProperties(configuration, resource, properties, false);
		//Unfortunately we need to cleanup the result a bit...
		stubbedResult = cleanupOldStyleResult(stubbedResult);
		String stubbedExpected = TestFileUtils.getTestFile("/Digester/Loaded/SimpleConfigurationStubbed.xml");
		stubbedExpected = StringResolver.substVars(stubbedExpected, properties);
		MatchUtils.assertXmlSimilar(stubbedExpected, stubbedResult);
	}

	private String cleanupOldStyleResult(String oldStyleResult) {
		String result = oldStyleResult.replaceAll("(</?module>)", "");//Remove the modules root tag
		result = result.replaceAll("(</?exits>)", "");//Remove the exits tag
		result = result.replace("<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">", "").replace("</root>", "");//Remove the root tag
		return result;
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
		
		String actual = new String(target.toString());

		String expectedConfiguration = TestFileUtils.getTestFile(baseDirectory + "/expected.xml");
		MatchUtils.assertXmlEquals(expectedConfiguration, actual);
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
