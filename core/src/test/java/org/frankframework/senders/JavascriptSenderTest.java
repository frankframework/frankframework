package org.frankframework.senders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.util.stream.Stream;

import org.frankframework.testutil.TestAssertions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.SenderException;
import org.frankframework.parameters.Parameter;
import org.frankframework.senders.JavascriptSender.JavaScriptEngines;
import org.frankframework.stream.Message;
import org.frankframework.testutil.BooleanParameterBuilder;
import org.frankframework.testutil.NumberParameterBuilder;
import org.frankframework.testutil.ParameterBuilder;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JavascriptSenderTest extends SenderTestBase<JavascriptSender> {

	private Message dummyInput;

	@BeforeAll
	public static void setup() {
		// Since it's not possible to conditionally check the EnumSource here, just disable all for now...
		assumeFalse(TestAssertions.isTestRunningOnARM(), "uses J2V8 which does not work on ARM");
	}

	@Override
	public JavascriptSender createSender() {
		return new JavascriptSender();
	}

	@AfterEach
	void tearDownCloseMessage() throws Exception {
		super.tearDown();
		if (dummyInput != null) {
			dummyInput.close();
		}
	}

	// Test without a given jsFunctionName. Will call the javascript function main as default
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	void callMain(JavaScriptEngines engine) throws Exception {
		dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setEngineName(engine);

		sender.configure();
		sender.start();

		assertEquals("0", sender.sendMessageOrThrow(dummyInput, session).asString());
	}

	// Test without parameters, returns the result of a subtraction
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	void noParameters(JavaScriptEngines engine) throws Exception {
		dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName("f1");
		sender.setEngineName(engine);

		sender.configure();
		sender.start();

		assertEquals("1", sender.sendMessageOrThrow(dummyInput, session).asString());
	}

	// Test with two given parameters. The integer values of the given parameters will be added and the result
	// is given as the output of the pipe.
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	void twoParameters(JavaScriptEngines engine) throws Exception {

		dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName("f2");
		sender.setEngineName(engine);

		sender.addParameter(NumberParameterBuilder.create("x", 1));
		sender.addParameter(NumberParameterBuilder.create("y", 2));

		sender.configure();
		sender.start();

		assertEquals("3", sender.sendMessageOrThrow(dummyInput, session).asString());
	}

	// Test with two parameters. The first parameter is the input of the pipe given using the originalMessage sessionKey. The input is expected to be
	// an integer. The two parameters will be added and the result is given as the output of the pipe.
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	void inputAsFirstParameter(JavaScriptEngines engine) throws Exception {
		String input = "10";
		Message message = new Message(input);
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName("f2");
		sender.setEngineName(engine);

		session.put("originalMessage", input);

		sender.addParameter(NumberParameterBuilder.create("x").withSessionKey("originalMessage"));
		sender.addParameter(NumberParameterBuilder.create("y", 2));

		sender.configure();
		sender.start();

		assertEquals("12", sender.sendMessageOrThrow(message, session).asString());
	}

	/*
	 * Test with two given parameters, the first parameter being the input of the pipe. Both parameters need to be of type String and the output of the pipe
	 * will be the result of concatenating the two parameter strings.
	 */
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	void concatenateString(JavaScriptEngines engine) throws Exception {
		dummyInput = new Message("Hello");
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName("f2");
		sender.setEngineName(engine);

		session.put("originalMessage", dummyInput);

		sender.addParameter(ParameterBuilder.create().withName("x").withSessionKey("originalMessage"));
		sender.addParameter(new Parameter("y", " World!"));

		sender.configure();
		sender.start();

		assertEquals("Hello World!", sender.sendMessageOrThrow(dummyInput, session).asString());
	}

	/*
	 * Test with three given parameters. The integer values of the first two given parameters will be added and the result
	 * is given as the output of the pipe, if the value of the last parameter is set to true. If the value of the last parameter is
	 * set to false, the function will return 0.
	 */
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	void threeParametersTrue(JavaScriptEngines engine) throws Exception {
		dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName("f3");
		sender.setEngineName(engine);

		sender.addParameter(NumberParameterBuilder.create("x", 1));
		sender.addParameter(NumberParameterBuilder.create("y", 2));
		sender.addParameter(BooleanParameterBuilder.create("z", true));

		sender.configure();
		sender.start();

		assertEquals("3", sender.sendMessageOrThrow(dummyInput, session).asString());
	}

	/*
	 * Test with three given parameters. The integer values of the first two given parameters will be added and the result
	 * is given as the output of the pipe, if the value of the last parameter is set to true. If the value of the last parameter is
	 * set to false, the function will return 0
	 */
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	void threeParametersFalse(JavaScriptEngines engine) throws Exception {
		dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName("f3");
		sender.setEngineName(engine);

		sender.addParameter(NumberParameterBuilder.create("x", 1));
		sender.addParameter(NumberParameterBuilder.create("y", 2));
		sender.addParameter(BooleanParameterBuilder.create("z", false));

		sender.configure();
		sender.start();

		assertEquals("0", sender.sendMessageOrThrow(dummyInput, session).asString());
	}

	// A ConfigurationException is given when a non existing file is given as FileName
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	void invalidFileGivenException(JavaScriptEngines engine) throws Exception {
		sender.setJsFileName("Nonexisting.js");
		sender.setJsFunctionName("f1");
		sender.setEngineName(engine);

		ConfigurationException e = assertThrows(ConfigurationException.class, sender::configure);
		assertEquals("cannot find resource [Nonexisting.js]", e.getMessage());
	}

	// A ConfigurationException is given when an empty string is given as FileName
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	void emptyFileNameGivenException(JavaScriptEngines engine) throws Exception {
		sender.setJsFileName("");
		sender.setJsFunctionName("f1");
		sender.setEngineName(engine);

		ConfigurationException e = assertThrows(ConfigurationException.class, sender::configure);
		assertEquals("no jsFileName specified", e.getMessage());
	}

	// If the given FunctionName is not a function of the given javascript file a SenderException is thrown.
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	void invalidFunctionGivenException(JavaScriptEngines engine) throws Exception {
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName("nonexisting");
		sender.setEngineName(engine);

		sender.configure();
		sender.start();

		dummyInput = new Message("dummyinput");
		SenderException e = assertThrows(SenderException.class, () -> sender.sendMessageOrThrow(dummyInput, session));
		assertTrue(e.getMessage().startsWith("unable to execute script/function"));
	}

	// A ConfigurationException is given when an empty string is given as FunctionName
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	void emptyFunctionGivenException(JavaScriptEngines engine) throws Exception {
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName("");
		sender.setEngineName(engine);

		ConfigurationException e = assertThrows(ConfigurationException.class, sender::configure);
		assertEquals("JavaScript FunctionName not specified!", e.getMessage());
	}

	// If there is a syntax error in the given Javascript file a SenderException is thrown.
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	void invalidJavascriptSyntax(JavaScriptEngines engine) throws ConfigurationException, SenderException {
		dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/IncorrectJavascript.js");
		sender.setEngineName(engine);

		sender.configure();
		sender.start();

		assertThrows(SenderException.class, () -> sender.sendMessageOrThrow(dummyInput, session));
	}

	// This test uses a Javascript file which contains a function call to a function which does not exist. A SenderException
	// is thrown if the used javascript function gives an error.
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	void errorInJavascriptCode(JavaScriptEngines engine) throws ConfigurationException, SenderException {
		dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/IncorrectJavascript2.js");
		sender.setEngineName(engine);

		sender.configure();
		sender.start();

		assertThrows(SenderException.class, () -> sender.sendMessageOrThrow(dummyInput, session));
	}

	// The input is expected to be of type integer but an input of type Sting is given.
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	void wrongInputAsFirstParameter(JavaScriptEngines engine) throws ConfigurationException, SenderException {
		dummyInput = new Message("Stringinput");
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName("f2");
		sender.setEngineName(engine);

		session.put("originalMessage", dummyInput);

		sender.addParameter(NumberParameterBuilder.create("x").withSessionKey("originalMessage"));
		sender.addParameter(NumberParameterBuilder.create("y", 2));

		sender.configure();
		sender.start();

		assertThrows(SenderException.class, () -> sender.sendMessageOrThrow(dummyInput, session));
	}

	// Receive and return a string with a single quotes.
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	void stringWithSingleQuote(JavaScriptEngines engine) throws Exception {
		dummyInput = new Message("Stringinput");
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName("f5");
		sender.setEngineName(engine);

		session.put("originalMessage", dummyInput);

		sender.addParameter(ParameterBuilder.create("input", "{ \"key\": \"value met 'single' quotes\" }"));

		sender.configure();
		sender.start();

		sender.sendMessageOrThrow(dummyInput, session);
	}

	static Stream<Arguments> testWithJavaScriptReturningAnObject() {
		return Stream.of(
				Arguments.of(JavaScriptEngines.J2V8, "returnObject", "[object Object]"),
				Arguments.of(JavaScriptEngines.J2V8, "returnArray", "1,3,5"),
				Arguments.of(JavaScriptEngines.GRAALJS, "returnObject", "{answer: 42}"),
				Arguments.of(JavaScriptEngines.GRAALJS, "returnArray", "(3)[1, 3, 5]") // Different .toString() methods on PolyglotList
		);
	}

	@ParameterizedTest
	@MethodSource
	void testWithJavaScriptReturningAnObject(JavaScriptEngines engine, String functionName, String expectedOutput) throws Exception {
		// Arrange
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName(functionName);
		sender.setEngineName(engine);

		sender.addParameter(NumberParameterBuilder.create("x").withSessionKey("originalMessage"));
		sender.addParameter(NumberParameterBuilder.create("y", 2));

		sender.configure();
		sender.start();

		dummyInput = Message.nullMessage();

		// Act
		Message result = sender.sendMessageOrThrow(dummyInput, session);

		// Assert
		assertEquals(expectedOutput, result.asString());
	}

}
