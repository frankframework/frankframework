package nl.nn.adapterframework.extensions.cmis;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectFactory;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.QueryResult;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.runtime.FolderImpl;
import org.apache.chemistry.opencmis.client.runtime.ObjectIdImpl;
import org.apache.chemistry.opencmis.client.runtime.OperationContextImpl;
import org.apache.chemistry.opencmis.client.runtime.repository.ObjectFactoryImpl;
import org.apache.chemistry.opencmis.client.runtime.util.EmptyItemIterable;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;

@RunWith(Parameterized.class)
public class TestBindingTypes extends SenderBase<CmisSender>{

	private final static String INPUT = "<cmis><id>id</id><objectId>dummy</objectId><objectTypeId>cmis:document</objectTypeId>"
			+ "<fileName>fileInput.txt</fileName>" + 
			" <properties><property name=\"project:number\" type=\"integer\">123456789</property>" + 
			"<property name=\"project:lastModified\" type=\"datetime\">2019-02-26T16:31:15</property>" + 
			"<property name=\"project:onTime\" type=\"boolean\">true</property></properties></cmis>";
	private final static String FIND_INPUT = "<query><name>dummy</name>\n" + 
			"	<objectId>dummy</objectId>\n" + 
			"	<objectTypeId>dummy</objectTypeId>\n" + 
			"	<maxItems>15</maxItems>\n" + 
			"	<skipCount>0</skipCount>\n" + 
			"	<searchAllVersions>true</searchAllVersions>\n" + 
			"	<properties>\n" + 
			"		<property name=\"cmis:name\">dummy</property>\n" + 
			"		<property name=\"project:number\" type=\"integer\">123456789</property>\n" + 
			"		<property name=\"project:onTime\" type=\"boolean\">true</property>\n" + 
			"		<property name=\"project:lastModified\" type=\"datetime\">2019-02-26T16:29:46</property>\n" + 
			"	</properties>\n" + 
			"	<statement>SELECT * from cmis:document</statement>\n" + 
			"	<filter>cmis:objectId</filter>\n" + 
			"	<includeAllowableActions>true</includeAllowableActions>\n" + 
			"	<includePolicies>true</includePolicies>\n" + 
			"	<includeAcl>true</includeAcl> </query>";
	private final static String FIND_RESULT = "<cmis totalNumItems=\"0\">  <rowset /></cmis>";
	private final static String FETCH_RESULT = "<cmis>  <properties>    "
			+ "<property name=\"cmis:name\" type=\"id\">dummy</property>    "
			+ "<property name=\"project:number\" type=\"integer\">123456789</property>    "
			+ "<property name=\"project:lastModified\" type=\"datetime\">2019-02-26T16:31:15</property>    "
			+ "<property name=\"project:onTime\" type=\"boolean\">true</property>  "
			+ "</properties>  <allowableActions>    <action>canCreateDocument</action>  </allowableActions>  <isExactAcl>false</isExactAcl>  "
			+ "<policyIds>    <policyId>dummyObjectId</policyId>  </policyIds>  "
			+ "<relationships>    <relation>dummyId</relation>  </relationships>"
			+ "</cmis>";

	@Parameters(name = "{0} - {1}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{ "atompub", "create", INPUT, "dummy_id" },
				{ "atompub", "get", INPUT, "dummy_stream" },
				{ "atompub", "find", FIND_INPUT, FIND_RESULT },
				{ "atompub", "update", INPUT, "dummy_id" },
				{ "atompub", "fetch", INPUT, FETCH_RESULT },

				{ "webservices", "create", INPUT, "dummy_id" },
				{ "webservices", "get", INPUT, "dummy_stream" },
				{ "webservices", "find", FIND_INPUT, FIND_RESULT },
				{ "webservices", "update", INPUT, "dummy_id" },
				{ "webservices", "fetch", INPUT, FETCH_RESULT },

				{ "browser", "create", INPUT, "dummy_id" },
				{ "browser", "get", INPUT, "dummy_stream" },
				{ "browser", "find", FIND_INPUT, FIND_RESULT },
				{ "browser", "update", INPUT, "dummy_id" },
				{ "browser", "fetch", INPUT, FETCH_RESULT },
		});
	}

	private String bindingType;
	private String action;
	private Message input;
	private String expectedResult;

	public TestBindingTypes(String bindingType, String action, String input, String expected) {
		this.bindingType = bindingType;
		this.action = action;
		this.input = new Message(input);
		this.expectedResult = expected;
	}

	@SuppressWarnings("unchecked")
	@Override
	public CmisSender createSender() throws ConfigurationException {
		CmisSender sender = spy(new CmisSender());

		sender.setUrl("http://dummy.url");
		sender.setRepository("dummyRepository");
		sender.setFileContentSessionKey("fileContent");
		sender.setFileNameSessionKey("my-file");
		sender.setUsername("test");
		sender.setPassword("test");
		sender.setKeepSession(false);

		byte[] base64 = Base64.encodeBase64("dummy data".getBytes());
		session.put("fileContent", new String(base64));
		HttpServletResponse response = mock(HttpServletResponse.class);
		session.put(PipeLineSession.HTTP_RESPONSE_KEY, response);

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

//		CREATE
		Folder folder = mock(FolderImpl.class);
		doReturn(cmisObject).when(folder).createDocument(anyMap(), any(ContentStreamImpl.class), any(VersioningState.class));
		doReturn(folder).when(cmisSession).getRootFolder();

//		FIND
		ItemIterable<QueryResult> query = new EmptyItemIterable<QueryResult>();
		doReturn(query).when(cmisSession).query(anyString(), anyBoolean(), any(OperationContext.class));

		try {
			doReturn(cmisSession).when(sender).createCmisSession(any(ParameterValueList.class));
		} catch (SenderException e) {
			//Since we stub the entire session it won't throw exceptions
		}

		return sender;
	}

	@Test
	public void configure() throws ConfigurationException, SenderException, TimeOutException {
		sender.setBindingType(bindingType);
		sender.setAction(action);
		sender.configure();
	}

	@Test
	public void sendMessage() throws ConfigurationException, SenderException, TimeOutException, IOException {

		if(action.equals("get")) {
			sender.setFileContentSessionKey("");
			sender.setFileNameSessionKey("");
		}

		configure();

		String actualResult = sender.sendMessage(input, session).asString();
		assertEqualsIgnoreRN(expectedResult, actualResult);
	}

	@Test
	public void sendMessageWithContentStream() throws ConfigurationException, SenderException, TimeOutException, IOException {
		if(!action.equals("get")) return;

		configure();

		assertTrue(Message.isEmpty(sender.sendMessage(input, session)));
		String base64 = (String) session.get("fileContent");
		assertEqualsIgnoreRN(Base64.encodeBase64String(expectedResult.getBytes()), base64);
	}
}
