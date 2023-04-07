package nl.nn.adapterframework.extensions.javascript;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.Parameter.ParameterType;
import nl.nn.adapterframework.senders.JavascriptSender;
import nl.nn.adapterframework.senders.JavascriptSender.JavaScriptEngines;
import nl.nn.adapterframework.senders.SenderTestBase;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.ParameterBuilder;

@RunWith(Parameterized.class)
public class JavascriptSenderTest extends SenderTestBase<JavascriptSender> {

	@Parameterized.Parameter(0)
	public JavaScriptEngines engine;

	@Override
	public JavascriptSender createSender() {
		return new JavascriptSender();
	}

	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {{JavaScriptEngines.J2V8}, {JavaScriptEngines.NASHORN}, {JavaScriptEngines.RHINO}});
	}

	//Test without a given jsFunctionName. Will call the javascript function main as default
	@Test
	public void callMain() throws ConfigurationException, SenderException, TimeoutException, IOException {
		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setEngineName(engine);

		sender.configure();
		sender.open();

		assertEquals("0", sender.sendMessageOrThrow(dummyInput,session).asString());
	}

	//Test without parameters, returns the result of a subtraction
	@Test
	public void noParameters() throws ConfigurationException, SenderException, TimeoutException, IOException {
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
	@Test
	public void twoParameters() throws ConfigurationException, SenderException, TimeoutException, IOException {

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
	@Test
	public void inputAsFirstParameter() throws ConfigurationException, SenderException, TimeoutException, IOException {

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
	@Test
	public void concatenateString() throws ConfigurationException, SenderException, TimeoutException, IOException {

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
	@Test
	public void threeParametersTrue() throws ConfigurationException, SenderException, TimeoutException, IOException {

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
	@Test
	public void threeParametersFalse() throws Exception {

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
	@Test
	public void invalidFileGivenException() throws  Exception {
		sender.setJsFileName("Nonexisting.js");
		sender.setJsFunctionName("f1");
		sender.setEngineName(engine);

		sender.configure();
		SenderException e = assertThrows(SenderException.class, sender::open);
		assertEquals("JavascriptSender cannot find resource [Nonexisting.js]", e.getMessage());
	}

	//A ConfigurationException is given when an empty string is given as FileName
	@Test
	public void emptyFileNameGivenException() throws Exception {
		sender.setJsFileName("");
		sender.setJsFunctionName("f1");
		sender.setEngineName(engine);

		sender.configure();
		SenderException e = assertThrows(SenderException.class, sender::open);
		assertEquals("JavascriptSender has neither fileName nor inputString specified", e.getMessage());
	}

	//If the given FunctionName is not a function of the given javascript file a SenderException is thrown.
	@Test
	public void invalidFunctionGivenException() throws Exception {
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
	@Test
	public void emptyFunctionGivenException() throws Exception {
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName("");
		sender.setEngineName(engine);

		sender.configure();
		SenderException e = assertThrows(SenderException.class, sender::open);
		assertEquals("JavascriptSender JavaScript FunctionName not specified!", e.getMessage());
	}

	//If there is a syntax error in the given Javascript file a SenderException is thrown.
	@Test(expected = SenderException.class)
	public void invalidJavascriptSyntax() throws Exception {
		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/IncorrectJavascript.js");
		sender.setEngineName(engine);

		sender.configure();
		sender.open();

		assertEquals("1", sender.sendMessageOrThrow(dummyInput,session).asString());
	}

	// This test uses a Javascript file which contains a function call to a function which does not exist. A SenderException
	// is thrown if the used javascript function gives an error.
	@Test(expected = SenderException.class)
	public void errorInJavascriptCode() throws ConfigurationException, SenderException, TimeoutException, IOException {
		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/IncorrectJavascript2.js");
		sender.setEngineName(engine);

		sender.configure();
		sender.open();

		assertEquals("1", sender.sendMessageOrThrow(dummyInput,session).asString());
	}

	//The input is expected to be of type integer but an input of type Sting is given.
	//@Test(expected = SenderException.class)
	public void wrongInputAsFirstParameter() throws ConfigurationException, SenderException, TimeoutException, IOException {

		Message input = new Message("Stringinput");
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName("f2");
		sender.setEngineName(engine);

		session.put("originalMessage", input);

		sender.addParameter(ParameterBuilder.create().withName("x").withSessionKey("originalMessage").withType(ParameterType.INTEGER));
		sender.addParameter(ParameterBuilder.create("y", "2").withType(ParameterType.INTEGER));

		sender.configure();
		sender.open();

		assertEquals("12", sender.sendMessageOrThrow(input,session).asString());
	}

	//This test is used to compare the performance of J2V8 to that of Nashorn. J2V8 should finish about ten times faster than Nashorn.
	@Test
	@Ignore
	public void performance() throws ConfigurationException, SenderException, TimeoutException, IOException {

		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName("performance");
		sender.setEngineName(engine);

		sender.addParameter(ParameterBuilder.create("x", "100000").withType(ParameterType.INTEGER));

		sender.configure();
		sender.open();

		System.out.println("Start timer");
		long startTime = System.nanoTime();

		assertEquals("1", sender.sendMessageOrThrow(dummyInput,session).asString());
		long endTime = System.nanoTime();

		double duration = (double)(endTime - startTime)/1000000000;
		System.out.println("Run time: " + duration + " seconds");
	}
}