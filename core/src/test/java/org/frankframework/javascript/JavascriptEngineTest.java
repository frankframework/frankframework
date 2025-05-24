package org.frankframework.javascript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.frankframework.core.PipeLineSession;
import org.frankframework.senders.EchoSender;
import org.frankframework.senders.JavascriptSender;
import org.frankframework.testutil.TestAppender;
import org.frankframework.testutil.TestAssertions;

class JavascriptEngineTest {

	@BeforeAll
	public static void setup() {
		// Since it's not possible to conditionally check the EnumSource here, just disable all for now...
		assumeFalse(TestAssertions.isTestRunningOnARM(), "uses J2V8 which does not work on ARM");
	}

	@ParameterizedTest
	@EnumSource(JavascriptSender.JavaScriptEngines.class)
	void testBasicExecution(JavascriptSender.JavaScriptEngines engineWrapper) throws JavascriptException {
		JavascriptEngine engine = engineWrapper.create();
		engine.startRuntime();
		engine.executeScript("function test() { return 'Hello World!' }");
		Object result = engine.executeFunction("test");
		assertEquals("Hello World!", result.toString());
		engine.closeRuntime();
	}

	// This test doesn't do much at the moment... We cannot capture the log output (yet).
	@Test
	void testLogStatement() throws JavascriptException {
		JavascriptEngine engine = new GraalJS();
		engine.startRuntime();
		engine.executeScript("""
				function logThings() {
					console.log("log");
					console.info("info");
					console.warn("warn");
					console.error("error");
				}
				""");

		try (TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout("%level - %m").build()) {
			Object result = engine.executeFunction("logThings");

			Value value = assertInstanceOf(Value.class, result);
			assertTrue(value.isNull());
//			assertTrue(appender.contains("DEBUG {} - log"));
//			assertTrue(appender.contains("INFO {} - info"));
//			assertTrue(appender.contains("WARN {} - warn"));
//			assertTrue(appender.contains("ERROR {} - error"));
		}
		engine.closeRuntime();
	}

	@ParameterizedTest
	@EnumSource(JavascriptSender.JavaScriptEngines.class)
	void testFunctionDoesNotExist(JavascriptSender.JavaScriptEngines engineWrapper) throws JavascriptException {
		JavascriptEngine engine = engineWrapper.create();
		engine.startRuntime();

		JavascriptException ex = assertThrows(JavascriptException.class, () -> engine.executeFunction("doesNotExist"));
		assertEquals("unable to find function [doesNotExist]", ex.getMessage());
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
