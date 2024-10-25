package org.frankframework.extensions.cmis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.extensions.cmis.CmisSessionBuilder.BindingTypes;
import org.frankframework.parameters.Parameter;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestAssertions;

public class TestGetAction extends CmisSenderTestBase {

	private static final Message INPUT_WITH_PROPERTIES = new Message("""
			<cmis><id>id</id><objectId>dummy</objectId>\
			<objectTypeId>cmis:document</objectTypeId><fileName>fileInput.txt</fileName>\
			<properties><property name="cmis:description" type="string">123456789</property>\
			<property name="cmis:lastModificationDate" type="datetime">2019-02-26T16:31:15</property>\
			<property name="cmis:creationDate" type="boolean">true</property></properties></cmis>\
			""");

	private static final String GET_RESULT_FOR_INPUT = "dummy_stream";

	private static final String GET_RESULT_FOR_GET_PROPERTIES = """
			<cmis><properties>\
			<property name="cmis:name" type="id">dummy</property>\
			<property name="project:number" type="integer">123456789</property>\
			<property name="project:lastModified" type="datetime">2019-02-26T16:31:15</property>\
			<property name="project:onTime" type="boolean">true</property></properties></cmis>\
			""";

	public static Stream<Arguments> allImplementations() {
		return Stream.of(
				Arguments.of(BindingTypes.ATOMPUB, GET_RESULT_FOR_INPUT, false, false),
				Arguments.of(BindingTypes.ATOMPUB, GET_RESULT_FOR_GET_PROPERTIES, true, false),
				Arguments.of(BindingTypes.ATOMPUB, GET_RESULT_FOR_GET_PROPERTIES, true, true),

				Arguments.of(BindingTypes.WEBSERVICES, GET_RESULT_FOR_INPUT, false, false),
				Arguments.of(BindingTypes.WEBSERVICES, GET_RESULT_FOR_GET_PROPERTIES, true, false),
				Arguments.of(BindingTypes.WEBSERVICES, GET_RESULT_FOR_GET_PROPERTIES, true, true),

				Arguments.of(BindingTypes.BROWSER, GET_RESULT_FOR_INPUT, false, false),
				Arguments.of(BindingTypes.BROWSER, GET_RESULT_FOR_GET_PROPERTIES, true, false),
				Arguments.of(BindingTypes.BROWSER, GET_RESULT_FOR_GET_PROPERTIES, true, true)
		);
	}

	@ParameterizedTest(name = "{0} - {1} - getProperties = {2} - getDocumentContent = {3}")
	@MethodSource("allImplementations")
	@Nested
	@Retention(RetentionPolicy.RUNTIME)
	private @interface TestAllImplementations {
	}

	private void configure(BindingTypes bindingType, Boolean getProperties, Boolean getDocumentContent) throws Exception {
		sender.setGetProperties(getProperties);
		sender.setGetDocumentContent(getDocumentContent);

		sender.setBindingType(bindingType);
		sender.setAction(CmisSender.CmisAction.GET);
		sender.configure();

		if (!STUBBED) {
			sender.start();
		}
	}

	@TestAllImplementations
	public void sendMessageFileStream(BindingTypes bindingType, String expectedResult, Boolean getProperties, Boolean getDocumentContent) throws Exception {
		sender.setFileSessionKey("fis");
		configure(bindingType, getProperties, getDocumentContent);

		String actualResult = sender.sendMessageOrThrow(INPUT_WITH_PROPERTIES, session).asString();
		if (!getProperties) {
			assertNull(actualResult);
		} else {
			TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
		}

		Message stream = session.getMessage(sender.getFileSessionKey());
		if (!getProperties || getDocumentContent) {
			assertEquals(GET_RESULT_FOR_INPUT, stream.asString());
		} else {
			assertTrue(stream.isNull());
		}
	}

	@TestAllImplementations
	public void sendMessageStreamResult(BindingTypes bindingType, String expectedResult, Boolean getProperties, Boolean getDocumentContent) throws Exception {
		sender.setBindingType(bindingType);
		sender.setAction(CmisSender.CmisAction.GET);
		sender.configure();

		Message actualResult = sender.sendMessageOrThrow(INPUT_WITH_PROPERTIES, session);
		assertEquals(GET_RESULT_FOR_INPUT, actualResult.asString());
	}

	@TestAllImplementations
	public void sendMessageFileContentWithParameters(BindingTypes bindingType, String expectedResult, Boolean getProperties, Boolean getDocumentContent) throws Exception {
		sender.setFileSessionKey("fileContent");
		sender.addParameter(new Parameter("getProperties", getProperties.toString()));
		sender.addParameter(new Parameter("getDocumentContent", getDocumentContent.toString()));

		configure(bindingType, getProperties, getDocumentContent);

		String actualResult = sender.sendMessageOrThrow(INPUT_WITH_PROPERTIES, session).asString();
		if (!getProperties) {
			assertNull(actualResult);
		} else {
			TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
		}

		Message message = (Message) session.get(sender.getFileSessionKey());
		if(!getProperties || getDocumentContent) {
			assertEquals(GET_RESULT_FOR_INPUT, message.asString());
		} else {
			assertNull(message);
		}
	}
}
