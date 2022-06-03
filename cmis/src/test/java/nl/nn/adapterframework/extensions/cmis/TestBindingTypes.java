package nl.nn.adapterframework.extensions.cmis;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestAssertions;

@RunWith(Parameterized.class)
public class TestBindingTypes extends CmisSenderTestBase {

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
			+ "<relationships>    <relation>dummy</relation>  </relationships>"
			+ "</cmis>";

	private static String createActionExpectedBase64 = "ZmlsZUlucHV0LnR4dA==";
	private static String updateActionExpectedBase64 = "aWQ=";

	@Parameters(name = "{0} - {1}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{ "atompub", "create", INPUT, createActionExpectedBase64 },
				{ "atompub", "get", INPUT, "dummy_stream" },
				{ "atompub", "find", FIND_INPUT, FIND_RESULT },
				{ "atompub", "update", INPUT, updateActionExpectedBase64 },
				{ "atompub", "fetch", INPUT, FETCH_RESULT },

				{ "webservices", "create", INPUT, createActionExpectedBase64 },
				{ "webservices", "get", INPUT, "dummy_stream" },
				{ "webservices", "find", FIND_INPUT, FIND_RESULT },
				{ "webservices", "update", INPUT, updateActionExpectedBase64 },
				{ "webservices", "fetch", INPUT, FETCH_RESULT },

				{ "browser", "create", INPUT, createActionExpectedBase64 },
				{ "browser", "get", INPUT, "dummy_stream" },
				{ "browser", "find", FIND_INPUT, FIND_RESULT },
				{ "browser", "update", INPUT, updateActionExpectedBase64 },
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

	@Override
	public CmisSender createSender() throws Exception {
		CmisSender sender = super.createSender();
		sender.setFileContentSessionKey("fileContent");
		sender.setFileNameSessionKey("my-file");

		byte[] base64 = Base64.encodeBase64("dummy data".getBytes());
		session.put("fileContent", new String(base64));
		HttpServletResponse response = mock(HttpServletResponse.class);
		session.put(PipeLineSession.HTTP_RESPONSE_KEY, response);

		return sender;
	}

	@Test
	public void configure() throws ConfigurationException {
		sender.setBindingType(bindingType);
		sender.setAction(action);
		sender.configure();
	}

	@Test
	public void sendMessage() throws ConfigurationException, SenderException, TimeoutException, IOException {

		if(action.equals("get")) {
			sender.setFileContentSessionKey("");
			sender.setFileNameSessionKey("");
		}

		configure();

		String actualResult = sender.sendMessage(input, session).asString();
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
	}

	@Test
	public void sendMessageWithContentStream() throws ConfigurationException, SenderException, TimeoutException, IOException {
		if(!action.equals("get")) return;

		configure();

		assertTrue(Message.isEmpty(sender.sendMessage(input, session)));
		String base64 = (String) session.get("fileContent");
		TestAssertions.assertEqualsIgnoreRNTSpace(Base64.encodeBase64String(expectedResult.getBytes()), base64);
	}
}
