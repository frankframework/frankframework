package nl.nn.adapterframework.extensions.cmis;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.stream.Stream;

import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.extensions.cmis.CmisSender.CmisAction;
import nl.nn.adapterframework.extensions.cmis.CmisSessionBuilder.BindingTypes;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestAssertions;

public class TestBindingTypes extends CmisSenderTestBase {

	private static final String INPUT = "<cmis><id>id</id><objectId>dummy</objectId><objectTypeId>cmis:document</objectTypeId>"
			+ "<fileName>fileInput.txt</fileName>" +
			" <properties><property name=\"project:number\" type=\"integer\">123456789</property>" +
			"<property name=\"project:lastModified\" type=\"datetime\">2019-02-26T16:31:15</property>" +
			"<property name=\"project:onTime\" type=\"boolean\">true</property></properties></cmis>";
	private static final String FIND_INPUT = "<query><name>dummy</name>\n" +
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
	private static final String FIND_RESULT = "<cmis totalNumItems=\"0\">  <rowset /></cmis>";
	private static final String FETCH_RESULT = "<cmis>  <properties>    "
			+ "<property name=\"cmis:name\" type=\"id\">dummy</property>    "
			+ "<property name=\"project:number\" type=\"integer\">123456789</property>    "
			+ "<property name=\"project:lastModified\" type=\"datetime\">2019-02-26T16:31:15</property>    "
			+ "<property name=\"project:onTime\" type=\"boolean\">true</property>  "
			+ "</properties>  <allowableActions>    <action>canCreateDocument</action>  </allowableActions>  <isExactAcl>false</isExactAcl>  "
			+ "<policyIds>    <policyId>dummyObjectId</policyId>  </policyIds>  "
			+ "<relationships>    <relation>dummy</relation>  </relationships>"
			+ "</cmis>";

	private static final String createActionExpectedBase64 = "ZmlsZUlucHV0LnR4dA==";
	private static final String updateActionExpectedBase64 = "aWQ=";

	public static Stream<Arguments> allImplementations() {
		return Stream.of(
				Arguments.of(BindingTypes.ATOMPUB, CmisAction.CREATE, INPUT, createActionExpectedBase64),
				Arguments.of(BindingTypes.ATOMPUB, CmisAction.GET, INPUT, "dummy_stream"),
				Arguments.of(BindingTypes.ATOMPUB, CmisAction.FIND, FIND_INPUT, FIND_RESULT),
				Arguments.of(BindingTypes.ATOMPUB, CmisAction.UPDATE, INPUT, updateActionExpectedBase64),
				Arguments.of(BindingTypes.ATOMPUB, CmisAction.FETCH, INPUT, FETCH_RESULT),

				Arguments.of(BindingTypes.WEBSERVICES, CmisAction.CREATE, INPUT, createActionExpectedBase64),
				Arguments.of(BindingTypes.WEBSERVICES, CmisAction.GET, INPUT, "dummy_stream"),
				Arguments.of(BindingTypes.WEBSERVICES, CmisAction.FIND, FIND_INPUT, FIND_RESULT),
				Arguments.of(BindingTypes.WEBSERVICES, CmisAction.UPDATE, INPUT, updateActionExpectedBase64),
				Arguments.of(BindingTypes.WEBSERVICES, CmisAction.FETCH, INPUT, FETCH_RESULT),

				Arguments.of(BindingTypes.BROWSER, CmisAction.CREATE, INPUT, createActionExpectedBase64),
				Arguments.of(BindingTypes.BROWSER, CmisAction.GET, INPUT, "dummy_stream"),
				Arguments.of(BindingTypes.BROWSER, CmisAction.FIND, FIND_INPUT, FIND_RESULT),
				Arguments.of(BindingTypes.BROWSER, CmisAction.UPDATE, INPUT, updateActionExpectedBase64),
				Arguments.of(BindingTypes.BROWSER, CmisAction.FETCH, INPUT, FETCH_RESULT)
		);
	}

	@Override
	public CmisSender createSender() throws Exception {
		CmisSender sender = super.createSender();
		sender.setFileSessionKey("fileContent");
		sender.setFilenameSessionKey("my-file");

		session.put("fileContent", "dummy data");
		HttpServletResponse response = mock(HttpServletResponse.class);
		session.put(PipeLineSession.HTTP_RESPONSE_KEY, response);

		return sender;
	}

	public void configure(BindingTypes bindingType, CmisAction action) throws Exception {
		sender.setBindingType(bindingType);
		sender.setAction(action);
		sender.configure();
		sender.open();
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("allImplementations")
	public void canConfigure(BindingTypes bindingType, CmisAction action, String input, String expectedResult) throws Exception {
		configure(bindingType, action);
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("allImplementations")
	public void sendMessage(BindingTypes bindingType, CmisAction action, String input, String expectedResult) throws Exception {
		if(action == CmisAction.GET) {
			sender.setFileSessionKey("");
			sender.setFilenameSessionKey("");
		}
		configure(bindingType, action);

		String actualResult = sender.sendMessageOrThrow(Message.asMessage(input), session).asString();
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("allImplementations")
	public void sendMessageWithContentStream(BindingTypes bindingType, CmisAction action, String input, String expectedResult) throws Exception {
		if(action != CmisAction.GET) return;
		configure(bindingType, action);

		assertTrue(Message.isEmpty(sender.sendMessageOrThrow(Message.asMessage(input), session)));
		Message message = (Message) session.get("fileContent");
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, message.asString());
	}
}
