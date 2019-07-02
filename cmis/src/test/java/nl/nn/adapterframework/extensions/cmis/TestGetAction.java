package nl.nn.adapterframework.extensions.cmis;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

@RunWith(Parameterized.class)
public class TestGetAction extends SenderBase<CmisSender>{
	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	private final static String INPUT_WITH_PROPERTIES = "<cmis><id>id</id><objectId>dummy</objectId>"
			+ "<objectTypeId>cmis:document</objectTypeId><fileName>fileInput.txt</fileName>"
			+ "<properties><property name=\"project:number\" type=\"integer\">123456789</property>"
			+ "<property name=\"project:lastModified\" type=\"datetime\">2019-02-26T16:31:15</property>"
			+ "<property name=\"project:onTime\" type=\"boolean\">true</property></properties></cmis>";
	
	private final static String GET_RESULT_FOR_INPUT= "dummy_stream";
	
	private final static String GET_RESULT_TO_SERVLET= "";
	
	private final static String GET_RESULT_FOR_GET_PROPERTIES = "<cmis><properties>"
			+ "<property name=\"cmis:name\">dummy</property>"
			+ "<property name=\"project:number\" type=\"integer\">123456789</property>"
			+ "<property name=\"project:lastModified\" type=\"datetime\">2019-02-26T16:31:15</property>"
			+ "<property name=\"project:onTime\" type=\"boolean\">true</property></properties></cmis>";
	
	@Parameters(name = "{0} - {1} - toServlet = {4} - getProperties = {5}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{ "atompub", "get", INPUT_WITH_PROPERTIES, GET_RESULT_FOR_INPUT, false, false},
				{ "atompub", "get", INPUT_WITH_PROPERTIES, GET_RESULT_TO_SERVLET, true, false},
				{ "atompub", "get", INPUT_WITH_PROPERTIES, GET_RESULT_FOR_GET_PROPERTIES, false, true},

				{ "webservices", "get", INPUT_WITH_PROPERTIES, GET_RESULT_FOR_INPUT, false, false},
				{ "webservices", "get", INPUT_WITH_PROPERTIES, GET_RESULT_TO_SERVLET, true, false},
				{ "webservices", "get", INPUT_WITH_PROPERTIES, GET_RESULT_FOR_GET_PROPERTIES, false, true},
				
				{ "browser", "get", INPUT_WITH_PROPERTIES, GET_RESULT_FOR_INPUT, false, false},
				{ "browser", "get", INPUT_WITH_PROPERTIES, GET_RESULT_TO_SERVLET, true, false},
				{ "browser", "get", INPUT_WITH_PROPERTIES, GET_RESULT_FOR_GET_PROPERTIES, false, true},
				
		});
	}
	
	private String bindingType;
	private String action;
	private String input;
	private String expectedResult;
	private Boolean resultToServlet;
	private Boolean getProperties;

	public TestGetAction(String bindingType, String action, String input, String expected, Boolean resultToServlet, Boolean getProperties) {
		this.bindingType = bindingType;
		this.action = action;
		this.input = input;
		this.expectedResult = expected;
		this.resultToServlet = resultToServlet;
		this.getProperties = getProperties;
	}
	@Override
	public CmisSender createSender() throws ConfigurationException {
		CmisSender sender = spy(new CmisSender());

		sender.setUrl("http://dummy.url");
		sender.setRepository("dummyRepository");
		sender.setUserName("test");
		sender.setPassword("test");
		sender.setKeepSession(false);

		
		HttpServletResponse response = mock(HttpServletResponse.class);
		session.put(IPipeLineSession.HTTP_RESPONSE_KEY, response);
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
		CmisObject cmisObject = spy(new CmisTestObject());
		doReturn(cmisObject).when(cmisSession).getObject(any(ObjectId.class));
		doReturn(cmisObject).when(cmisSession).getObject(any(ObjectId.class), any(OperationContext.class));
		
//		GET
		OperationContext operationContext = mock(OperationContextImpl.class);
		doReturn(operationContext).when(cmisSession).createOperationContext();
		
		try {
			doReturn(cmisSession).when(sender).createSession(any(ParameterResolutionContext.class));
		} catch (SenderException e) {
			//Since we stub the entire session it won't throw exceptions
		}

		return sender;
	}
	
	public void configure() throws ConfigurationException, SenderException, TimeOutException {
		sender.setBindingType(bindingType);
		sender.setAction(action);
		sender.configure();
	}

	@Test
	public void sendMessageFileStream() throws ConfigurationException, SenderException, TimeOutException, FileNotFoundException {
		configure();
		ParameterResolutionContext prc = new ParameterResolutionContext("input", session);
		
		session.put("fis", new FileInputStream(getClass().getResource("/fileInput.txt").getFile()));
		sender.setFileInputStreamSessionKey("fis");
		sender.setStreamResultToServlet(resultToServlet);
		
		sender.setGetProperties(getProperties);
		String actualResult = sender.sendMessage(bindingType+"-"+action, input, prc);
		assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
	}
	
	@Test
	public void sendMessageFileContent() throws ConfigurationException, SenderException, TimeOutException, FileNotFoundException {
		configure();
		ParameterResolutionContext prc = new ParameterResolutionContext("input", session);
		
		session.put("fileContent", "some content".getBytes());
		sender.setFileContentSessionKey("fileContent");
		sender.setStreamResultToServlet(resultToServlet);
		
		sender.setGetProperties(getProperties);
		String actualResult = sender.sendMessage(bindingType+"-"+action, input, prc);
		assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
	}
}
