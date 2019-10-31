package nl.nn.adapterframework.webcontrol.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.core.*;
import nl.nn.adapterframework.larva.MessageListener;
import nl.nn.adapterframework.larva.TestPreparer;
import nl.nn.adapterframework.larva.TestTool;
import nl.nn.adapterframework.larva.test.IbisTester;
import nl.nn.adapterframework.util.*;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.xml.sax.SAXException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertTrue;

public class IbisConsoleTest {
	private static String SHOW_CONFIGURATION_STATUS_XSLT = "webcontrol/pipes/xsl/ShowConfigurationStatus.xsl";
	private static String SHOW_ENVIRONMENT_VARIABLES_XSLT = "webcontrol/pipes/xsl/ShowEnvironmentVariables.xsl";
	private static Transformer showConfigurationStatusTransformer;
	private static Transformer showEnvironmentVariablesTransformer;
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
	public static void initTest() throws ConfigurationException, TransformerConfigurationException, IOException {
		ibisTester = new IbisTester();
		System.setProperty("HelloWorld.job.active", "false");
		System.setProperty("junit.active", "true");
		System.setProperty("configurations.names", "${instance.name},NotExistingConfig");

		boolean started = ibisTester.initTester();
		assertTrue(started);
		ibisContext = ibisTester.getIbisContext();

		URL showConfigurationStatusUrl = ClassUtils.getResourceURL(IbisConsoleTest.class, SHOW_CONFIGURATION_STATUS_XSLT);
		if (showConfigurationStatusUrl == null) {
			throw new ConfigurationException("cannot find resource [" + SHOW_CONFIGURATION_STATUS_XSLT + "]");
		}
		showConfigurationStatusTransformer = XmlUtils.createTransformer(showConfigurationStatusUrl);

		URL showEnvironmentVariablesUrl = ClassUtils.getResourceURL(IbisConsoleTest.class, SHOW_ENVIRONMENT_VARIABLES_XSLT);
		if (showEnvironmentVariablesUrl == null) {
			throw new ConfigurationException("cannot find resource [" + SHOW_ENVIRONMENT_VARIABLES_XSLT + "]");
		}
		showEnvironmentVariablesTransformer = XmlUtils.createTransformer(showEnvironmentVariablesUrl);
	}

	@Test
	public void runLarva() {
		MessageListener messageListener = new MessageListener();
		AppConstants appConstants = AppConstants.getInstance();
		String appConstantsRealPath = appConstants.getResolvedProperty("webapp.realpath");
		String realPath = appConstantsRealPath + "larva/";

		String rootDirectory = TestPreparer.initScenariosRootDirectories(realPath, null, appConstants);
		rootDirectory = "C:\\Users\\murat\\Documents\\Integration Partners\\iaf\\example\\src\\test\\resources\\TestTool\\";
		System.out.println(rootDirectory);
		appConstants = TestPreparer.getAppConstantsFromDirectory(rootDirectory, appConstants);
		JSONObject scenarioFiles = new JSONObject(TestPreparer.readScenarioFiles(rootDirectory, false, appConstants));
		System.out.println(scenarioFiles);
		TestTool testTool = new TestTool(messageListener);
		String paramExecute = "C:\\Users\\murat\\Documents\\Integration Partners\\iaf\\example\\src\\test\\resources\\TestTool\\";
		int tests = testTool.runScenarios(paramExecute, 100, rootDirectory, 1, Integer.MAX_VALUE);
		System.out.println(rootDirectory);
		System.out.println(tests);
		JSONArray jsonArray = messageListener.getMessages();
		try {
			FileWriter fileWriter = new FileWriter("C:\\Users\\murat\\Desktop\\out.txt");
		}catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(jsonArray.toString());
		assertTrue(true);
	}

