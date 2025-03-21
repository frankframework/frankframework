package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.Parameter;
import org.frankframework.stream.Message;
import org.frankframework.testutil.ParameterBuilder;

class CompareStringPipeTest extends PipeTestBase<CompareStringPipe> {

	private static final String GREATER_THAN = "greaterthan";
	private static final String LESS_THAN = "lessthan";
	private static final String EQUALS = "equals";

	@Override
	public CompareStringPipe createPipe() throws ConfigurationException {
		CompareStringPipe pipe = new CompareStringPipe();

		pipe.addForward(new PipeForward(LESS_THAN, null));
		pipe.addForward(new PipeForward(GREATER_THAN, null));
		pipe.addForward(new PipeForward(EQUALS, null));

		return pipe;
	}

	@Test
	void emptyOperandParameters() throws Exception {
		pipe.addParameter(new Parameter(CompareStringPipe.OPERAND1, ""));
		pipe.addParameter(new Parameter(CompareStringPipe.OPERAND2, ""));
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, "a", session);
		assertEquals(EQUALS, prr.getPipeForward().getName());
	}

	@Test
	void testNoOperandParametersShouldThrow() {
		assertThrows(ConfigurationException.class, () -> pipe.configure());
	}

	@Test
	void testLessThan() throws Exception {
		pipe.addParameter(new Parameter(CompareStringPipe.OPERAND1, "a"));
		pipe.addParameter(new Parameter(CompareStringPipe.OPERAND2, "b"));

		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, null, session);
		assertEquals(LESS_THAN, prr.getPipeForward().getName());
	}

	@Test
	void testEquals() throws Exception {
		pipe.addParameter(new Parameter(CompareStringPipe.OPERAND1, "a"));

		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, "a", session);
		assertEquals(EQUALS, prr.getPipeForward().getName());
	}

	@Test
	void testGreaterThan() throws Exception {
		pipe.addParameter(new Parameter(CompareStringPipe.OPERAND2, "a"));

		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, "b", session);
		assertEquals(GREATER_THAN, prr.getPipeForward().getName());
	}

	static Stream<Arguments> testXmlCompare() {
		return Stream.of(
				arguments("Should ignore newlines",
						"<test>\n<a>9</a><b>2</b><c>7</c>\n</test>\n",
						"<test>\n<a>9</a>\n<b>2</b>\n<c>7</c>\n</test>"),
				arguments("Should ignore different namespace prefixes",
						"<?xml version=\"1.0\" encoding=\"UTF-8\"?><ns1:Root xmlns:ns1=\"urn:pim\"><ns1:Employee/></ns1:Root>",
						"<?xml version=\"1.0\" encoding=\"UTF-8\"?><ns2:Root xmlns:ns2=\"urn:pim\"><ns2:Employee/></ns2:Root>"),
				arguments("Should ignore namespaces",
						"<?xml version=\"1.0\" encoding=\"UTF-8\"?><ns1:Root xmlns:ns1=\"urn:pim\"><ns1:Employee/></ns1:Root>",
						"<?xml version=\"1.0\" encoding=\"UTF-8\"?><Root><Employee/></Root>"),
				arguments("Should ignore XML declaration",
						"<Root><Employee/></Root>",
						"<?xml version=\"1.0\" encoding=\"UTF-8\"?><Root><Employee/></Root>"),
				arguments("Should ignore attribute order",
						"<test><a a=\"1\" b=\"2\">9</a><b>2</b><c>7</c></test>\n",
						"<test><a b=\"2\" a=\"1\">9</a><b>2</b><c>7</c></test>"),
				arguments("Should ignore spaces",
						"<test><a>9</a><b>2</b><c>7</c>    </test>\n",
						"<test><a>9</a>    <b>2</b><c>7</c></test>")
		);
	}
	@ParameterizedTest(name = "[{index}] {0}")
	@MethodSource
	void testXmlCompare(String description, String operand1, String operand2) throws Exception {
		pipe.addParameter(new Parameter(CompareStringPipe.OPERAND1, operand1));
		pipe.addParameter(new Parameter(CompareStringPipe.OPERAND2, operand2));

		pipe.setXml(true);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe("<ignored/>");
		assertEquals(EQUALS, prr.getPipeForward().getName(), description);
	}

	@Test
	void testIgnorePatterns() throws Exception {
		pipe.addParameter(new Parameter(CompareStringPipe.OPERAND1, "<test><a>tralalala</a><b>1</b><c>ignore me</c></test>"));
		pipe.addParameter(new Parameter(CompareStringPipe.OPERAND2, "<test><a>9</a><b>2</b><c>7</c></test>"));

		pipe.addParameter(new Parameter("ignorePatterns", "<ignores><ignore><after>&lt;a&gt;</after><before>&lt;/a&gt;</before></ignore><ignore><after>&lt;c&gt;</after><before>&lt;/c&gt;</before></ignore></ignores>"));

		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe("<ignored/>");
		assertEquals(LESS_THAN, prr.getPipeForward().getName());
	}

	@Test
	void testNullOperands() throws Exception {
		pipe.addParameter(ParameterBuilder.create().withName(CompareStringPipe.OPERAND1));
		pipe.addParameter(ParameterBuilder.create().withName(CompareStringPipe.OPERAND2));

		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(Message.nullMessage());
		assertEquals(EQUALS, prr.getPipeForward().getName());
	}

	@Test
	void testNullOperandIsLessThan() throws Exception {
		pipe.addParameter(ParameterBuilder.create().withName(CompareStringPipe.OPERAND1));
		pipe.addParameter(ParameterBuilder.create().withName(CompareStringPipe.OPERAND2).withValue("something"));

		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(Message.nullMessage());
		assertEquals(LESS_THAN, prr.getPipeForward().getName());
	}
}
