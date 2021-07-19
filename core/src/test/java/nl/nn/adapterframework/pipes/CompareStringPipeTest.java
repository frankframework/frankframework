package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;

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

	@Test(expected = ConfigurationException.class)
	public void emptySessionKeys() throws ConfigurationException {
		pipe.setSessionKey1("");
		pipe.setSessionKey2("");
		pipe.configure();
	}

	@Test
	public void testLessThan() throws Exception {
		Parameter param1 = new Parameter();
		param1.setName("operand1");
		param1.setValue("a");
		pipe.addParameter(param1);

		Parameter param2 = new Parameter();
		param2.setName("operand2");
		param2.setValue("b");
		pipe.addParameter(param2);

		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, null, session);
		assertEquals(LESS_THAN, prr.getPipeForward().getName());
	}
	
	@Test
	public void testEquals() throws Exception {
		Parameter param1 = new Parameter();
		param1.setName("operand1");
		param1.setValue("a");
		pipe.addParameter(param1);

		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, "a", session);
		assertEquals(EQUALS, prr.getPipeForward().getName());
	}

	@Test
	public void testgreaterThan() throws Exception {
		Parameter param1 = new Parameter();
		param1.setName("operand2");
		param1.setValue("a");
		pipe.addParameter(param1);

		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, "b", session);
		assertEquals(GREATER_THAN, prr.getPipeForward().getName());
	}

	@Test
	public void textXmlCompareWithNewlines() throws Exception {
		Parameter operand1 = new Parameter();
		operand1.setName("operand1");
		operand1.setValue("<test>\n<a>9</a>\n<b>2</b>\n<c>7</c>\n</test>\n");
		pipe.addParameter(operand1);

		Parameter operand2 = new Parameter();
		operand2.setName("operand2");
		operand2.setValue("<test><a>9</a><b>2</b><c>7</c></test>");
		pipe.addParameter(operand2);

		pipe.setXml(true);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe("<ignored/>");
		assertEquals(EQUALS, prr.getPipeForward().getName());
	}

	@Test
	public void textXmlCompareWithAttributes() throws Exception {
		Parameter operand1 = new Parameter();
		operand1.setName("operand1");
		operand1.setValue("<test><a a=\"1\" b=\"2\">9</a><b>2</b><c>7</c></test>\n");
		pipe.addParameter(operand1);

		Parameter operand2 = new Parameter();
		operand2.setName("operand2");
		operand2.setValue("<test><a b=\"2\" a=\"1\">9</a><b>2</b><c>7</c></test>");
		pipe.addParameter(operand2);

		pipe.setXml(true);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe("<ignored/>");
		assertEquals(EQUALS, prr.getPipeForward().getName());
	}

	@Test
	public void textXmlCompareWithSpaces() throws Exception {
		Parameter operand1 = new Parameter();
		operand1.setName("operand1");
		operand1.setValue("<test><a>9</a><b>2</b><c>7</c>    </test>\n");
		pipe.addParameter(operand1);

		Parameter operand2 = new Parameter();
		operand2.setName("operand2");
		operand2.setValue("<test><a>9</a>    <b>2</b><c>7</c></test>");
		pipe.addParameter(operand2);

		pipe.setXml(true);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe("<ignored/>");
		assertEquals(EQUALS, prr.getPipeForward().getName());
	}

	@Test
	public void testIgnorePatterns() throws Exception {
		Parameter operand1 = new Parameter();
		operand1.setName("operand1");
		operand1.setValue("<test><a>tralalala</a><b>1</b><c>ignore me</c></test>");
		pipe.addParameter(operand1);

		Parameter operand2 = new Parameter();
		operand2.setName("operand2");
		operand2.setValue("<test><a>9</a><b>2</b><c>7</c></test>");
		pipe.addParameter(operand2);

		Parameter ignorePatterns = new Parameter();
		ignorePatterns.setName("ignorePatterns");
		ignorePatterns.setValue("<ignores><ignore><after>&lt;a&gt;</after><before>&lt;/a&gt;</before></ignore><ignore><after>&lt;c&gt;</after><before>&lt;/c&gt;</before></ignore></ignores>");
		pipe.addParameter(ignorePatterns);

		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe("<ignored/>");
		assertEquals(LESS_THAN, prr.getPipeForward().getName());
	}
}