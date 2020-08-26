package nl.nn.adapterframework.extensions.javascript;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collection;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import edu.emory.mathcs.backport.java.util.Arrays;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.senders.JavascriptSender;
import nl.nn.adapterframework.senders.SenderTestBase;
import nl.nn.adapterframework.stream.Message;

@RunWith(Parameterized.class)
public class JavascriptSenderTest extends SenderTestBase<JavascriptSender> {

	//Tests will be executed for the Rhino engine and the J2V8 engine
	@Parameterized.Parameter(0)
	public String engine;

	@Override
	public JavascriptSender createSender() {
		return new JavascriptSender();
	}

	@Parameterized.Parameters
	public static Collection<Object[]> data() {
		Object[][] data = new Object[][] {{"J2V8"}, {"Rhino"}};
		return Arrays.asList(data);
	}

	//Test without a given jsFunctionName. Will call the javascript function main as default
	@Test
	public void callMain() throws ConfigurationException, SenderException, TimeOutException, IOException {
		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/JavascriptTest.js"); 
		sender.setEngineName(engine);

		sender.configure();
		sender.open();

		assertEquals("0", sender.sendMessage(dummyInput,session).asString());
	}

	//Test without parameters, returns the result of a subtraction
	@Test
	public void noParameters() throws ConfigurationException, SenderException, TimeOutException, IOException {
		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/JavascriptTest.js"); 
		sender.setJsFunctionName("f1");
		sender.setEngineName(engine);

		sender.configure();
		sender.open();

		assertEquals("1", sender.sendMessage(dummyInput,session).asString());
	}

	/*Test with two given parameters. The integer values of the given parameters will be added and the result
	is given as the output of the pipe */
	@Test
	public void twoParameters() throws ConfigurationException, SenderException, TimeOutException, IOException {

		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/JavascriptTest.js"); 
		sender.setJsFunctionName("f2");
		sender.setEngineName(engine);

		Parameter param = new Parameter();
		param.setName("x");
		param.setType("integer");
		param.setValue("1");
		sender.addParameter(param);

		Parameter param2 = new Parameter();
		param2.setName("y");
		param2.setType("integer");
		param2.setValue("2");
		sender.addParameter(param2);

		sender.configure();
		sender.open();

		assertEquals("3", sender.sendMessage(dummyInput,session).asString());
	}

	/*Test with two parameters. The first parameter is the input of the pipe given using the originalMessage sessionKey. The input is expected to be
	 * an integer. The two parameters will be added and the result is given as the output of the pipe */
	@Test
	public void inputAsFirstParameter() throws ConfigurationException, SenderException, TimeOutException, IOException {

		String input = "10";
		Message message = new Message(input);
		sender.setJsFileName("Javascript/JavascriptTest.js"); 
		sender.setJsFunctionName("f2");
		sender.setEngineName(engine);

		session.put("originalMessage", input);

		Parameter param = new Parameter();
		param.setName("x");
		param.setType("integer");
		param.setSessionKey("originalMessage");
		sender.addParameter(param);

		Parameter param2 = new Parameter();
		param2.setName("y");
		param2.setType("integer");
		param2.setValue("2");
		sender.addParameter(param2);

		sender.configure();
		sender.open();

		assertEquals("12", sender.sendMessage(message,session).asString());
	}

	/* Test with two given parameters, the first parameter being the input of the pipe. Both parameters need to be of type String and the output of the pipe
	 * will be the result of concatenating the two parameter strings. */
	@Test
	public void concatenateString() throws ConfigurationException, SenderException, TimeOutException, IOException {

		Message input = new Message("Hello");
		sender.setJsFileName("Javascript/JavascriptTest.js"); 
		sender.setJsFunctionName("f2");
		sender.setEngineName(engine);

		session.put("originalMessage", input);

		Parameter param = new Parameter();
		param.setName("x");
		param.setSessionKey("originalMessage");
		sender.addParameter(param);

		Parameter param2 = new Parameter();
		param2.setName("y");
		param2.setValue(" World!");
		sender.addParameter(param2);

		sender.configure();
		sender.open();

		assertEquals("Hello World!", sender.sendMessage(input,session).asString());
	}

	/*Test with three given parameters. The integer values of the first two given parameters will be added and the result
	is given as the output of the pipe, if the value of the last parameter is set to true. If the value of the last parameter is 
	set to false, the function will return 0 */
	@Test
	public void threeParametersTrue() throws ConfigurationException, SenderException, TimeOutException, IOException {

		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/JavascriptTest.js"); 
		sender.setJsFunctionName("f3");
		sender.setEngineName(engine);

		Parameter param = new Parameter();
		param.setName("x");
		param.setType("integer");
		param.setValue("1");
		sender.addParameter(param);

		Parameter param2 = new Parameter();
		param2.setName("y");
		param2.setType("integer");
		param2.setValue("2");
		sender.addParameter(param2);

		Parameter param3 = new Parameter();
		param3.setName("z");
		param3.setType("boolean");
		param3.setValue("true");
		sender.addParameter(param3);

		sender.configure();
		sender.open();

		assertEquals("3", sender.sendMessage(dummyInput,session).asString());
	}

