package nl.nn.adapterframework.pipes;


import static org.junit.Assert.assertEquals;


import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.Parameter.ParameterType;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.ParameterBuilder;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class XmlIfTest extends PipeTestBase<XmlIf>{

	private String pipeForwardThen = "then";
	private String pipeForwardElse = "else";

	@Override
	public XmlIf createPipe() throws ConfigurationException {
		XmlIf xmlIf = new XmlIf();

		//Add default pipes
		xmlIf.registerForward(new PipeForward(pipeForwardThen, null));
		xmlIf.registerForward(new PipeForward(pipeForwardElse, null));
		return xmlIf;
	}

	@Test
	public void nullXPathExpressionTest() throws Exception{
		pipe.setXpathExpression(null);
		pipe.configure();
		pipe.start();

		PipeRunResult pipeRunResult = doPipe(pipe, "<test", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void emptySessionKeyTest() throws Exception {
		pipe.setSessionKey("");
		configureAndStartPipe();

		PipeRunResult pipeRunResult = doPipe(pipe, "<test123", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void someSessionKeyTest() throws Exception {
		pipe.setSessionKey("test");
		configureAndStartPipe();

		session.put("test", "testValue");
		session.put("test123", "testValue");

		PipeRunResult pipeRunResult = doPipe(pipe, "test123", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void expressionValueTest() throws Exception {
		pipe.setExpressionValue("test");
		configureAndStartPipe();

		PipeRunResult pipeRunResult = doPipe(pipe, "<test", session);
		assertEquals(pipeForwardElse, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void emptySessionKeyNullInputTest() throws Exception {
		pipe.setSessionKey("");
		configureAndStartPipe();
		
		PipeRunResult pipeRunResult = doPipe(pipe, null, session);
		assertEquals(pipeForwardElse, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void invalidXPathExpressionTest() throws Exception {
		exception.expect(PipeRunException.class);

		pipe.setXpathExpression("someexpression");
		configureAndStartPipe();
		
		PipeRunResult pipeRunResult = doPipe(pipe, "test", session);
		assertEquals(pipeForwardElse, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void emptyRegexTest() throws Exception {
		pipe.setRegex("");
		configureAndStartPipe();

		PipeRunResult pipeRunResult = doPipe(pipe, "<test", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void someRegexTextTest() throws Exception {
		pipe.setRegex("some");
		pipe.configure();
		pipe.start();

		PipeRunResult pipeRunResult = doPipe(pipe, "test", session);
		assertEquals(pipeForwardElse, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void emptyXPathExpressionTest() throws Exception {
		pipe.setXpathExpression("");
		configureAndStartPipe();
		
		PipeRunResult pipeRunResult = doPipe(pipe, "<test", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void emptyXPathExpressionWithEmptyExpressionValueTest() throws Exception {
		pipe.setXpathExpression("");
		pipe.setExpressionValue("");
		configureAndStartPipe();
		
		PipeRunResult pipeRunResult = doPipe(pipe, "<test", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void inputMatchesWithRegexTest() throws Exception {
		pipe.setRegex("test123");
		pipe.setExpressionValue("");
		configureAndStartPipe();
		PipeRunResult pipeRunResult = doPipe(pipe, "test123", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void inputMatchesExpressionValueTest() throws Exception {
		pipe.setExpressionValue("test123");
		configureAndStartPipe();

		PipeRunResult pipeRunResult = doPipe(pipe, "test123", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void invalidXPathExpressionValueTest() throws Exception {
		pipe.setXpathExpression("");
		configureAndStartPipe();

		PipeRunResult pipeRunResult = doPipe(pipe, "test123", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void testWithInvalidThenPipe() throws Exception {
		String pipeName = "someText";
		pipe.setThenForwardName(pipeName);
		pipe.registerForward(new PipeForward(pipeName,null));
		configureAndStartPipe();

		PipeRunResult pipeRunResult = doPipe(pipe, "<test123", session);
		assertEquals(pipeName, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void testWithInvalidElsePipe() throws Exception {
		String pipeName = "someText";
		pipe.setElseForwardName(pipeName);
		pipe.registerForward(new PipeForward(pipeName,null));
		configureAndStartPipe();

		PipeRunResult pipeRunResult = doPipe(pipe, "<test123", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void someXMLInputEmptyExpressionValue() throws Exception {
		pipe.setXpathExpression("/root");
		pipe.setExpressionValue("");
		pipe.configure();
		pipe.start();
		
		PipeRunResult pipeRunResult = doPipe(pipe, "<root>test</root>", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void someXMLInput() throws Exception {
		pipe.setXpathExpression("/root");
		pipe.setExpressionValue("test");
		configureAndStartPipe();
		
		PipeRunResult pipeRunResult = doPipe(pipe, "<root>test</root>", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void someXMLInputNotEqualtoExpressionValue() throws Exception {
		pipe.setXpathExpression("/root");
		pipe.setExpressionValue("test");
		configureAndStartPipe();
		
		PipeRunResult pipeRunResult = doPipe(pipe, "<root>test123</root>", session);
		assertEquals(pipeForwardElse, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void someEmptyXMLInputTest() throws Exception {
		pipe.setXpathExpression("/root");
		pipe.setExpressionValue("");
		configureAndStartPipe();
		
		PipeRunResult pipeRunResult = doPipe(pipe, "<root/>", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void testXsltVersion3() throws Exception {
		pipe.setXsltVersion(3);
		// The '||' operator is used to concatenate the string representation of two values. This operator is new to XPath 3.0.
		pipe.setXpathExpression("/company/office/employee/first_name = ('Joh' || 'n')");
		configureAndStartPipe();
		
		PipeRunResult pipeRunResult = doPipe(pipe, "<company><office><employee><first_name>John</first_name></employee></office></company>", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void someXMLInputNotEqualtoXPath() throws Exception {
		pipe.setXpathExpression("/test");
		pipe.setExpressionValue("");
		configureAndStartPipe();
		
		PipeRunResult pipeRunResult = doPipe(pipe, "<root>test123</root>", session);
		assertEquals(pipeForwardElse, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void xsltVersion1Success() throws Exception {
		pipe.setXpathExpression("number(count(/results/result[contains(@name , 'test')]))>1");
		pipe.setXsltVersion(1);
		configureAndStartPipe();

		PipeRunResult pipeRunResult = doPipe(pipe, "<results><result name=\"test\"></result><result name=\"test\"></result></results>", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test(expected = ConfigurationException.class)
	public void xsltVersion1Error() throws Exception {
		pipe.setXpathExpression("number(count(/results/result[contains(@name , lower-case('test'))]))>1");
		pipe.setXsltVersion(1); //current default
		configureAndStartPipe();

		PipeRunResult pipeRunResult = doPipe(pipe, "<results><result name=\"test\"></result><result name=\"test\"></result></results>", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void xsltVersion2Success() throws Exception {
		pipe.setXpathExpression("number(count(/results/result[contains(@name , lower-case('test'))]))>1");
		pipe.setXsltVersion(2); //current default
		configureAndStartPipe();

		PipeRunResult pipeRunResult = doPipe(pipe, "<results><result name=\"test\"></result><result name=\"test\"></result></results>", session);
		assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void dummyNamedElsePipe() throws Exception {
		exception.expect(PipeRunException.class);
		pipe.setXpathExpression("/test");
		pipe.setElseForwardName("test");
		configureAndStartPipe();
		doPipe(pipe, "<root>test123</root>", session);
	}

	@Test
	public void dummyNamedThenPipe() throws Exception {
		exception.expect(PipeRunException.class);
		pipe.setXpathExpression("/root");
		pipe.setThenForwardName("test");
		configureAndStartPipe();
		doPipe(pipe, "<root>test123</root>", session);
	}

	@Test
	public void spaceInputOnValidThenPipeTest() throws Exception {
		pipe.setThenForwardName("then");
		configureAndStartPipe();

		PipeRunResult prr = doPipe(pipe, " <test1", session);
		assertEquals(pipeForwardThen, prr.getPipeForward().getName());
	}

	@Test
	public void spaceInputOnInvalidThenPipeTest() throws Exception {
		exception.expect(PipeRunException.class);

		String pipeName = "test123";
		pipe.setThenForwardName(pipeName);
		configureAndStartPipe();

		PipeRunResult prr = doPipe(pipe, " <test1", session);
		assertEquals(pipeName, prr.getPipeForward().getName());
	}

	@Test
	public void tabInputOnValidThenPipeTest() throws Exception {
		pipe.setThenForwardName("then");
		configureAndStartPipe();

		PipeRunResult prr = doPipe(pipe, "	<test1", session);
		assertEquals(pipeForwardThen, prr.getPipeForward().getName());
	}

	@Test
	public void tabInputOnInvalidThenPipeTest() throws Exception {
		exception.expect(PipeRunException.class);

		String pipeName = "test1";
		pipe.setThenForwardName(pipeName);
		configureAndStartPipe();

		PipeRunResult prr = doPipe(pipe, "	<test1", session);
		assertEquals(pipeName, prr.getPipeForward().getName());
	}

	@Test
	public void emptyNamespaceDefsTest() throws Exception {
		exception.expectMessage("Namespace prefix 'xs' has not been declared");
		String input = "&lt;root&gt;\n" + 
				"&lt;dummy&gt;true&lt;/dummy&gt;\n" + 
				"&lt;dummy&gt;true&lt;/dummy&gt;\n" + 
				"&lt;/root&gt;";
		String pipeName = "test1";
		pipe.setThenForwardName(pipeName);
		pipe.setXpathExpression("xs:boolean(count(/root/dummy) > 1)");
		configureAndStartPipe();

		PipeRunResult prr = doPipe(pipe, input, session);
		assertEquals(pipeName, prr.getPipeForward().getName());
	}

	@Test
	public void namespaceDefsTestTrue() throws Exception {
		String input = "<root><dummy>true</dummy><dummy>true</dummy></root>";
		String pipeName = "test1";
		pipe.setThenForwardName(pipeName);
		pipe.registerForward(new PipeForward(pipeName,null));
		pipe.setXpathExpression("xs:boolean(count(/root/dummy) > 1)");
		pipe.setNamespaceDefs("xs=http://www.w3.org/2001/XMLSchema");
		configureAndStartPipe();

		PipeRunResult prr = doPipe(pipe, input, session);
		assertEquals(pipeName, prr.getPipeForward().getName());
	}

	@Test
	public void namespaceDefsTestFalse() throws Exception {
		String input = "<root><dummy>true</dummy><dummy>true</dummy></root>";
		String pipeName = "test1";
		pipe.setElseForwardName(pipeName);
		pipe.registerForward(new PipeForward(pipeName,null));
		pipe.setXpathExpression("xs:boolean(count(/root/dummy) > 2)");
		pipe.setNamespaceDefs("xs=http://www.w3.org/2001/XMLSchema");
		configureAndStartPipe();

		PipeRunResult prr = doPipe(pipe, input, session);
		assertEquals(pipeName, prr.getPipeForward().getName());
	}

	@Test
	public void namespaceDefsTestEmptyBooleanCheck() throws Exception {
		exception.expect(ConfigurationException.class);
		String input = "<root><dummy>true</dummy><dummy>true</dummy></root>";
		String pipeName = "test1";
		pipe.setElseForwardName(pipeName);
		pipe.registerForward(new PipeForward(pipeName,null));
		pipe.setXpathExpression("xs:boolean()");
		pipe.setNamespaceDefs("xs=http://www.w3.org/2001/XMLSchema");
		configureAndStartPipe();

		PipeRunResult prr = doPipe(pipe, input, session);
		assertEquals(pipeName, prr.getPipeForward().getName());
	}

	@Test
	public void testNullInput() throws Exception {
		String pipeName = "test1";
		pipe.setElseForwardName(pipeName);
		pipe.registerForward(new PipeForward(pipeName,null));
		configureAndStartPipe();

		PipeRunResult prr = doPipe(pipe, new Message((String)null), session);
		assertEquals(pipeName, prr.getPipeForward().getName());
	}

	@Test
	public void testWithParameter() throws Exception {
		String elsePipeName = "test1";
		String thenPipeName = "test2";
		pipe.setElseForwardName(elsePipeName);
		pipe.setThenForwardName(thenPipeName);
		pipe.registerForward(new PipeForward(elsePipeName, null));
		pipe.registerForward(new PipeForward(thenPipeName, null));
		pipe.addParameter(new Parameter("param", "value"));
		pipe.setXpathExpression("contains(/root/test, $param)");
		String input = "<root><test>value is present</test></root>";

		configureAndStartPipe();

		PipeRunResult prr = doPipe(pipe, input, session);
		assertEquals(thenPipeName, prr.getPipeForward().getName());
	}

	@Test
	public void testWithParameterEquals() throws Exception {
		String elsePipeName = "test1";
		String thenPipeName = "test2";
		pipe.setElseForwardName(elsePipeName);
		pipe.setThenForwardName(thenPipeName);
		pipe.registerForward(new PipeForward(elsePipeName, null));
		pipe.registerForward(new PipeForward(thenPipeName, null));
		Parameter p = new Parameter("param", "<root><test>value</test></root>");
		p.setType(ParameterType.DOMDOC);
		pipe.addParameter(p);
		pipe.setXpathExpression("$param/root/test='value'");

		configureAndStartPipe();

		PipeRunResult prr = doPipe(pipe, "<dummy/>", session);
		assertEquals(thenPipeName, prr.getPipeForward().getName());
	}

	@Test
	public void testWithMultipleParameters() throws Exception {
		String elsePipeName = "test1";
		String thenPipeName = "test2";
		pipe.setElseForwardName(elsePipeName);
		pipe.setThenForwardName(thenPipeName);
		pipe.registerForward(new PipeForward(elsePipeName, null));
		pipe.registerForward(new PipeForward(thenPipeName, null));
		Parameter p = new Parameter("param", "<root><test>value</test></root>");
		p.setType(ParameterType.DOMDOC);
		pipe.addParameter(p);

		p = new Parameter("param2", "<root><test>value2</test></root>");
		p.setType(ParameterType.DOMDOC);
		pipe.addParameter(p);
		pipe.setXpathExpression("$param2/root/test='value2'");

		configureAndStartPipe();

		PipeRunResult prr = doPipe(pipe, "<dummy/>", session);
		assertEquals(thenPipeName, prr.getPipeForward().getName());
	}

	@Test
	public void testWithParameterThatGetsValueFromInput() throws Exception {
		String elsePipeName = "test1";
		String thenPipeName = "test2";
		pipe.setElseForwardName(elsePipeName);
		pipe.setThenForwardName(thenPipeName);
		pipe.registerForward(new PipeForward(elsePipeName, null));
		pipe.registerForward(new PipeForward(thenPipeName, null));
		Parameter p = ParameterBuilder.create("param", "<root><test>value</test></root>").withType(ParameterType.DOMDOC);
		pipe.addParameter(p);

		p = ParameterBuilder.create().withName("param2").withType(ParameterType.DOMDOC);
		p.setXpathExpression("/request/b");
		pipe.addParameter(p);

		pipe.setXpathExpression("/request/b=$param2");

		configureAndStartPipe();

		PipeRunResult prr = doPipe(pipe, TestFileUtils.getTestFileURL("/Xslt/AnyXml/in.xml").openStream(), session);
		assertEquals(thenPipeName, prr.getPipeForward().getName());
	}
}
