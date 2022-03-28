package nl.nn.adapterframework.extensions.cmis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.util.Misc;

@RunWith(Parameterized.class)
public class TestGetAction extends CmisSenderTestBase {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	private final static String INPUT_WITH_PROPERTIES = "<cmis><id>id</id><objectId>dummy</objectId>"
			+ "<objectTypeId>cmis:document</objectTypeId><fileName>fileInput.txt</fileName>"
			+ "<properties><property name=\"cmis:description\" type=\"string\">123456789</property>"
			+ "<property name=\"cmis:lastModificationDate\" type=\"datetime\">2019-02-26T16:31:15</property>"
			+ "<property name=\"cmis:creationDate\" type=\"boolean\">true</property></properties></cmis>";
	
	private final static String GET_RESULT_FOR_INPUT= "dummy_stream";

	private final static String GET_RESULT_TO_SERVLET= null;

	private final static String GET_RESULT_FOR_GET_PROPERTIES = "<cmis><properties>"
			+ "<property name=\"cmis:name\" type=\"id\">dummy</property>"
			+ "<property name=\"project:number\" type=\"integer\">123456789</property>"
			+ "<property name=\"project:lastModified\" type=\"datetime\">2019-02-26T16:31:15</property>"
			+ "<property name=\"project:onTime\" type=\"boolean\">true</property></properties></cmis>";

	@Parameters(name = "{0} - {1} - toServlet = {4} - getProperties = {5} - getDocumentContent = {6}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{ "atompub", "get", INPUT_WITH_PROPERTIES, GET_RESULT_FOR_INPUT, false, false, false},
				{ "atompub", "get", INPUT_WITH_PROPERTIES, GET_RESULT_TO_SERVLET, true, false, false},
				{ "atompub", "get", INPUT_WITH_PROPERTIES, GET_RESULT_FOR_GET_PROPERTIES, false, true, false},
				{ "atompub", "get", INPUT_WITH_PROPERTIES, GET_RESULT_FOR_GET_PROPERTIES, false, true, true},

				{ "webservices", "get", INPUT_WITH_PROPERTIES, GET_RESULT_FOR_INPUT, false, false, false},
				{ "webservices", "get", INPUT_WITH_PROPERTIES, GET_RESULT_TO_SERVLET, true, false, false},
				{ "webservices", "get", INPUT_WITH_PROPERTIES, GET_RESULT_FOR_GET_PROPERTIES, false, true, false},
				{ "webservices", "get", INPUT_WITH_PROPERTIES, GET_RESULT_FOR_GET_PROPERTIES, false, true, true},

				{ "browser", "get", INPUT_WITH_PROPERTIES, GET_RESULT_FOR_INPUT, false, false, false},
				{ "browser", "get", INPUT_WITH_PROPERTIES, GET_RESULT_TO_SERVLET, true, false, false},
				{ "browser", "get", INPUT_WITH_PROPERTIES, GET_RESULT_FOR_GET_PROPERTIES, false, true, false},
				{ "browser", "get", INPUT_WITH_PROPERTIES, GET_RESULT_FOR_GET_PROPERTIES, false, true, true},
		});
	}

	private String bindingType;
	private String action;
	private Message input;
	private String expectedResult;
	private Boolean resultToServlet;
	private Boolean getProperties;
	private Boolean getDocumentContent;

	public TestGetAction(String bindingType, String action, String input, String expected, Boolean resultToServlet, Boolean getProperties, Boolean getDocumentContent) {
		this.bindingType = bindingType;
		this.action = action;
		this.input = new Message(input);
		this.expectedResult = expected;
		this.resultToServlet = resultToServlet;
		this.getProperties = getProperties;
		this.getDocumentContent = getDocumentContent;
	}

	public void configure() throws Exception {
		sender.setGetProperties(getProperties);
		sender.setGetDocumentContent(getDocumentContent);

		sender.setStreamResultToServlet(resultToServlet);
		sender.setBindingType(bindingType);
		sender.setAction(action);
		sender.configure();

		if(!STUBBED) {
			sender.open();
		}
	}

	public void configureWithParameters() throws Exception {
		sender.addParameter(new Parameter("getProperties", getProperties.toString()));
		sender.addParameter(new Parameter("getDocumentContent", getDocumentContent.toString()));

		configure();
	}

	@Test
	public void sendMessageFileStream() throws Exception {
		sender.setFileInputStreamSessionKey("fis");
		configure();

		String actualResult = sender.sendMessage(input, session).asString();
		if(!getProperties && !resultToServlet) {
			assertNull(actualResult);
		} else {
			TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
		}

		InputStream stream = (InputStream) session.get(sender.getFileSessionKey());
		if((getProperties && getDocumentContent) || (!getProperties && !resultToServlet)) {
			assertEquals(GET_RESULT_FOR_INPUT, Misc.streamToString(stream));
		} else {
			assertNull(stream);
		}
	}

	@Test
	public void sendMessageStreamResult() throws Exception {
		sender.setBindingType(bindingType);
		sender.setAction(action);
		sender.configure();

		Message actualResult = sender.sendMessage(input, session);
		assertTrue(actualResult.asObject() instanceof InputStream);
		assertEquals(GET_RESULT_FOR_INPUT, actualResult.asString());
	}

	@Test
	public void sendMessageFileContentSessionKey() throws Exception {
		sender.setFileContentSessionKey("fileContent");
		configure();

		String actualResult = sender.sendMessage(input, session).asString();
		if(!getProperties && !resultToServlet) {
			assertNull(actualResult);
		} else {
			TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
		}

		String base64Content = (String) session.get(sender.getFileSessionKey());
		if((getProperties && getDocumentContent) || (!getProperties && !resultToServlet)) {
			assertEquals("ZHVtbXlfc3RyZWFt", base64Content);
		} else {
			assertNull(base64Content);
		}
	}

	@Test
	public void sendMessageFileStreamWithParameters() throws Exception {
		sender.setFileInputStreamSessionKey("fis");
		configureWithParameters();

		String actualResult = sender.sendMessage(input, session).asString();
		if(!getProperties && !resultToServlet) {
			assertNull(actualResult);
		} else {
			TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
		}

		InputStream stream = (InputStream) session.get(sender.getFileSessionKey());
		if((getProperties && getDocumentContent) || (!getProperties && !resultToServlet)) {
			assertEquals(GET_RESULT_FOR_INPUT, Misc.streamToString(stream));
		} else {
			assertNull(stream);
		}
	}

	@Test
	public void sendMessageFileContentWithParameters() throws Exception {
		sender.setFileContentSessionKey("fileContent");
		configureWithParameters();

		String actualResult = sender.sendMessage(input, session).asString();
		if(!getProperties && !resultToServlet) {
			assertNull(actualResult);
		} else {
			TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
		}

		String base64Content = (String) session.get(sender.getFileSessionKey());
		if((getProperties && getDocumentContent) || (!getProperties && !resultToServlet)) {
			assertEquals("ZHVtbXlfc3RyZWFt", base64Content);
		} else {
			assertNull(base64Content);
		}
	}
}
