package nl.nn.adapterframework.extensions.cmis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.ObjectFactory;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.runtime.ObjectIdImpl;
import org.apache.chemistry.opencmis.client.runtime.OperationContextImpl;
import org.apache.chemistry.opencmis.client.runtime.repository.ObjectFactoryImpl;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.senders.SenderTestBase;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.ParameterBuilder;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.util.Misc;

@RunWith(Parameterized.class)
public class TestGetAction extends SenderTestBase<CmisSender>{

	@Rule
	public ExpectedException exception = ExpectedException.none();

	private final static String INPUT_WITH_PROPERTIES = "<cmis><id>id</id><objectId>dummy</objectId>"
			+ "<objectTypeId>cmis:document</objectTypeId><fileName>fileInput.txt</fileName>"
			+ "<properties><property name=\"project:number\" type=\"integer\">123456789</property>"
			+ "<property name=\"project:lastModified\" type=\"datetime\">2019-02-26T16:31:15</property>"
			+ "<property name=\"project:onTime\" type=\"boolean\">true</property></properties></cmis>";
	
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

	@Override
	public CmisSender createSender() throws Exception {
		CmisSender sender = spy(new CmisSender());

		sender.setUrl("http://dummy.url");
		sender.setRepository("dummyRepository");
		sender.setUsername("test");
		sender.setPassword("test");
		sender.setKeepSession(false);

		HttpServletResponse response = mock(HttpServletResponse.class);
		session.put(PipeLineSession.HTTP_RESPONSE_KEY, response);
		ServletOutputStream outputStream = mock(ServletOutputStream.class);
		try {
			doReturn(outputStream).when(response).getOutputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Session cmisSession = mock(Session.class);
		ObjectFactory objectFactory = mock(ObjectFactoryImpl.class);
		doReturn(objectFactory).when(cmisSession).getObjectFactory();

//		GENERIC cmis object
		ObjectId objectId = mock(ObjectIdImpl.class);
		doReturn(objectId).when(cmisSession).createObjectId(anyString());
		CmisObject cmisObject = CmisTestObject.newInstance();
		doReturn(cmisObject).when(cmisSession).getObject(any(ObjectId.class));
		doReturn(cmisObject).when(cmisSession).getObject(any(ObjectId.class), any(OperationContext.class));

//		GET
		OperationContext operationContext = mock(OperationContextImpl.class);
		doReturn(operationContext).when(cmisSession).createOperationContext();

		doReturn(cmisSession).when(sender).createCmisSession(any());

		return sender;
	}

	public void configure() throws ConfigurationException, SenderException, TimeoutException {
		sender.setGetProperties(getProperties);
		sender.setGetDocumentContent(getDocumentContent);

		sender.setStreamResultToServlet(resultToServlet);
		sender.setBindingType(bindingType);
		sender.setAction(action);
		sender.configure();
	}

	public void configureWithParameters() throws ConfigurationException, SenderException, TimeoutException {
		sender.addParameter(new ParameterBuilder("getProperties", getProperties.toString()));
		sender.addParameter(new ParameterBuilder("getDocumentContent", getDocumentContent.toString()));

		configure();
	}

	@Test
	public void sendMessageFileStream() throws ConfigurationException, SenderException, TimeoutException, IOException {
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
	public void sendMessageStreamResult() throws ConfigurationException, SenderException, TimeoutException, IOException {
		sender.setBindingType(bindingType);
		sender.setAction(action);
		sender.configure();

		Message actualResult = sender.sendMessage(input, session);
		assertTrue(actualResult.asObject() instanceof InputStream);
		assertEquals(GET_RESULT_FOR_INPUT, actualResult.asString());
	}

	@Test
	public void sendMessageFileContent() throws ConfigurationException, SenderException, TimeoutException, IOException {
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
	public void sendMessageFileStreamWithParameters() throws ConfigurationException, SenderException, TimeoutException, IOException {
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
	public void sendMessageFileContentWithParameters() throws ConfigurationException, SenderException, TimeoutException, IOException {
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
