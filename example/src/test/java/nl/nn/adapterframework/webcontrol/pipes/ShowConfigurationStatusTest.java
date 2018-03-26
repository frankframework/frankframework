package nl.nn.adapterframework.webcontrol.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.xml.sax.SAXException;

public class ShowConfigurationStatusTest {
	private static String adapters_xslt = "webcontrol/xsl/adapters.xsl";
	private static Transformer adaptersTransformer;
	private static IbisTester ibisTester;
	private static IbisContext ibisContext;
	private IPipeLineSession session;

	@Before
	public void initXMLUnit() throws IOException {
		XMLUnit.setIgnoreWhitespace(true);
		XMLUnit.setIgnoreDiffBetweenTextAndCDATA(true);
		session = new PipeLineSessionBase();
		session.put("method", "GET");
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
	public void testAllConfigs() throws ConfigurationException,
			PipeRunException, DomBuilderException, SAXException, IOException,
			TransformerException {
		ShowConfigurationStatus showConfigurationStatus = new ShowConfigurationStatus();
		PipeLine pipeLine = new PipeLine();
		Adapter adapter = (Adapter) ibisContext.getIbisManager()
				.getRegisteredAdapter("WebControlShowConfigurationStatus");
		pipeLine.setAdapter(adapter);
		showConfigurationStatus.registerForward(createPipeSuccessForward());
		showConfigurationStatus.configure(pipeLine);
		session.put("configuration", "*ALL*");
		MockHttpServletRequest request = new MockHttpServletRequest();
		session.put(IPipeLineSession.HTTP_REQUEST_KEY, request);
		PipeRunResult pipeRunResult = showConfigurationStatus.doPipe(null,
				session);
		compareXML("webcontrol/allConfigs.xml",
				(String) pipeRunResult.getResult());
	}

	@Test
	public void testSingleConfig() throws ConfigurationException,
			PipeRunException, DomBuilderException, SAXException, IOException,
			TransformerException {
		ShowConfigurationStatus showConfigurationStatus = new ShowConfigurationStatus();
		PipeLine pipeLine = new PipeLine();
		Adapter adapter = (Adapter) ibisContext.getIbisManager()
				.getRegisteredAdapter("WebControlShowConfigurationStatus");
		pipeLine.setAdapter(adapter);
		showConfigurationStatus.registerForward(createPipeSuccessForward());
		showConfigurationStatus.configure(pipeLine);
		session.put("configuration", "Ibis4Example");
		MockHttpServletRequest request = new MockHttpServletRequest();
		session.put(IPipeLineSession.HTTP_REQUEST_KEY, request);
		PipeRunResult pipeRunResult = showConfigurationStatus.doPipe(null,
				session);
		compareXML("webcontrol/singleConfig.xml",
				(String) pipeRunResult.getResult());
	}

	@AfterClass
	public static void closeTest() {
		ibisTester.closeTest();
	}

	private PipeForward createPipeSuccessForward() {
		PipeForward pipeForward = new PipeForward();
		pipeForward.setName("success");
		return pipeForward;
	}

	private void compareXML(String expectedFile, String result)
			throws SAXException, IOException, DomBuilderException,
			TransformerException {
		URL expectedUrl = ClassUtils.getResourceURL(this, expectedFile);
		if (expectedUrl == null) {
			throw new IOException("cannot find resource [" + expectedUrl + "]");
		}
		String expected = Misc.resourceToString(expectedUrl);
		expected = transformAdaptersXml(expected);
		result = transformAdaptersXml(result);
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
