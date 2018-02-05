package nl.nn.adapterframework.webcontrol.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.extensions.test.IbisTester;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlUtils;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

public class ShowConfigurationStatusTest {
	private static String adapters_xslt = "webcontrol/xsl/adapters.xsl";
	private static Transformer adaptersTransformer;
	private static IbisTester ibisTester;
	private static IbisContext ibisContext;

	@Before
	public void initXMLUnit() throws IOException {
		XMLUnit.setIgnoreWhitespace(true);
		XMLUnit.setIgnoreDiffBetweenTextAndCDATA(true);
	}

	@BeforeClass
	public static void initTest() throws ConfigurationException,
			TransformerConfigurationException, IOException {
		URL adaptersUrl = ClassUtils.getResourceURL(
				ShowConfigurationStatusTest.class, adapters_xslt);
		if (adaptersUrl == null) {
			throw new ConfigurationException("cannot find resource ["
					+ adapters_xslt + "]");
		}
		adaptersTransformer = XmlUtils.createTransformer(adaptersUrl, true);

		ibisTester = new IbisTester();
		System.setProperty("HelloWorld.job.active", "false");
		System.setProperty("junit.active", "true");
		System.setProperty("configurations.names",
				"${instance.name},NotExistingConfig");
		ibisTester.initTest();
		if (ibisTester.testStartAdapters()) {
			ibisContext = ibisTester.getIbisContext();
		}
		assertEquals(true, ibisContext != null);
	}

	@Test
	public void testConfigurationsXml() throws IOException, SAXException,
			DomBuilderException, TransformerException {
		List<Configuration> allConfigurations = ibisContext.getIbisManager()
				.getConfigurations();
		ShowConfigurationStatus showConfigurationStatus = new ShowConfigurationStatus();
		String configurationsXml = showConfigurationStatus.toConfigurationsXml(
				allConfigurations).toXML();
		compareXML("webcontrol/configurations.xml", configurationsXml);
	}

	@Test
	public void testAdaptersXmlAll() throws IOException, SAXException,
			DomBuilderException, TransformerException {
		List<Configuration> allConfigurations = ibisContext.getIbisManager()
				.getConfigurations();
		ShowConfigurationStatus showConfigurationStatus = new ShowConfigurationStatus();
		List<IAdapter> registeredAdapters = ibisContext.getIbisManager()
				.getRegisteredAdapters();
		String adaptersXml = showConfigurationStatus.toAdaptersXml(ibisContext,
				allConfigurations, null, registeredAdapters).toXML();
		compareXML("webcontrol/adaptersAll.xml", adaptersXml, "adapters");
	}

	@Test
	public void testAdaptersXmlSingle() throws IOException, SAXException,
			DomBuilderException, TransformerException {
		List<Configuration> allConfigurations = ibisContext.getIbisManager()
				.getConfigurations();
		ShowConfigurationStatus showConfigurationStatus = new ShowConfigurationStatus();
		List<IAdapter> registeredAdapters = ibisContext.getIbisManager()
				.getRegisteredAdapters();
		Configuration configurationSelected = ibisContext.getIbisManager()
				.getConfiguration("Ibis4Example");
		String adaptersXml = showConfigurationStatus.toAdaptersXml(ibisContext,
				allConfigurations, configurationSelected, registeredAdapters)
				.toXML();
		compareXML("webcontrol/adaptersSingle.xml", adaptersXml, "adapters");
	}

	@AfterClass
	public static void closeTest() {
		ibisTester.closeTest();
	}

	private void compareXML(String expectedFile, String result)
			throws SAXException, IOException, DomBuilderException,
			TransformerException {
		compareXML(expectedFile, result, null);
	}

	private void compareXML(String expectedFile, String result, String id)
			throws SAXException, IOException, DomBuilderException,
			TransformerException {
		URL expectedUrl = ClassUtils.getResourceURL(this, expectedFile);
		if (expectedUrl == null) {
			throw new IOException("cannot find resource [" + expectedUrl + "]");
		}
		String expected = Misc.resourceToString(expectedUrl);
		if ("adapters".equals(id)) {
			expected = transformAdaptersXml(expected);
			result = transformAdaptersXml(result);
		}
		// System.out.println("Expected [" + expected + "]");
		// System.out.println("Result [" + result + "]");
		Diff diff = XMLUnit.compareXML(expected, result);
		assertTrue(diff.toString(), diff.identical());
	}

	private String transformAdaptersXml(String adaptersXml)
			throws DomBuilderException, TransformerException, IOException {
		return XmlUtils.transformXml(adaptersTransformer, adaptersXml);

	}
}
