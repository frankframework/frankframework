package nl.nn.adapterframework.extensions.cmis;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import nl.nn.adapterframework.extensions.cmis.CmisSender.CmisAction;
import nl.nn.adapterframework.extensions.cmis.CmisSessionBuilder.BindingTypes;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.UrlMessage;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.EnumUtils;
import nl.nn.adapterframework.util.StreamUtil;

@SuppressWarnings("deprecation")
@RunWith(Parameterized.class)
public class TestCreateAction extends CmisSenderTestBase {
	private static final AtomicInteger atomicInt = new AtomicInteger();

	private static final String EMPTY_INPUT = "";
	private static final String EMPTY_RESULT = "[unknown]";

	private static final String INPUT = "<cmis><objectId>random</objectId><objectTypeId>cmis:document</objectTypeId><fileName>${filename}</fileName>"
			+ "<properties><property name=\"cmis:description\" type=\"string\">123456789</property>"
			+ "<property name=\"cmis:lastModificationDate\" type=\"datetime\">2019-02-26T16:31:15</property>"
			+ "<property name=\"cmis:creationDate\" type=\"boolean\">true</property></properties></cmis>";

	private static final String FILE_INPUT = "/fileInput.txt";

	private String bindingType;
	private String action;
	private Message input;
	private String expectedResult;

	@Parameters(name = "{0} - {1} - {index}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{ "atompub", "create", EMPTY_INPUT },
				{ "atompub", "create", INPUT },

				{ "webservices", "create", EMPTY_INPUT },
				{ "webservices", "create", INPUT },

				{ "browser", "create", EMPTY_INPUT },
				{ "browser", "create", INPUT },
		});
	}

	public TestCreateAction(String bindingType, String action, String input) {
		if(EMPTY_INPUT.equals(input)) {
			assumeTrue(STUBBED); //Only test empty named objects when stubbed
			this.expectedResult = EMPTY_RESULT;
			this.input = new Message(EMPTY_INPUT);
		} else {
			String filename = "/fileInput-"+atomicInt.getAndIncrement()+".txt";
			this.input = new Message(input.replace("${filename}", filename));
			this.expectedResult = filename;
		}

		this.bindingType = bindingType;
		this.action = action;
	}

	private void configure() throws Exception {
		sender.setBindingType(EnumUtils.parse(BindingTypes.class, bindingType));
		sender.setAction(EnumUtils.parse(CmisAction.class, action));
		sender.configure();

		if(!STUBBED) {
			sender.open();
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

	@Test
	public void fileFromSessionKeyAsString() throws Exception {
		session.put("fileContent", getTestFile(false).asString());
		sender.setFileSessionKey("fileContent");
		configure();

		Message actualResult = sender.sendMessageOrThrow(input, session);
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, base64Decode(actualResult));
	}

	@Test
	public void fileFromSessionKeyAsStringParameter() throws Exception {
		session.put("fileContent", getTestFile(false).asString());
		Parameter fileSessionKey = new Parameter("fileSessionKey", "fileContent");
		sender.addParameter(fileSessionKey);
		configure();

		Message actualResult = sender.sendMessageOrThrow(input, session);
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, base64Decode(actualResult));
	}

	@Test
	public void fileFromSessionKeyAsByteArray() throws Exception {
		session.put("fileContent", getTestFile(false).asByteArray());
		sender.setFileSessionKey("fileContent");
		configure();

		Message actualResult = sender.sendMessageOrThrow(input, session);
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, base64Decode(actualResult));
	}

	@Test
	public void fileFromSessionKeyAsByteArrayParameter() throws Exception {
		session.put("fileContent", getTestFile(false).asByteArray());
		Parameter fileSessionKey = new Parameter("fileSessionKey", "fileContent");
		sender.addParameter(fileSessionKey);
		configure();

		Message actualResult = sender.sendMessageOrThrow(input, session);
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, base64Decode(actualResult));
	}

	@Test
	public void fileFromSessionKeyAsInputStream() throws Exception {
		URL testFile = TestFileUtils.getTestFileURL(FILE_INPUT);
		assertNotNull(testFile);

		session.put("fileContent", testFile.openStream());
		sender.setFileSessionKey("fileContent");
		configure();

		Message actualResult = sender.sendMessageOrThrow(input, session);
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, base64Decode(actualResult));
	}

	@Test
	public void fileFromSessionKeyAsInputStreamParameter() throws Exception {
		URL testFile = TestFileUtils.getTestFileURL(FILE_INPUT);
		assertNotNull(testFile);

		session.put("fileContent", testFile.openStream());
		Parameter fileSessionKey = new Parameter("fileSessionKey", "fileContent");
		sender.addParameter(fileSessionKey);
		configure();

		Message actualResult = sender.sendMessageOrThrow(input, session);
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, base64Decode(actualResult));
	}

	@Test //should base64 decode the fileContent string
	public void fileContentFromSessionKeyAsString() throws Exception {
		session.put("fileContent", getTestFile(true).asString());
		sender.setFileContentSessionKey("fileContent");
		configure();

		Message actualResult = sender.sendMessageOrThrow(input, session);
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, base64Decode(actualResult));
	}

	@Test
	public void fileContentFromSessionKeyAsByteArray() throws Exception {
		session.put("fileContent", getTestFile(false).asByteArray());
		sender.setFileContentSessionKey("fileContent");
		configure();

		Message actualResult = sender.sendMessageOrThrow(input, session);
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, base64Decode(actualResult));
	}

	@Test
	public void fileContentFromSessionKeyAsInputStream() throws Exception {
		URL testFile = TestFileUtils.getTestFileURL(FILE_INPUT);
		assertNotNull(testFile);

		session.put("fileContent", testFile.openStream());
		sender.setFileContentSessionKey("fileContent");
		configure();

		Message actualResult = sender.sendMessageOrThrow(input, session);
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, base64Decode(actualResult));
	}

	@Test
	public void fileStreamFromSessionKeyAsString() throws Exception {
		session.put("fis", getTestFile(false).asString());
		sender.setFileInputStreamSessionKey("fis");
		configure();

		Message actualResult = sender.sendMessageOrThrow(input, session);
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, base64Decode(actualResult));
	}

	@Test
	public void fileStreamFromSessionKeyAsByteArray() throws Exception {
		session.put("fis", getTestFile(false).asByteArray());
		sender.setFileInputStreamSessionKey("fis");
		configure();

		Message actualResult = sender.sendMessageOrThrow(input, session);
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, base64Decode(actualResult));
	}

	@Test
	public void fileStreamFromSessionKeyAsInputStream() throws Exception {
		URL testFile = TestFileUtils.getTestFileURL(FILE_INPUT);
		assertNotNull(testFile);

		session.put("fis", testFile.openStream());
		sender.setFileInputStreamSessionKey("fis");
		configure();

		Message actualResult = sender.sendMessageOrThrow(input, session);
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, base64Decode(actualResult));
	}

	@Test
	public void fileStreamFromSessionKeyWithIllegalType() throws Exception {
//		exception.expect(SenderException.class);
//		exception.expectMessage("expected InputStream, ByteArray or Base64-String but got");
		URL testFile = TestFileUtils.getTestFileURL(FILE_INPUT);
		assertNotNull(testFile);
		session.put("fis", new UrlMessage(testFile));
		sender.setFileInputStreamSessionKey("fis");
		configure();

		Message actualResult = sender.sendMessageOrThrow(input, session);
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, base64Decode(actualResult));
	}

}
