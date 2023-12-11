package nl.nn.adapterframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.ParameterBuilder;

public class CompareStringPipeTest extends PipeTestBase<CompareStringPipe> {

	private static final String GREATER_THAN = "greaterthan";
	private static final String LESS_THAN = "lessthan";
	private static final String EQUALS = "equals";

	@Override
	public CompareStringPipe createPipe() throws ConfigurationException {
		CompareStringPipe pipe = new CompareStringPipe();

		pipe.registerForward(new PipeForward(LESS_THAN, null));
		pipe.registerForward(new PipeForward(GREATER_THAN, null));
		pipe.registerForward(new PipeForward(EQUALS, null));

		return pipe;
	}

	@Test
	public void emptySessionKeys() {
		pipe.setSessionKey1("");
		pipe.setSessionKey2("");

		assertThrows(ConfigurationException.class, pipe::configure);
	}

	@Test
	public void testLessThan() throws Exception {
		pipe.addParameter(new Parameter("operand1", "a"));
		pipe.addParameter(new Parameter("operand2", "b"));

		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, null, session);
		assertEquals(LESS_THAN, prr.getPipeForward().getName());
	}

	@Test
	public void testEquals() throws Exception {
		pipe.addParameter(new Parameter("operand1", "a"));

		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, "a", session);
		assertEquals(EQUALS, prr.getPipeForward().getName());
	}

	@Test
	public void testgreaterThan() throws Exception {
		pipe.addParameter(new Parameter("operand2", "a"));

		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, "b", session);
		assertEquals(GREATER_THAN, prr.getPipeForward().getName());
	}

	@Test
	public void textXmlCompareWithNewlines() throws Exception {
		pipe.addParameter(new Parameter("operand1", "<test>\n<a>9</a>\n<b>2</b>\n<c>7</c>\n</test>\n"));
		pipe.addParameter(new Parameter("operand2", "<test>\n<a>9</a>\n<b>2</b>\n<c>7</c>\n</test>\n"));

		pipe.setXml(true);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe("<ignored/>");
		assertEquals(EQUALS, prr.getPipeForward().getName());
	}

	@Test
	public void textXmlCompareWithAttributes() throws Exception {
		pipe.addParameter(new Parameter("operand1", "<test><a a=\"1\" b=\"2\">9</a><b>2</b><c>7</c></test>\n"));
		pipe.addParameter(new Parameter("operand2", "<test><a b=\"2\" a=\"1\">9</a><b>2</b><c>7</c></test>"));

		pipe.setXml(true);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe("<ignored/>");
		assertEquals(EQUALS, prr.getPipeForward().getName());
	}

	@Test
	public void textXmlCompareWithSpaces() throws Exception {
		pipe.addParameter(new Parameter("operand1", "<test><a>9</a><b>2</b><c>7</c>    </test>\n"));
		pipe.addParameter(new Parameter("operand2", "<test><a>9</a>    <b>2</b><c>7</c></test>"));

		pipe.setXml(true);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe("<ignored/>");
		assertEquals(EQUALS, prr.getPipeForward().getName());
	}

	@Test
	public void testIgnorePatterns() throws Exception {
		pipe.addParameter(new Parameter("operand1", "<test><a>tralalala</a><b>1</b><c>ignore me</c></test>"));
		pipe.addParameter(new Parameter("operand2", "<test><a>9</a><b>2</b><c>7</c></test>"));

		pipe.addParameter(new Parameter("ignorePatterns", "<ignores><ignore><after>&lt;a&gt;</after><before>&lt;/a&gt;</before></ignore><ignore><after>&lt;c&gt;</after><before>&lt;/c&gt;</before></ignore></ignores>"));

		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe("<ignored/>");
		assertEquals(LESS_THAN, prr.getPipeForward().getName());
	}

	@Test
	public void testNullOperands() throws Exception {
		pipe.addParameter(ParameterBuilder.create().withName("operand1"));
		pipe.addParameter(ParameterBuilder.create().withName("operand2"));

		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(Message.nullMessage());
		assertEquals(EQUALS, prr.getPipeForward().getName());
	}

	@Test
	public void testNullOperandIsLessThan() throws Exception {
		pipe.addParameter(ParameterBuilder.create().withName("operand1"));
		pipe.addParameter(ParameterBuilder.create().withName("operand2").withValue("something"));

		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(Message.nullMessage());
		assertEquals(LESS_THAN, prr.getPipeForward().getName());
	}
}