	/*Test with three given parameters. The integer values of the first two given parameters will be added and the result
	is given as the output of the pipe, if the value of the last parameter is set to true. If the value of the last parameter is 
	set to false, the function will return 0 */
	@Test
	public void threeParametersFalse() throws ConfigurationException, PipeRunException, PipeStartException, SenderException, TimeOutException, IOException {

		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/JavascriptTest.js"); 
		sender.setJsFunctionName("f3");
		sender.setEngineName(engine);

		Parameter param = new Parameter();
		param.setName("x");
		param.setType("integer");
		param.setValue("1");
		sender.addParameter(param);

		Parameter param2 = new Parameter();
		param2.setName("y");
		param2.setType("integer");
		param2.setValue("2");
		sender.addParameter(param2);

		Parameter param3 = new Parameter();
		param3.setName("z");
		param3.setType("boolean");
		param3.setValue("false");
		sender.addParameter(param3);

		sender.configure();
		sender.open();

		assertEquals("0", sender.sendMessage(dummyInput,session).asString());
	}

	//A ConfigurationException is given when a non existing file is given as FileName
	@Test
	public void invalidFileGivenException() throws ConfigurationException, SenderException, TimeOutException, IOException {
		exception.expectMessage("cannot find resource");
		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Nonexisting.js"); 
		sender.setJsFunctionName("f1");
		sender.setEngineName(engine);
		
		sender.configure();
		sender.open();
		
		assertEquals("1", sender.sendMessage(dummyInput,session).asString());
	}
	
	//A ConfigurationException is given when an empty string is given as FileName
	@Test
	public void emptyFileNameGivenException() throws ConfigurationException, SenderException, TimeOutException, IOException {
		exception.expectMessage("has neither fileName nor inputString specified");
		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName(""); 
		sender.setJsFunctionName("f1");
		sender.setEngineName(engine);
		
		sender.configure();
		sender.open();
		
		assertEquals("1", sender.sendMessage(dummyInput,session).asString());
	}
	
	//If the given FunctionName is not a function of the given javascript file a RuntimeException is given.
	@Test(expected = RuntimeException.class)
	public void invalidFunctionGivenException() throws ConfigurationException, SenderException, TimeOutException, IOException {
		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/JavascriptTest.js"); 
		sender.setJsFunctionName("nonexisting");
		sender.setEngineName(engine);
		
		sender.configure();
		sender.open();
		
		assertEquals("1", sender.sendMessage(dummyInput,session).asString());
	}
	
	//A ConfigurationException is given when an empty string is given as FunctionName
	@Test
	public void emptyFunctionGivenException() throws ConfigurationException, SenderException, TimeOutException, IOException {
		exception.expectMessage("JavaScript FunctionName not specified!");
		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/JavascriptTest.js"); 
		sender.setJsFunctionName("");
		sender.setEngineName(engine);
		
		sender.configure();
		sender.open();
		
		assertEquals("1", sender.sendMessage(dummyInput,session).asString());
	}
	
	//If there is a syntax error in the given Javascript file a RuntimeException is given.
	@Test(expected = RuntimeException.class)
	public void invalidJavascriptSyntax() throws ConfigurationException, SenderException, TimeOutException, IOException {
		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/IncorrectJavascript.js"); 
		sender.setEngineName(engine);
		
		sender.configure();
		sender.open();
		
		assertEquals("1", sender.sendMessage(dummyInput,session).asString());
	}
	
	/*This test uses a Javascript file which contains a function call to a function which does not exist. A RuntimeException
	is given if the used javascript function gives an error. */
	@Test(expected = RuntimeException.class)
	public void errorInJavascriptCode() throws ConfigurationException, SenderException, TimeOutException, IOException {
		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/IncorrectJavascript2.js"); 
		sender.setEngineName(engine);
		
		sender.configure();
		sender.open();
		
		assertEquals("1", sender.sendMessage(dummyInput,session).asString());
	}
	
	//The input is expected to be of type integer but an input of type Sting is given.
	//@Test(expected = SenderException.class)
	public void wrongInputAsFirstParameter() throws ConfigurationException, SenderException, TimeOutException, IOException {
		
		Message input = new Message("Stringinput");
		sender.setJsFileName("Javascript/JavascriptTest.js"); 
		sender.setJsFunctionName("f2");
		sender.setEngineName(engine);
		
		session.put("originalMessage", input);
		
		Parameter param = new Parameter();
		param.setName("x");
		param.setType("integer");
		param.setSessionKey("originalMessage");
		sender.addParameter(param);
		
		Parameter param2 = new Parameter();
		param2.setName("y");
		param2.setType("integer");
		param2.setValue("2");
		sender.addParameter(param2);
		
		sender.configure();
		sender.open();

		assertEquals("12", sender.sendMessage(input,session).asString());
	}
	
	//This test is used to compare the performance of J2V8 to that of Nashorn. J2V8 should finish about ten times faster than Nashorn.
	@Test
	@Ignore
	public void performance() throws ConfigurationException, SenderException, TimeOutException, IOException {
		
		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/JavascriptTest.js"); 
		sender.setJsFunctionName("performance");
		sender.setEngineName(engine);
		
		Parameter param = new Parameter();
		param.setName("x");
		param.setType("integer");
		param.setValue("100000");
		sender.addParameter(param);
		
		sender.configure();
		sender.open();

		System.out.println("Start timer");
		long startTime = System.nanoTime();

		assertEquals("1", sender.sendMessage(dummyInput,session).asString());
		long endTime = System.nanoTime();

		double duration = (double)(endTime - startTime)/1000000000; 
		System.out.println("Run time: " + duration + " seconds");
	}

}