package org.frankframework.javascript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import org.frankframework.testutil.TestAssertions;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.frankframework.core.PipeLineSession;
import org.frankframework.senders.EchoSender;
import org.frankframework.senders.JavascriptSender;

class JavascriptEngineTest {

	@BeforeAll
	public static void setup() {
		assumeFalse(TestAssertions.isTestRunningOnARMMacOS());
	}

	@ParameterizedTest
	@EnumSource(JavascriptSender.JavaScriptEngines.class)
	void testBasicExecution(JavascriptSender.JavaScriptEngines engineWrapper) throws JavascriptException {
		JavascriptEngine engine = engineWrapper.create();
		engine.startRuntime();
		engine.executeScript("function test(){ return 'Hello World!' }");
		Object result = engine.executeFunction("test");
		assertEquals("Hello World!", result.toString());
		engine.closeRuntime();
	}

	@ParameterizedTest
	@EnumSource(JavascriptSender.JavaScriptEngines.class)
	void testJavascriptExecution(JavascriptSender.JavaScriptEngines engineWrapper) throws JavascriptException {
		JavascriptEngine engine = engineWrapper.create();
		engine.startRuntime();
		engine.executeScript("""
				function f5(x, y){
				  var a = x * y;
				  var b = (a);
				  return b;
				}""");
		Object result = engine.executeFunction("f5", 3, 5);
		assertEquals("15", result.toString());
		engine.closeRuntime();
	}

	@ParameterizedTest
	@EnumSource(JavascriptSender.JavaScriptEngines.class)
	void testJavascriptExecutionCallBack(JavascriptSender.JavaScriptEngines engineWrapper) throws JavascriptException {
		JavascriptEngine engine = engineWrapper.create();
		engine.startRuntime();
		EchoSender echoSender = new EchoSender();
		echoSender.setName("echoFunction");
		engine.registerCallback(echoSender, new PipeLineSession());
		engine.executeScript("""
				function f5(x, y){
				  var a = x * y;
				  var b = echoFunction(a);
				  return b;
				}""");
		Object result = engine.executeFunction("f5", 3, 5);
		assertEquals("15", result.toString());
		engine.closeRuntime();
	}

	@Test
	void testGraalJSReturnArray() throws JavascriptException {
		JavascriptEngine engine = new GraalJS();
		engine.startRuntime();
		engine.executeScript("""
				function f5() {
					return [1, 3, 5];
				}""");
		Object result = engine.executeFunction("f5");
		assertEquals("(3)[1, 3, 5]", result.toString());
		engine.closeRuntime();
	}

	@Test
	void testGraalJSONParseInput() throws JavascriptException {
		JavascriptEngine engine = new GraalJS();
		engine.startRuntime();
		engine.executeScript("""
				function returnObject() {
					return JSON.parse("{\\"answer\\": 42}");
				}""");
		Object result = engine.executeFunction("returnObject");
		assertEquals("{answer: 42}", result.toString());
		engine.closeRuntime();
	}

}
