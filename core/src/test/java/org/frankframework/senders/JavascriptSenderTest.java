package org.frankframework.senders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.stream.Stream;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterType;
import org.frankframework.senders.JavascriptSender.JavaScriptEngines;
import org.frankframework.stream.Message;
import org.frankframework.testutil.ParameterBuilder;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JavascriptSenderTest extends SenderTestBase<JavascriptSender> {

	@Override
	public JavascriptSender createSender() {
		return new JavascriptSender();
	}

	//Test without a given jsFunctionName. Will call the javascript function main as default
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	public void callMain(JavaScriptEngines engine) throws ConfigurationException, SenderException, TimeoutException, IOException {
		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setEngineName(engine);

		sender.configure();
		sender.open();

		assertEquals("0", sender.sendMessageOrThrow(dummyInput,session).asString());
	}

	//Test without parameters, returns the result of a subtraction
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	public void noParameters(JavaScriptEngines engine) throws ConfigurationException, SenderException, TimeoutException, IOException {
		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName("f1");
		sender.setEngineName(engine);

		sender.configure();
		sender.open();

		assertEquals("1", sender.sendMessageOrThrow(dummyInput,session).asString());
	}

	/*Test with two given parameters. The integer values of the given parameters will be added and the result
	is given as the output of the pipe */
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	public void twoParameters(JavaScriptEngines engine) throws ConfigurationException, SenderException, TimeoutException, IOException {

		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName("f2");
		sender.setEngineName(engine);

		sender.addParameter(ParameterBuilder.create("x", "1").withType(ParameterType.INTEGER));

		sender.addParameter(ParameterBuilder.create("y", "2").withType(ParameterType.INTEGER));

		sender.configure();
		sender.open();

		assertEquals("3", sender.sendMessageOrThrow(dummyInput,session).asString());
	}

	/*Test with two parameters. The first parameter is the input of the pipe given using the originalMessage sessionKey. The input is expected to be
	 * an integer. The two parameters will be added and the result is given as the output of the pipe */
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	public void inputAsFirstParameter(JavaScriptEngines engine) throws ConfigurationException, SenderException, TimeoutException, IOException {

		String input = "10";
		Message message = new Message(input);
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName("f2");
		sender.setEngineName(engine);

		session.put("originalMessage", input);

		sender.addParameter(ParameterBuilder.create().withName("x").withSessionKey("originalMessage").withType(ParameterType.INTEGER));
		sender.addParameter(ParameterBuilder.create("y", "2").withType(ParameterType.INTEGER));

		sender.configure();
		sender.open();

		assertEquals("12", sender.sendMessageOrThrow(message,session).asString());
	}

	/* Test with two given parameters, the first parameter being the input of the pipe. Both parameters need to be of type String and the output of the pipe
	 * will be the result of concatenating the two parameter strings. */
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	public void concatenateString(JavaScriptEngines engine) throws ConfigurationException, SenderException, TimeoutException, IOException {

		Message input = new Message("Hello");
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName("f2");
		sender.setEngineName(engine);

		session.put("originalMessage", input);

		sender.addParameter(ParameterBuilder.create().withName("x").withSessionKey("originalMessage"));
		sender.addParameter(new Parameter("y"," World!"));

		sender.configure();
		sender.open();

		assertEquals("Hello World!", sender.sendMessageOrThrow(input,session).asString());
	}

	/*Test with three given parameters. The integer values of the first two given parameters will be added and the result
	is given as the output of the pipe, if the value of the last parameter is set to true. If the value of the last parameter is
	set to false, the function will return 0 */
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	public void threeParametersTrue(JavaScriptEngines engine) throws ConfigurationException, SenderException, TimeoutException, IOException {

		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName("f3");
		sender.setEngineName(engine);

		sender.addParameter(ParameterBuilder.create("x", "1").withType(ParameterType.INTEGER));
		sender.addParameter(ParameterBuilder.create("y", "2").withType(ParameterType.INTEGER));
		sender.addParameter(ParameterBuilder.create("z", "true").withType(ParameterType.BOOLEAN));

		sender.configure();
		sender.open();

		assertEquals("3", sender.sendMessageOrThrow(dummyInput,session).asString());
	}

	/*Test with three given parameters. The integer values of the first two given parameters will be added and the result
	is given as the output of the pipe, if the value of the last parameter is set to true. If the value of the last parameter is
	set to false, the function will return 0 */
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	public void threeParametersFalse(JavaScriptEngines engine) throws Exception {

		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName("f3");
		sender.setEngineName(engine);

		sender.addParameter(ParameterBuilder.create("x", "1").withType(ParameterType.INTEGER));
		sender.addParameter(ParameterBuilder.create("y", "2").withType(ParameterType.INTEGER));
		sender.addParameter(ParameterBuilder.create("z", "false").withType(ParameterType.BOOLEAN));

		sender.configure();
		sender.open();

		assertEquals("0", sender.sendMessageOrThrow(dummyInput,session).asString());
	}

	//A ConfigurationException is given when a non existing file is given as FileName
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	public void invalidFileGivenException(JavaScriptEngines engine) throws  Exception {
		sender.setJsFileName("Nonexisting.js");
		sender.setJsFunctionName("f1");
		sender.setEngineName(engine);

		sender.configure();
		SenderException e = assertThrows(SenderException.class, sender::open);
		assertEquals("JavascriptSender cannot find resource [Nonexisting.js]", e.getMessage());
	}

	//A ConfigurationException is given when an empty string is given as FileName
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	public void emptyFileNameGivenException(JavaScriptEngines engine) throws Exception {
		sender.setJsFileName("");
		sender.setJsFunctionName("f1");
		sender.setEngineName(engine);

		sender.configure();
		SenderException e = assertThrows(SenderException.class, sender::open);
		assertEquals("JavascriptSender has neither fileName nor inputString specified", e.getMessage());
	}

	//If the given FunctionName is not a function of the given javascript file a SenderException is thrown.
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	public void invalidFunctionGivenException(JavaScriptEngines engine) throws Exception {
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName("nonexisting");
		sender.setEngineName(engine);

		sender.configure();
		sender.open();

		Message dummyInput = new Message("dummyinput");
		SenderException e = assertThrows(SenderException.class, ()->sender.sendMessageOrThrow(dummyInput, session));
		assertTrue(e.getMessage().startsWith("unable to execute script/function"));
	}

	//A ConfigurationException is given when an empty string is given as FunctionName
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	public void emptyFunctionGivenException(JavaScriptEngines engine) throws Exception {
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName("");
		sender.setEngineName(engine);

		sender.configure();
		SenderException e = assertThrows(SenderException.class, sender::open);
		assertEquals("JavascriptSender JavaScript FunctionName not specified!", e.getMessage());
	}

	//If there is a syntax error in the given Javascript file a SenderException is thrown.
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	public void invalidJavascriptSyntax(JavaScriptEngines engine) throws ConfigurationException, SenderException {
		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/IncorrectJavascript.js");
		sender.setEngineName(engine);

		sender.configure();
		sender.open();

		assertThrows(SenderException.class, () -> sender.sendMessageOrThrow(dummyInput, session));
	}

	// This test uses a Javascript file which contains a function call to a function which does not exist. A SenderException
	// is thrown if the used javascript function gives an error.
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	public void errorInJavascriptCode(JavaScriptEngines engine) throws ConfigurationException, SenderException, TimeoutException, IOException {
		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/IncorrectJavascript2.js");
		sender.setEngineName(engine);

		sender.configure();
		sender.open();

		assertThrows(SenderException.class, () -> sender.sendMessageOrThrow(dummyInput, session));
	}

	//The input is expected to be of type integer but an input of type Sting is given.
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	public void wrongInputAsFirstParameter(JavaScriptEngines engine) throws ConfigurationException, SenderException, TimeoutException, IOException {

		Message input = new Message("Stringinput");
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName("f2");
		sender.setEngineName(engine);

		session.put("originalMessage", input);

		sender.addParameter(ParameterBuilder.create().withName("x").withSessionKey("originalMessage").withType(ParameterType.INTEGER));
		sender.addParameter(ParameterBuilder.create("y", "2").withType(ParameterType.INTEGER));

		sender.configure();
		sender.open();

		assertThrows(SenderException.class, () -> sender.sendMessageOrThrow(input, session));
	}

	static Stream<Arguments> testWithJavaScriptReturningAnObject() {
		return Stream.of(
				Arguments.of(JavaScriptEngines.J2V8, "returnObject", "[object Object]"),
				Arguments.of(JavaScriptEngines.J2V8, "returnArray", "1,3,5")
		);
	}
	@ParameterizedTest
	@MethodSource
	public void testWithJavaScriptReturningAnObject(JavaScriptEngines engine, String functionName, String expectedOutput) throws Exception {
		// Arrange
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName(functionName);
		sender.setEngineName(engine);

		sender.addParameter(ParameterBuilder.create().withName("x").withSessionKey("originalMessage").withType(ParameterType.INTEGER));
		sender.addParameter(ParameterBuilder.create("y", "2").withType(ParameterType.INTEGER));

		sender.configure();
		sender.open();

		Message input = Message.nullMessage();

		// Act
		Message result = sender.sendMessageOrThrow(input, session);

		// Assert
		assertEquals(expectedOutput, result.asString());
	}
}
