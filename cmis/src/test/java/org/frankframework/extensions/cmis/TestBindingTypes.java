package org.frankframework.extensions.cmis;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.stream.Message;
import org.frankframework.testutil.TestAssertions;

public class TestBindingTypes extends CmisSenderTestBase {

	private static final String INPUT = """
			<cmis><id>id</id><objectId>dummy</objectId><objectTypeId>cmis:document</objectTypeId>\
			<fileName>fileInput.txt</fileName>\
			 <properties><property name="project:number" type="integer">123456789</property>\
			<property name="project:lastModified" type="datetime">2019-02-26T16:31:15</property>\
			<property name="project:onTime" type="boolean">true</property></properties></cmis>\
			""";
	private static final String FIND_INPUT = """
			<query><name>dummy</name>
				<objectId>dummy</objectId>
				<objectTypeId>dummy</objectTypeId>
				<maxItems>15</maxItems>
				<skipCount>0</skipCount>
				<searchAllVersions>true</searchAllVersions>
				<properties>
					<property name="cmis:name">dummy</property>
					<property name="project:number" type="integer">123456789</property>
					<property name="project:onTime" type="boolean">true</property>
					<property name="project:lastModified" type="datetime">2019-02-26T16:29:46</property>
				</properties>
				<statement>SELECT * from cmis:document</statement>
				<filter>cmis:objectId</filter>
				<includeAllowableActions>true</includeAllowableActions>
				<includePolicies>true</includePolicies>
				<includeAcl>true</includeAcl> </query>\
			""";
	private static final String FIND_RESULT = "<cmis totalNumItems=\"0\">  <rowset /></cmis>";
	private static final String FETCH_RESULT = """
			<cmis>  <properties>    \
			<property name="cmis:name" type="id">dummy</property>    \
			<property name="project:number" type="integer">123456789</property>    \
			<property name="project:lastModified" type="datetime">2019-02-26T16:31:15</property>    \
			<property name="project:onTime" type="boolean">true</property>  \
			</properties>  <allowableActions>    <action>canCreateDocument</action>  </allowableActions>  <isExactAcl>false</isExactAcl>  \
			<policyIds>    <policyId>dummyObjectId</policyId>  </policyIds>  \
			<relationships>    <relation>dummy</relation>  </relationships>\
			</cmis>\
			""";

	private static final String createActionExpectedBase64 = "ZmlsZUlucHV0LnR4dA==";
	private static final String updateActionExpectedBase64 = "aWQ=";

	public static Stream<Arguments> allImplementations() {
		return Stream.of(
				Arguments.of(CmisSessionBuilder.BindingTypes.ATOMPUB, CmisSender.CmisAction.CREATE, INPUT, createActionExpectedBase64),
				Arguments.of(CmisSessionBuilder.BindingTypes.ATOMPUB, CmisSender.CmisAction.GET, INPUT, "dummy_stream"),
				Arguments.of(CmisSessionBuilder.BindingTypes.ATOMPUB, CmisSender.CmisAction.FIND, FIND_INPUT, FIND_RESULT),
				Arguments.of(CmisSessionBuilder.BindingTypes.ATOMPUB, CmisSender.CmisAction.UPDATE, INPUT, updateActionExpectedBase64),
				Arguments.of(CmisSessionBuilder.BindingTypes.ATOMPUB, CmisSender.CmisAction.FETCH, INPUT, FETCH_RESULT),

				Arguments.of(CmisSessionBuilder.BindingTypes.WEBSERVICES, CmisSender.CmisAction.CREATE, INPUT, createActionExpectedBase64),
				Arguments.of(CmisSessionBuilder.BindingTypes.WEBSERVICES, CmisSender.CmisAction.GET, INPUT, "dummy_stream"),
				Arguments.of(CmisSessionBuilder.BindingTypes.WEBSERVICES, CmisSender.CmisAction.FIND, FIND_INPUT, FIND_RESULT),
				Arguments.of(CmisSessionBuilder.BindingTypes.WEBSERVICES, CmisSender.CmisAction.UPDATE, INPUT, updateActionExpectedBase64),
				Arguments.of(CmisSessionBuilder.BindingTypes.WEBSERVICES, CmisSender.CmisAction.FETCH, INPUT, FETCH_RESULT),

				Arguments.of(CmisSessionBuilder.BindingTypes.BROWSER, CmisSender.CmisAction.CREATE, INPUT, createActionExpectedBase64),
				Arguments.of(CmisSessionBuilder.BindingTypes.BROWSER, CmisSender.CmisAction.GET, INPUT, "dummy_stream"),
				Arguments.of(CmisSessionBuilder.BindingTypes.BROWSER, CmisSender.CmisAction.FIND, FIND_INPUT, FIND_RESULT),
				Arguments.of(CmisSessionBuilder.BindingTypes.BROWSER, CmisSender.CmisAction.UPDATE, INPUT, updateActionExpectedBase64),
				Arguments.of(CmisSessionBuilder.BindingTypes.BROWSER, CmisSender.CmisAction.FETCH, INPUT, FETCH_RESULT)
		);
	}

	@Override
	public CmisSender createSender() throws Exception {
		CmisSender sender = super.createSender();
		sender.setFileSessionKey("fileContent");
		sender.setFilenameSessionKey("my-file");

		session.put("fileContent", "dummy data");

		return sender;
	}

	public void configure(CmisSessionBuilder.BindingTypes bindingType, CmisSender.CmisAction action) {
		sender.setBindingType(bindingType);
		sender.setAction(action);
		assertDoesNotThrow(sender::configure);
		assertDoesNotThrow(sender::start);

	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("allImplementations")
	public void canConfigure(CmisSessionBuilder.BindingTypes bindingType, CmisSender.CmisAction action, String input, String expectedResult) {
		assertDoesNotThrow( () -> configure(bindingType, action) );
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("allImplementations")
	public void sendMessage(CmisSessionBuilder.BindingTypes bindingType, CmisSender.CmisAction action, String input, String expectedResult) throws Exception {
		if (action == CmisSender.CmisAction.GET) {
			sender.setFileSessionKey("");
			sender.setFilenameSessionKey("");
		}
		configure(bindingType, action);

		String actualResult = sender.sendMessageOrThrow(new Message(input), session).asString();
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("allImplementations")
	public void sendMessageWithContentStream(CmisSessionBuilder.BindingTypes bindingType, CmisSender.CmisAction action, String input, String expectedResult) throws Exception {
		if (action != CmisSender.CmisAction.GET) return;
		configure(bindingType, action);

		assertTrue(Message.isEmpty(sender.sendMessageOrThrow(new Message(input), session)));
		Message message = (Message) session.get("fileContent");
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, message.asString());
	}
}
