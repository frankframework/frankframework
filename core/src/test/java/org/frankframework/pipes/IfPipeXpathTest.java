package org.frankframework.pipes;

import static org.frankframework.pipes.IfPipeTest.PIPE_FORWARD_ELSE;
import static org.frankframework.pipes.IfPipeTest.PIPE_FORWARD_THEN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterType;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.testutil.XmlParameterBuilder;
import org.frankframework.util.CloseUtils;

public class IfPipeXpathTest extends PipeTestBase<IfPipe> {

	private PipeRunResult pipeRunResult;

	@Override
	@AfterEach
	public void tearDown() {
		CloseUtils.closeSilently(pipeRunResult);

		super.tearDown();
	}

	@Override
	public IfPipe createPipe() throws ConfigurationException {
		IfPipe ifPipe = new IfPipe();

		// Add default forwards
		ifPipe.addForward(new PipeForward(PIPE_FORWARD_THEN, null));
		ifPipe.addForward(new PipeForward(PIPE_FORWARD_ELSE, null));

		return ifPipe;
	}

	public static Stream<Arguments> messageSource() {
		return Stream.of(
				// input, expression, expressionValue, expectedValue
				Arguments.of("<root/>", "/root", "", PIPE_FORWARD_THEN),
				Arguments.of("<root>test</root>", "/root", "", PIPE_FORWARD_THEN),
				Arguments.of("<root/>", "/root", "test", PIPE_FORWARD_ELSE),
				Arguments.of("<root>test</root>", "/root", "test", PIPE_FORWARD_THEN),
				Arguments.of("<root>test</root>", "/root/text()", "test", PIPE_FORWARD_THEN),
				Arguments.of("<root>test123</root>", "/root", "test", PIPE_FORWARD_ELSE)
		);
	}

	@ParameterizedTest
	@MethodSource("messageSource")
	void testExpressions(String input, String expression, String expressionValue, String expectedValue) throws Exception {
		pipe.setXpathExpression(expression);
		pipe.setExpressionValue(expressionValue);
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, input, session);
		assertEquals(expectedValue, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void testXsltVersion3() throws Exception {
		pipe.setXsltVersion(3);
		// The '||' operator is used to concatenate the string representation of two values. This operator is new to XPath 3.0.
		pipe.setXpathExpression("/company/office/employee/first_name = ('Joh' || 'n')");
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, "<company><office><employee><first_name>John</first_name></employee></office></company>", session);
		assertEquals(PIPE_FORWARD_THEN, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void someXMLInputNotEqualToXPath() throws Exception {
		pipe.setXpathExpression("/test");
		pipe.setExpressionValue("");
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, "<root>test123</root>", session);
		assertEquals(PIPE_FORWARD_ELSE, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void xsltVersion1Success() throws Exception {
		pipe.setXpathExpression("number(count(/results/result[contains(@name , 'test')]))>1");
		pipe.setXsltVersion(1);
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, "<results><result name=\"test\"></result><result name=\"test\"></result></results>", session);
		assertEquals(PIPE_FORWARD_THEN, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void xsltVersion1Error() {
		assertThrows(ConfigurationException.class, () -> {
			pipe.setXpathExpression("number(count(/results/result[contains(@name , lower-case('test'))]))>1");
			pipe.setXsltVersion(1); //current default
			configureAndStartPipe();

			pipeRunResult = doPipe(pipe, "<results><result name=\"test\"></result><result name=\"test\"></result></results>", session);
			assertEquals(PIPE_FORWARD_THEN, pipeRunResult.getPipeForward().getName());
		});
	}

	@Test
	void xsltVersion2Success() throws Exception {
		pipe.setXpathExpression("number(count(/results/result[contains(@name , lower-case('test'))]))>1");
		pipe.setXsltVersion(2); //current default
		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, "<results><result name=\"test\"></result><result name=\"test\"></result></results>", session);
		assertEquals(PIPE_FORWARD_THEN, pipeRunResult.getPipeForward().getName());
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void testInvalidForwards(boolean testThenForward) {
		pipe.setXpathExpression("/root");
		if (testThenForward) {
			pipe.setThenForwardName("test");
		} else {
			pipe.setElseForwardName("test");
		}

		ConfigurationException e = assertThrows(ConfigurationException.class, this::configureAndStartPipe);
		assertThat(e.getMessage(), Matchers.containsString("has no forward with name [test]"));
	}

	@Test
	void emptyNamespaceDefsTest() {
		pipe.setXpathExpression("xs:boolean(count(/root/dummy) > 1)");

		ConfigurationException e = assertThrows(ConfigurationException.class, this::configureAndStartPipe);
		assertThat(e.getMessage(), Matchers.containsString("Namespace prefix 'xs' has not been declared"));
	}

	@Test
	void namespaceDefsTestTrue() throws Exception {
		String input = "<root><dummy>true</dummy><dummy>true</dummy></root>";

		pipe.setXpathExpression("xs:boolean(count(/root/dummy) > 1)");
		pipe.setNamespaceDefs("xs=http://www.w3.org/2001/XMLSchema");

		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, input, session);
		assertEquals(PIPE_FORWARD_THEN, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void namespaceDefsTestFalse() throws Exception {
		String input = "<root><dummy>true</dummy><dummy>true</dummy></root>";

		pipe.setXpathExpression("xs:boolean(count(/root/dummy) > 2)");
		pipe.setNamespaceDefs("xs=http://www.w3.org/2001/XMLSchema");

		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, input, session);
		assertEquals(PIPE_FORWARD_ELSE, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void namespaceDefsTestEmptyBooleanCheck() {
		pipe.setXpathExpression("xs:boolean()");
		pipe.setNamespaceDefs("xs=http://www.w3.org/2001/XMLSchema");

		ConfigurationException e = assertThrows(ConfigurationException.class, this::configureAndStartPipe);

		assertThat(e.getMessage(), Matchers.containsString("Cannot create TransformerPool for XPath expression"));
	}

	@Test
	void testWithParameter() throws Exception {
		pipe.addParameter(new Parameter("param", "value"));
		pipe.setXpathExpression("contains(/root/test, $param)");
		String input = "<root><test>value is present</test></root>";

		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, input, session);
		assertEquals(PIPE_FORWARD_THEN, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void testWithParameterEquals() throws Exception {
		XmlParameterBuilder parameter = XmlParameterBuilder.create("param", "<root><test>value</test></root>")
				.withType(ParameterType.DOMDOC);
		pipe.addParameter(parameter);
		pipe.setXpathExpression("$param/root/test='value'");

		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, "<dummy/>", session);
		assertEquals(PIPE_FORWARD_THEN, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void testWithMultipleParameters() throws Exception {
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
		assertEquals(PIPE_FORWARD_THEN, pipeRunResult.getPipeForward().getName());
	}

	@Test
	void testWithParameterThatGetsValueFromInput() throws Exception {
		XmlParameterBuilder parameter = XmlParameterBuilder.create("param", "<root><test>value</test></root>").withType(ParameterType.DOMDOC);
		pipe.addParameter(parameter);

		parameter = XmlParameterBuilder.create().withName("param2").withType(ParameterType.DOMDOC);
		parameter.setXpathExpression("/request/b");
		pipe.addParameter(parameter);

		pipe.setXpathExpression("/request/b=$param2");

		configureAndStartPipe();

		pipeRunResult = doPipe(pipe, TestFileUtils.getTestFileURL("/Xslt/AnyXml/in.xml").openStream(), session);
		assertEquals(PIPE_FORWARD_THEN, pipeRunResult.getPipeForward().getName());
	}
}
