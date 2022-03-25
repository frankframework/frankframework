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

import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.Misc;

@RunWith(Parameterized.class)
public class TestCreateAction extends CmisSenderTestBase {
	private static final AtomicInteger atomicInt = new AtomicInteger();
	private static final String ENDPOINT = "http://localhost:8080";

	private final static String EMPTY_INPUT = "";
	private final static String EMPTY_RESULT = "[unknown]";

	private final static String INPUT = "<cmis><objectId>random</objectId><objectTypeId>cmis:document</objectTypeId><fileName>${filename}</fileName>"
			+ "<properties><property name=\"cmis:description\" type=\"string\">123456789</property>"
			+ "<property name=\"cmis:lastModificationDate\" type=\"datetime\">2019-02-26T16:31:15</property>"
			+ "<property name=\"cmis:creationDate\" type=\"boolean\">true</property></properties></cmis>";

	private final static String FILE_INPUT = "/fileInput.txt";

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

	private String bindingType;
	private String action;
	private Message input;
	private String expectedResult;

	public TestCreateAction(String bindingType, String action, String input) {
		if(input == EMPTY_INPUT) {
			assumeTrue(STUBBED); //Only test empty named objects when stubbed
			this.expectedResult = EMPTY_RESULT;
		} else {
			String filename = "/fileInput-"+atomicInt.getAndIncrement()+".txt";
			input = input.replace("${filename}", filename);
			this.expectedResult = filename;
		}

		this.bindingType = bindingType;
		this.action = action;
		this.input = new Message(input);
	}

	private void configure() throws Exception {
		switch (bindingType) {
		case "atompub":
			sender.setUrl(ENDPOINT+"/atom11");
			break;
		case "webservices":
			sender.setUrl(ENDPOINT+"/services11/cmis");
			break;
		case "browser":
			sender.setUrl(ENDPOINT+"/browser");
			break;
		}
		sender.setBindingType(bindingType);
		sender.setAction(action);
		sender.configure();

		if(!STUBBED) {
			sender.open();
		}
	}

	private String base64Decode(Message message) throws IOException {
		return new String(Base64.decodeBase64(message.asByteArray()));
	}

	private String base64Encode(String message) throws IOException {
		return new String(Base64.encodeBase64(message.getBytes()));
	}

	@Test
	public void fileFromSessionKeyAsString() throws Exception {
		session.put("fileContent", base64Encode("some content here for test FileContent as String"));
		sender.setFileSessionKey("fileContent");
		configure();

		Message actualResult = sender.sendMessage(input, session);
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, base64Decode(actualResult));
	}

	@Test
	public void fileFromSessionKeyAsStringParameter() throws Exception {
		session.put("fileContent", base64Encode("some content here for test FileContent as String"));
		Parameter fileSessionKey = new Parameter("fileSessionKey", "fileContent");
		sender.addParameter(fileSessionKey);
		configure();

		Message actualResult = sender.sendMessage(input, session);
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, base64Decode(actualResult));
	}

	@Test
	public void fileFromSessionKeyAsByteArray() throws Exception {
		URL testFile = TestFileUtils.getTestFileURL(FILE_INPUT);
		assertNotNull(testFile);
		byte[] bytes = Misc.streamToBytes(testFile.openStream());

		session.put("fileContent", bytes);
		sender.setFileSessionKey("fileContent");
		configure();

		Message actualResult = sender.sendMessage(input, session);
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, base64Decode(actualResult));
	}

	@Test
	public void fileFromSessionKeyAsByteArrayParameter() throws Exception {
		URL testFile = TestFileUtils.getTestFileURL(FILE_INPUT);
		assertNotNull(testFile);
		byte[] bytes = Misc.streamToBytes(testFile.openStream());
		session.put("fileContent", bytes);
		Parameter fileSessionKey = new Parameter("fileSessionKey", "fileContent");
		sender.addParameter(fileSessionKey);
		configure();

		Message actualResult = sender.sendMessage(input, session);
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, base64Decode(actualResult));
	}

	@Test
	public void fileFromSessionKeyAsInputStream() throws Exception {
		URL testFile = TestFileUtils.getTestFileURL(FILE_INPUT);
		assertNotNull(testFile);

		session.put("fileContent", testFile.openStream());
		sender.setFileSessionKey("fileContent");
		configure();

		Message actualResult = sender.sendMessage(input, session);
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

		Message actualResult = sender.sendMessage(input, session);
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, base64Decode(actualResult));
	}

	@Test
	public void fileContentFromSessionKeyAsString() throws Exception {
		session.put("fileContent", base64Encode("some content here for test FileContent as String"));
		sender.setFileContentSessionKey("fileContent");
		configure();

		Message actualResult = sender.sendMessage(input, session);
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, base64Decode(actualResult));
	}

	@Test
	public void fileContentFromSessionKeyAsByteArray() throws Exception {
		session.put("fileContent", "some content here for test fileContent as byte array".getBytes());
		sender.setFileContentSessionKey("fileContent");
		configure();

		Message actualResult = sender.sendMessage(input, session);
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, base64Decode(actualResult));
	}

	@Test
	public void fileContentFromSessionKeyAsInputStream() throws Exception {
		URL testFile = TestFileUtils.getTestFileURL(FILE_INPUT);
		assertNotNull(testFile);

		session.put("fileContent", testFile.openStream());
		sender.setFileContentSessionKey("fileContent");
		configure();

		Message actualResult = sender.sendMessage(input, session);
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, base64Decode(actualResult));
	}

	@Test
	public void fileStreamFromSessionKeyAsString() throws Exception {
		session.put("fis", base64Encode("some content here for test FileContent as String"));
		sender.setFileInputStreamSessionKey("fis");
		configure();

		Message actualResult = sender.sendMessage(input, session);
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, base64Decode(actualResult));
	}

	@Test
	public void fileStreamFromSessionKeyAsByteArray() throws Exception {
		session.put("fis", "some content here for test FileStream as byte array".getBytes());
		sender.setFileInputStreamSessionKey("fis");
		configure();

		Message actualResult = sender.sendMessage(input, session);
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, base64Decode(actualResult));
	}

	@Test
	public void fileStreamFromSessionKeyAsInputStream() throws Exception {
		URL testFile = TestFileUtils.getTestFileURL(FILE_INPUT);
		assertNotNull(testFile);

		session.put("fis", testFile.openStream());
		sender.setFileInputStreamSessionKey("fis");
		configure();

		Message actualResult = sender.sendMessage(input, session);
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, base64Decode(actualResult));
	}

	@Test
	public void fileStreamFromSessionKeyWithIllegalType() throws Exception {
//		exception.expect(SenderException.class);
//		exception.expectMessage("expected InputStream, ByteArray or Base64-String but got");
		session.put("fis", 1);
		sender.setFileInputStreamSessionKey("fis");
		configure();

		Message actualResult = sender.sendMessage(input, session);
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, base64Decode(actualResult));
	}

}
