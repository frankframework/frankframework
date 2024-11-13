package org.frankframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterType;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.testutil.XmlParameterBuilder;
import org.frankframework.util.CloseUtils;

public class IfPipeTest extends PipeTestBase<IfPipe> {

	private final String pipeForwardThen = "then";
	private final String pipeForwardElse = "else";
	private PipeRunResult pipeRunResult;

	@Override
	@AfterEach
	public void tearDown() {
		CloseUtils.closeSilently(pipeRunResult);
	}

	@Override
	public IfPipe createPipe() throws ConfigurationException {
		IfPipe ifPipe = new IfPipe();

		//Add default pipes
		ifPipe.addForward(new PipeForward(pipeForwardThen, null));
		ifPipe.addForward(new PipeForward(pipeForwardElse, null));
		return ifPipe;
	}

	@Test
	void nullJsonPathExpressionTest() throws Exception {
		pipe.setJsonPathExpression(null);
		pipe.configure();
		pipe.start();

		Message message = new Message("{ test: '';}");
		message.getContext().withMimeType("application/json");

		pipeRunResult = doPipe(pipe, message, session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}


	@Test
	void nullXPathExpressionTest() throws Exception {
		pipe.setXpathExpression(null);
		pipe.configure();
		pipe.start();

		pipeRunResult = doPipe(pipe, "<test", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void expressionValueTest() throws Exception {
		pipe.setExpressionValue("test");
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, "<test", session);
		assertEquals(pipeForwardElse, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void invalidXPathExpressionTest() throws Exception {
		pipe.setXpathExpression("someexpression");
		configureAndStartPipe();

		PipeRunException e = assertThrows(PipeRunException.class, () -> doPipe(pipe, "test", session));
		assertThat(e.getMessage(), Matchers.containsString("cannot evaluate expression"));
	}

	@Test
	void emptyRegexTest() throws Exception {
		pipe.setRegex("");
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, "<test", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void someRegexTextTest() throws Exception {
		pipe.setRegex("some");
		pipe.configure();
		pipe.start();

		pipeRunResult = doPipe(pipe, "test", session);
		assertEquals(pipeForwardElse, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void testInputRegexTest() throws Exception {
		pipe.setRegex("(hoi)+");
		pipe.configure();
		pipe.start();

		String input = "hoihoihoi"; // Note that 'hoi a hoi' input is not a match!

		// Act & Assert 1: Test with matching regex
		pipeRunResult = doPipe(pipe, input, session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());

		// Act & Assert 2: Test with non-matching regex
		pipe.setRegex("(test3)+");
		pipeRunResult = doPipe(pipe, input, session);
		assertEquals(pipeForwardElse, pipeRunResult.getPipeForward().getName());
	}

	@Test
	@Disabled("Current regex implementation does not support multiline input. See #6963")
	void realWorldMultilineInputRegexTest() throws Exception {
		pipe.setRegex("(test1)+");
		pipe.configure();
		pipe.start();

		String input = """
				<directory>
					<file name="test1.txt"/>
					<file name="test2.txt"/>
				</directory>""";

		// Act & Assert 1: Test with matching regex
		pipeRunResult = doPipe(pipe, input, session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());

		// Act & Assert 2: Test with non-matching regex
		pipe.setRegex("(test3)+");
		pipeRunResult = doPipe(pipe, input, session);
		assertEquals(pipeForwardElse, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void emptyXPathExpressionTest() throws Exception {
		pipe.setXpathExpression("");
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, "<test", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void emptyXPathExpressionWithEmptyExpressionValueTest() throws Exception {
		pipe.setXpathExpression("");
		pipe.setExpressionValue("");
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, "<test", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void inputMatchesWithRegexTest() throws Exception {
		pipe.setRegex("test123");
		pipe.setExpressionValue("");
		configureAndStartPipe();
		pipeRunResult = doPipe(pipe, "test123", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void inputMatchesExpressionValueTest() throws Exception {
		pipe.setExpressionValue("test123");
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, "test123", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void invalidXPathExpressionValueTest() throws Exception {
		pipe.setXpathExpression("");
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, "test123", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void testWithInvalidThenPipe() throws Exception {
		String pipeName = "someText";
		pipe.setThenForwardName(pipeName);
		pipe.addForward(new PipeForward(pipeName, null));
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, "<test123", session);
		assertEquals(pipeName, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void testWithInvalidElsePipe() throws Exception {
		String pipeName = "someText";
		pipe.setElseForwardName(pipeName);
		pipe.addForward(new PipeForward(pipeName, null));
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, "<test123", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void someXMLInputEmptyExpressionValue() throws Exception {
		pipe.setXpathExpression("/root");
		pipe.setExpressionValue("");
		pipe.configure();
		pipe.start();

		pipeRunResult = doPipe(pipe, "<root>test</root>", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void someXMLInput() throws Exception {
		pipe.setXpathExpression("/root");
		pipe.setExpressionValue("test");
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, "<root>test</root>", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void someXMLInputNotEqualToExpressionValue() throws Exception {
		pipe.setXpathExpression("/root");
		pipe.setExpressionValue("test");
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, "<root>test123</root>", session);
		assertEquals(pipeForwardElse, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void someEmptyXMLInputTest() throws Exception {
		pipe.setXpathExpression("/root");
		pipe.setExpressionValue("");
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, "<root/>", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void testXsltVersion3() throws Exception {
		pipe.setXsltVersion(3);
		// The '||' operator is used to concatenate the string representation of two values. This operator is new to XPath 3.0.
		pipe.setXpathExpression("/company/office/employee/first_name = ('Joh' || 'n')");
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, "<company><office><employee><first_name>John</first_name></employee></office></company>", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void someXMLInputNotEqualtoXPath() throws Exception {
		pipe.setXpathExpression("/test");
		pipe.setExpressionValue("");
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, "<root>test123</root>", session);
		assertEquals(pipeForwardElse, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void xsltVersion1Success() throws Exception {
		pipe.setXpathExpression("number(count(/results/result[contains(@name , 'test')]))>1");
		pipe.setXsltVersion(1);
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, "<results><result name=\"test\"></result><result name=\"test\"></result></results>", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void xsltVersion1Error() {
		assertThrows(ConfigurationException.class, () -> {
			pipe.setXpathExpression("number(count(/results/result[contains(@name , lower-case('test'))]))>1");
			pipe.setXsltVersion(1); //current default
			configureAndStartPipe();

			pipeRunResult = doPipe(pipe, "<results><result name=\"test\"></result><result name=\"test\"></result></results>", session);
			assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
		});
	}

	@Test
	void xsltVersion2Success() throws Exception {
		pipe.setXpathExpression("number(count(/results/result[contains(@name , lower-case('test'))]))>1");
		pipe.setXsltVersion(2); //current default
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, "<results><result name=\"test\"></result><result name=\"test\"></result></results>", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void dummyNamedElsePipe() throws Exception {
		pipe.setXpathExpression("/test");
		pipe.setElseForwardName("test");
		configureAndStartPipe();

		PipeRunException e = assertThrows(PipeRunException.class, () -> doPipe(pipe, "<root>test123</root>", session));
		assertThat(e.getMessage(), Matchers.containsString("cannot find forward or pipe named [test]"));
	}

	@Test
	void dummyNamedThenPipe() throws Exception {
		pipe.setXpathExpression("/root");
		pipe.setThenForwardName("test");
		configureAndStartPipe();

		PipeRunException e = assertThrows(PipeRunException.class, () -> doPipe(pipe, "<root>test123</root>", session));
		assertThat(e.getMessage(), Matchers.containsString("cannot find forward or pipe named [test]"));
	}

	@Test
	void spaceInputOnValidThenPipeTest() throws Exception {
		pipe.setThenForwardName("then");
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, " <test1", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void spaceInputOnInvalidThenPipeTest() throws Exception {
		String pipeName = "test123";
		pipe.setThenForwardName(pipeName);
		configureAndStartPipe();

		PipeRunException e = assertThrows(PipeRunException.class, () -> doPipe(pipe, " <test1", session));
		assertThat(e.getMessage(), Matchers.endsWith("cannot find forward or pipe named [test123]"));
	}

	@Test
	void tabInputOnValidThenPipeTest() throws Exception {
		pipe.setThenForwardName("then");
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, "	<test1", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void tabInputOnInvalidThenPipeTest() throws Exception {
		String pipeName = "test1";
		pipe.setThenForwardName(pipeName);
		configureAndStartPipe();

		PipeRunException e = assertThrows(PipeRunException.class, () -> doPipe(pipe, "	<test1", session));
		assertThat(e.getMessage(), Matchers.endsWith("cannot find forward or pipe named [test1]"));
	}

	@Test
	void emptyNamespaceDefsTest() {
		String pipeName = "test1";
		pipe.setThenForwardName(pipeName);
		pipe.setXpathExpression("xs:boolean(count(/root/dummy) > 1)");

		ConfigurationException e = assertThrows(ConfigurationException.class, this::configureAndStartPipe);
		assertThat(e.getMessage(), Matchers.containsString("Namespace prefix 'xs' has not been declared"));
	}

	@Test
	void namespaceDefsTestTrue() throws Exception {
		String input = "<root><dummy>true</dummy><dummy>true</dummy></root>";
		String pipeName = "test1";
		pipe.setThenForwardName(pipeName);
		pipe.addForward(new PipeForward(pipeName, null));
		pipe.setXpathExpression("xs:boolean(count(/root/dummy) > 1)");
		pipe.setNamespaceDefs("xs=http://www.w3.org/2001/XMLSchema");
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, input, session);
		assertEquals(pipeName, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void namespaceDefsTestFalse() throws Exception {
		String input = "<root><dummy>true</dummy><dummy>true</dummy></root>";
		String pipeName = "test1";
		pipe.setElseForwardName(pipeName);
		pipe.addForward(new PipeForward(pipeName, null));
		pipe.setXpathExpression("xs:boolean(count(/root/dummy) > 2)");
		pipe.setNamespaceDefs("xs=http://www.w3.org/2001/XMLSchema");
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, input, session);
		assertEquals(pipeName, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void namespaceDefsTestEmptyBooleanCheck() {
		String pipeName = "test1";
		pipe.setElseForwardName(pipeName);
		pipe.addForward(new PipeForward(pipeName, null));
		pipe.setXpathExpression("xs:boolean()");
		pipe.setNamespaceDefs("xs=http://www.w3.org/2001/XMLSchema");

		ConfigurationException e = assertThrows(ConfigurationException.class, this::configureAndStartPipe);
		assertThat(e.getMessage(), Matchers.containsString("could not create transformer from xpathExpression"));
	}

	@Test
	void testNullInput() throws Exception {
		String pipeName = "test1";
		pipe.setElseForwardName(pipeName);
		pipe.addForward(new PipeForward(pipeName, null));
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, new Message((String) null), session);
		assertEquals(pipeName, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void testWithParameter() throws Exception {
		String elsePipeName = "test1";
		String thenPipeName = "test2";
		pipe.setElseForwardName(elsePipeName);
		pipe.setThenForwardName(thenPipeName);
		pipe.addForward(new PipeForward(elsePipeName, null));
		pipe.addForward(new PipeForward(thenPipeName, null));
		pipe.addParameter(new Parameter("param", "value"));
		pipe.setXpathExpression("contains(/root/test, $param)");
		String input = "<root><test>value is present</test></root>";

		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, input, session);
		assertEquals(thenPipeName, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void testWithParameterEquals() throws Exception {
		String elsePipeName = "test1";
		String thenPipeName = "test2";
		pipe.setElseForwardName(elsePipeName);
		pipe.setThenForwardName(thenPipeName);
		pipe.addForward(new PipeForward(elsePipeName, null));
		pipe.addForward(new PipeForward(thenPipeName, null));
		XmlParameterBuilder parameter = XmlParameterBuilder.create("param", "<root><test>value</test></root>")
				.withType(ParameterType.DOMDOC);
		pipe.addParameter(parameter);
		pipe.setXpathExpression("$param/root/test='value'");

		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, "<dummy/>", session);
		assertEquals(thenPipeName, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void testWithMultipleParameters() throws Exception {
		String elsePipeName = "test1";
		String thenPipeName = "test2";
		pipe.setElseForwardName(elsePipeName);
		pipe.setThenForwardName(thenPipeName);
		pipe.addForward(new PipeForward(elsePipeName, null));
		pipe.addForward(new PipeForward(thenPipeName, null));

		XmlParameterBuilder parameter1 = XmlParameterBuilder.create()
				.withName("param")
				.withValue("<root><test>value</test></root>")
				.withType(ParameterType.DOMDOC);
		pipe.addParameter(parameter1);

		XmlParameterBuilder parameter2 = XmlParameterBuilder.create()
				.withName("param2")
				.withValue("<root><test>value2</test></root>")
				.withType(ParameterType.DOMDOC);

		pipe.addParameter(parameter2);
		pipe.setXpathExpression("$param2/root/test='value2'");

		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, "<dummy/>", session);
		assertEquals(thenPipeName, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void testWithParameterThatGetsValueFromInput() throws Exception {
		String elsePipeName = "test1";
		String thenPipeName = "test2";
		pipe.setElseForwardName(elsePipeName);
		pipe.setThenForwardName(thenPipeName);
		pipe.addForward(new PipeForward(elsePipeName, null));
		pipe.addForward(new PipeForward(thenPipeName, null));
		XmlParameterBuilder parameter = XmlParameterBuilder.create("param", "<root><test>value</test></root>").withType(ParameterType.DOMDOC);
		pipe.addParameter(parameter);

		parameter = XmlParameterBuilder.create().withName("param2").withType(ParameterType.DOMDOC);
		parameter.setXpathExpression("/request/b");
		pipe.addParameter(parameter);

		pipe.setXpathExpression("/request/b=$param2");

		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, TestFileUtils.getTestFileURL("/Xslt/AnyXml/in.xml").openStream(), session);
		assertEquals(thenPipeName, pipeRunResult.getPipeForward().getName());
	}
}
