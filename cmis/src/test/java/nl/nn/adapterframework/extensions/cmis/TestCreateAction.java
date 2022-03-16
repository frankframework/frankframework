package nl.nn.adapterframework.extensions.cmis;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import nl.nn.adapterframework.parameters.Parameter;
import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestAssertions;

@RunWith(Parameterized.class)
public class TestCreateAction extends CmisSenderTestBase {

	private final static String EMPTY_INPUT = "";
	private final static String EMPTY_RESULT = "[unknown]";

	private final static String INPUT = "<cmis><objectId>dummy</objectId><objectTypeId>cmis:document</objectTypeId><fileName>fileInput.txt</fileName>"
			+ "<properties><property name=\"project:number\" type=\"integer\">123456789</property>"
			+ "<property name=\"project:lastModified\" type=\"datetime\">2019-02-26T16:31:15</property>"
			+ "<property name=\"project:onTime\" type=\"boolean\">true</property></properties></cmis>";

	private final static String RESULT = "fileInput.txt";

	@Parameters(name = "{0} - {1} - {2}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{ "atompub", "create", EMPTY_INPUT, EMPTY_RESULT },
				{ "atompub", "create", INPUT, RESULT },

				{ "webservices", "create", EMPTY_INPUT, EMPTY_RESULT },
				{ "webservices", "create", INPUT, RESULT },

				{ "browser", "create", EMPTY_INPUT, EMPTY_RESULT },
				{ "browser", "create", INPUT, RESULT },
		});
	}

	private String bindingType;
	private String action;
	private Message input;
	private String expectedResult;

	public TestCreateAction(String bindingType, String action, String input, String expected) {
		this.bindingType = bindingType;
		this.action = action;
		this.input = new Message(input);
		this.expectedResult = expected;
	}

	public void configure() throws ConfigurationException {
		sender.setBindingType(bindingType);
		sender.setAction(action);
		sender.configure();
	}

	@Test
	public void fileFromSessionKeyAsString() throws Exception {
		session.put("fileContent", new String(Base64.encodeBase64("some content here for test FileContent as String".getBytes())));
		sender.setFileSessionKey("fileContent");
		configure();
		String actualResult = sender.sendMessage(input, session).asString();
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
	}

	@Test
	public void fileFromSessionKeyAsStringParameter() throws Exception {
		session.put("fileContent", new String(Base64.encodeBase64("some content here for test FileContent as String".getBytes())));
		Parameter fileSessionKey = new Parameter("fileSessionKey", "fileContent");
		sender.addParameter(fileSessionKey);
		configure();
		String actualResult = sender.sendMessage(input, session).asString();
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
	}

	@Test
	public void fileFromSessionKeyAsByteArray() throws Exception {
		session.put("fileContent", "some content here for test fileContent as byte array".getBytes());
		sender.setFileSessionKey("fileContent");
		configure();
		String actualResult = sender.sendMessage(input, session).asString();
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
	}

	@Test
	public void fileFromSessionKeyAsByteArrayParameter() throws Exception {
		session.put("fileContent", "some content here for test fileContent as byte array".getBytes());
		Parameter fileSessionKey = new Parameter("fileSessionKey", "fileContent");
		sender.addParameter(fileSessionKey);
		configure();
		String actualResult = sender.sendMessage(input, session).asString();
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
	}

	@Test
	public void fileFromSessionKeyAsInputStream() throws Exception {
		session.put("fileContent", getClass().getResource("/fileInput.txt").openStream());
		sender.setFileSessionKey("fileContent");
		configure();
		String actualResult = sender.sendMessage(input, session).asString();
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
	}

	@Test
	public void fileFromSessionKeyAsInputStreamParameter() throws Exception {
		session.put("fileContent", getClass().getResource("/fileInput.txt").openStream());
		Parameter fileSessionKey = new Parameter("fileSessionKey", "fileContent");
		sender.addParameter(fileSessionKey);
		configure();
		String actualResult = sender.sendMessage(input, session).asString();
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
	}

	@Test
	public void fileContentFromSessionKeyAsString() throws Exception {
		session.put("fileContent", new String(Base64.encodeBase64("some content here for test FileContent as String".getBytes())));
		sender.setFileContentSessionKey("fileContent");
		configure();
		String actualResult = sender.sendMessage(input, session).asString();
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
	}

	@Test
	public void fileContentFromSessionKeyAsByteArray() throws Exception {
		session.put("fileContent", "some content here for test fileContent as byte array".getBytes());
		sender.setFileContentSessionKey("fileContent");
		configure();
		String actualResult = sender.sendMessage(input, session).asString();
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
	}

	@Test
	public void fileContentFromSessionKeyAsInputStream() throws Exception {
		session.put("fileContent", getClass().getResource("/fileInput.txt").openStream());
		sender.setFileContentSessionKey("fileContent");
		configure();
		String actualResult = sender.sendMessage(input, session).asString();
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
	}

	@Test
	public void fileStreamFromSessionKeyAsString() throws Exception {
		session.put("fis", new String(Base64.encodeBase64("some content here for test FileStream as String".getBytes())));
		sender.setFileInputStreamSessionKey("fis");
		configure();
		String actualResult = sender.sendMessage(input, session).asString();
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
	}

	@Test
	public void fileStreamFromSessionKeyAsByteArray() throws Exception {
		session.put("fis", "some content here for test FileStream as byte array".getBytes());
		sender.setFileInputStreamSessionKey("fis");
		configure();
		String actualResult = sender.sendMessage(input, session).asString();
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
	}

	@Test
	public void fileStreamFromSessionKeyAsInputStream() throws Exception {
		session.put("fis", getClass().getResource("/fileInput.txt").openStream());
		sender.setFileInputStreamSessionKey("fis");
		configure();
		String actualResult = sender.sendMessage(input, session).asString();
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
	}

	@Test
	public void fileStreamFromSessionKeyWithIllegalType() throws ConfigurationException, SenderException, TimeoutException, IOException {
//		exception.expect(SenderException.class);
//		exception.expectMessage("expected InputStream, ByteArray or Base64-String but got");
		session.put("fis", 1);
		sender.setFileInputStreamSessionKey("fis");
		configure();
		String actualResult = sender.sendMessage(input, session).asString();
		TestAssertions.assertEqualsIgnoreRNTSpace(expectedResult, actualResult);
	}

}