	@Test
	public void ShowConfigurationStatus_all() throws ConfigurationException, PipeRunException, DomBuilderException, SAXException, IOException, TransformerException {
		ShowConfigurationStatus showConfigurationStatus = new ShowConfigurationStatus();
		PipeLine pipeLine = new PipeLine();
		Adapter adapter = (Adapter) ibisContext.getIbisManager().getRegisteredAdapter("WebControlShowConfigurationStatus");
		pipeLine.setAdapter(adapter);
		showConfigurationStatus.registerForward(createPipeSuccessForward());
		showConfigurationStatus.configure(pipeLine);
		session.put("configuration", "*ALL*");
		MockHttpServletRequest request = new MockHttpServletRequest();
		session.put(IPipeLineSession.HTTP_REQUEST_KEY, request);
		PipeRunResult pipeRunResult = showConfigurationStatus.doPipe(null, session);
		String result = transformShowConfigurationStatusXml((String) pipeRunResult.getResult());
		// System.out.println("Result [" + result + "]");
		compareXML("webcontrol/pipes/ShowConfigurationStatus_all.xml", result);
	}

	@Test
	public void ShowConfigurationStatus_single() throws ConfigurationException, PipeRunException, DomBuilderException, SAXException, IOException, TransformerException {
		ShowConfigurationStatus showConfigurationStatus = new ShowConfigurationStatus();
		PipeLine pipeLine = new PipeLine();
		Adapter adapter = (Adapter) ibisContext.getIbisManager().getRegisteredAdapter("WebControlShowConfigurationStatus");
		pipeLine.setAdapter(adapter);
		showConfigurationStatus.registerForward(createPipeSuccessForward());
		showConfigurationStatus.configure(pipeLine);
		session.put("configuration", "Ibis4Example");
		MockHttpServletRequest request = new MockHttpServletRequest();
		session.put(IPipeLineSession.HTTP_REQUEST_KEY, request);
		PipeRunResult pipeRunResult = showConfigurationStatus.doPipe(null, session);
		String result = transformShowConfigurationStatusXml((String) pipeRunResult.getResult());
		// System.out.println("Result [" + result + "]");
		compareXML("webcontrol/pipes/ShowConfigurationStatus_single.xml", result);
	}

	@Test
	public void showEnvironmentVariables() throws ConfigurationException, PipeRunException, DomBuilderException, SAXException, IOException, TransformerException {
		ShowEnvironmentVariables showEnvironmentVariables = new ShowEnvironmentVariables();
		PipeLine pipeLine = new PipeLine();
		Adapter adapter = (Adapter) ibisContext.getIbisManager().getRegisteredAdapter("WebControlShowEnvironmentVariables");
		pipeLine.setAdapter(adapter);
		showEnvironmentVariables.registerForward(createPipeSuccessForward());
		showEnvironmentVariables.configure(pipeLine);
		// session.put("configuration", "Ibis4Example");
		MockHttpServletRequest request = new MockHttpServletRequest();
		session.put(IPipeLineSession.HTTP_REQUEST_KEY, request);
		PipeRunResult pipeRunResult = showEnvironmentVariables.doPipe(null, session);
		String result = transformShowEnvironmentVariablesXml((String) pipeRunResult.getResult());
		// System.out.println("Result [" + result + "]");
		compareXML("webcontrol/pipes/ShowEnvironmentVariables.xml", result);
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

	private void compareXML(String expectedFile, String result) throws SAXException, IOException, DomBuilderException, TransformerException {
		URL expectedUrl = ClassUtils.getResourceURL(this, expectedFile);
		if (expectedUrl == null) {
			throw new IOException("cannot find resource [" + expectedUrl + "]");
		}
		String expected = Misc.resourceToString(expectedUrl);
		Diff diff = XMLUnit.compareXML(expected, result);
		assertTrue(diff.toString(), diff.identical());
	}

	private String transformShowConfigurationStatusXml(String showConfigurationStatusXml)
			throws DomBuilderException, TransformerException, IOException {
		return XmlUtils.transformXml(showConfigurationStatusTransformer, showConfigurationStatusXml);

	}

	private String transformShowEnvironmentVariablesXml(String showEnvironmentVariablesXml)
			throws DomBuilderException, TransformerException, IOException {
		return XmlUtils.transformXml(showEnvironmentVariablesTransformer, showEnvironmentVariablesXml);

	}
}
