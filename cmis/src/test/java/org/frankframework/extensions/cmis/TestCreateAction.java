package org.frankframework.extensions.cmis;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.extensions.cmis.CmisSessionBuilder.BindingTypes;
import org.frankframework.parameters.Parameter;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestAssertions;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.StreamUtil;

public class TestCreateAction extends CmisSenderTestBase {
	private static final AtomicInteger atomicInt = new AtomicInteger();

	private static final String EMPTY_INPUT = "";
	private static final String EMPTY_RESULT = "[unknown]";

	private static final String INPUT = """
			<cmis><objectId>random</objectId><objectTypeId>cmis:document</objectTypeId><fileName>${filename}</fileName>\
			<properties><property name="cmis:description" type="string">123456789</property>\
			<property name="cmis:lastModificationDate" type="datetime">2019-02-26T16:31:15</property>\
			<property name="cmis:creationDate" type="boolean">true</property></properties></cmis>\
			""";

	private static final String FILE_INPUT = "/fileInput.txt";

	private Message input;
	private String expectedResult;

	public static Stream<Arguments> allImplementations() {
		return Stream.of(
				Arguments.of(BindingTypes.ATOMPUB, EMPTY_INPUT),
				Arguments.of(BindingTypes.ATOMPUB, INPUT),
				Arguments.of(BindingTypes.WEBSERVICES, EMPTY_INPUT),
				Arguments.of(BindingTypes.WEBSERVICES, INPUT),
				Arguments.of(BindingTypes.BROWSER, EMPTY_INPUT),
				Arguments.of(BindingTypes.BROWSER, INPUT));
	}

	@ParameterizedTest(name = "{0} - {1}")
	@MethodSource("allImplementations")
	@Nested
	@Retention(RetentionPolicy.RUNTIME)
	private @interface TestAllImplementations {
	}

	public void setup(String input) {
		if(EMPTY_INPUT.equals(input)) {
			assumeTrue(STUBBED); //Only test empty named objects when stubbed
			this.expectedResult = EMPTY_RESULT;
			this.input = new Message(EMPTY_INPUT);
		} else {
			String filename = "/fileInput-"+atomicInt.getAndIncrement()+".txt";
			this.input = new Message(input.replace("${filename}", filename));
			this.expectedResult = filename;
		}
	}

	private void configure(BindingTypes bindingType) throws Exception {
		sender.setBindingType(bindingType);
		sender.setAction(CmisSender.CmisAction.CREATE);
		sender.configure();

		if(!STUBBED) {
			sender.start();
		}
	}

	private Message getTestFile(boolean base64Encoded) throws Exception {
		URL testFile = TestFileUtils.getTestFileURL(FILE_INPUT);
		assertNotNull(testFile);
		byte[] bytes = StreamUtil.streamToBytes(testFile.openStream());

		return new Message(base64Encoded ? Base64.encodeBase64(bytes) : bytes);
	}

	private String base64Decode(Message message) throws IOException {
		return new String(Base64.decodeBase64(message.asByteArray()));
	}

	@TestAllImplementations
	public void fileFromSessionKeyAsString(BindingTypes bindingType, String inputString) throws Exception {
		setup(inputString);
		session.put("fileContent", getTestFile(false).asString());
		sender.setFileSessionKey("fileContent");
		configure(bindingType);

		Message actualResult = sender.sendMessageOrThrow(input, session);
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, base64Decode(actualResult));
	}

	@TestAllImplementations
	public void fileFromSessionKeyAsStringParameter(BindingTypes bindingType, String inputString) throws Exception {
		setup(inputString);
		session.put("fileContent", getTestFile(false).asString());
		Parameter fileSessionKey = new Parameter("fileSessionKey", "fileContent");
		sender.addParameter(fileSessionKey);
		configure(bindingType);

		Message actualResult = sender.sendMessageOrThrow(input, session);
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, base64Decode(actualResult));
	}


}
