package nl.nn.adapterframework.pipes;


import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;

public class XmlIfTest extends PipeTestBase<XmlIf>{

	@Mock
	private IPipeLineSession session = new PipeLineSessionBase();

	private String pipeForwardThen = "then";
	private String pipeForwardElse = "else";

	@Override
	public XmlIf createPipe() {
		XmlIf xmlIf = new XmlIf();

		//Add default pipes
		xmlIf.registerForward(new PipeForward(pipeForwardThen, null));
		xmlIf.registerForward(new PipeForward(pipeForwardElse, null));
		return xmlIf;
	}

	@Override
	public void notConfigured() throws ConfigurationException {
	}

	@Test
	public void nullXPathExpressionTest() throws ConfigurationException, PipeStartException, PipeRunException{
		pipe.setXpathExpression(null);
		pipe.configure();
		pipe.start();

		PipeRunResult pipeRunResult = pipe.doPipe("<test", session);
		Assert.assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void emptySessionKeyTest() throws ConfigurationException, PipeStartException, PipeRunException{
		pipe.setSessionKey("");
		pipe.configure();
		pipe.start();

		PipeRunResult pipeRunResult = pipe.doPipe("<test123", session);
		Assert.assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void someSessionKeyTest() throws ConfigurationException, PipeStartException, PipeRunException{
		pipe.setSessionKey("test");
		pipe.configure();
		pipe.start();

		session.put("test", "testValue");
		session.put("test123", "testValue");

		PipeRunResult pipeRunResult = pipe.doPipe("test123", session);
		Assert.assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void expressionValueTest() throws ConfigurationException, PipeStartException, PipeRunException{
		pipe.setExpressionValue("test");
		pipe.configure();
		pipe.start();

		PipeRunResult pipeRunResult = pipe.doPipe("<test", session);
		Assert.assertEquals(pipeForwardElse, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void emptySessionKeyNullInputTest() throws ConfigurationException, PipeStartException, PipeRunException{
		pipe.setSessionKey("");
		pipe.configure();
		pipe.start();
		
		PipeRunResult pipeRunResult = pipe.doPipe(null, session);
		Assert.assertEquals(pipeForwardElse, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void invalidXPathExpressionTest() throws ConfigurationException, PipeStartException, PipeRunException{
		exception.expect(PipeRunException.class);

		pipe.setXpathExpression("someexpression");
		pipe.configure();
		pipe.start();
		
		PipeRunResult pipeRunResult = pipe.doPipe("test", session);
		Assert.assertEquals(pipeForwardElse, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void emptyRegexTest() throws ConfigurationException, PipeStartException, PipeRunException{
		pipe.setRegex("");
		pipe.configure();
		pipe.start();

		PipeRunResult pipeRunResult = pipe.doPipe("<test", session);
		Assert.assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void someRegexTextTest() throws ConfigurationException, PipeStartException, PipeRunException{
		pipe.setRegex("some");
		pipe.configure();
		pipe.start();

		PipeRunResult pipeRunResult = pipe.doPipe("test", session);
		Assert.assertEquals(pipeForwardElse, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void emptyXPathExpressionTest() throws ConfigurationException, PipeStartException, PipeRunException{
		pipe.setXpathExpression("");
		pipe.configure();
		pipe.start();
		
		PipeRunResult pipeRunResult = pipe.doPipe("<test", session);
		Assert.assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void emptyXPathExpressionWithEmptyExpressionValueTest() throws ConfigurationException, PipeStartException, PipeRunException{
		pipe.setXpathExpression("");
		pipe.setExpressionValue("");
		pipe.configure();
		pipe.start();
		
		PipeRunResult pipeRunResult = pipe.doPipe("<test", session);
		Assert.assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void inputMatchesWithRegexTest() throws ConfigurationException, PipeStartException, PipeRunException{
		pipe.setRegex("test123");
		pipe.setExpressionValue("");
		pipe.configure();
		pipe.start();
		PipeRunResult pipeRunResult = pipe.doPipe("test123", session);
		Assert.assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void inputMatchesExpressionValueTest() throws ConfigurationException, PipeStartException, PipeRunException{
		pipe.setExpressionValue("test123");
		pipe.configure();
		pipe.start();

		PipeRunResult pipeRunResult = pipe.doPipe("test123", session);
		Assert.assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void invalidXPathExpressionValueTest() throws ConfigurationException, PipeStartException, PipeRunException{
		pipe.setXpathExpression("");
		pipe.configure();
		pipe.start();

		PipeRunResult pipeRunResult = pipe.doPipe("test123", session);
		Assert.assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void testWithInvalidThenPipe() throws ConfigurationException, PipeStartException, PipeRunException{
		String pipeName = "someText";
		pipe.setThenForwardName(pipeName);
		pipe.registerForward(new PipeForward(pipeName,null));
		pipe.configure();
		pipe.start();

		PipeRunResult pipeRunResult = pipe.doPipe("<test123", session);
		Assert.assertEquals(pipeName, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void testWithInvalidElsePipe() throws ConfigurationException, PipeStartException, PipeRunException{
		String pipeName = "someText";
		pipe.setElseForwardName(pipeName);
		pipe.registerForward(new PipeForward(pipeName,null));
		pipe.configure();
		pipe.start();

		PipeRunResult pipeRunResult = pipe.doPipe("<test123", session);
		Assert.assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void someXMLInputEmptyExpressionValue() throws ConfigurationException, PipeStartException, PipeRunException{
		pipe.setXpathExpression("/root");
		pipe.setExpressionValue("");
		pipe.configure();
		pipe.start();
		
		PipeRunResult pipeRunResult = pipe.doPipe("<root>test</root>", session);
		Assert.assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void someXMLInput() throws ConfigurationException, PipeStartException, PipeRunException{
		pipe.setXpathExpression("/root");
		pipe.setExpressionValue("test");
		pipe.configure();
		pipe.start();
		
		PipeRunResult pipeRunResult = pipe.doPipe("<root>test</root>", session);
		Assert.assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void someXMLInputNotEqualtoExpressionValue() throws ConfigurationException, PipeStartException, PipeRunException{
		pipe.setXpathExpression("/root");
		pipe.setExpressionValue("test");
		pipe.configure();
		pipe.start();
		
		PipeRunResult pipeRunResult = pipe.doPipe("<root>test123</root>", session);
		Assert.assertEquals(pipeForwardElse, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void someEmptyXMLInputTest() throws ConfigurationException, PipeStartException, PipeRunException{
		pipe.setXpathExpression("/root");
		pipe.setExpressionValue("");
		pipe.configure();
		pipe.start();
		
		PipeRunResult pipeRunResult = pipe.doPipe("<root/>", session);
		Assert.assertEquals(pipeForwardThen, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void someXMLInputNotEqualtoXPath() throws ConfigurationException, PipeStartException, PipeRunException{
		pipe.setXpathExpression("/test");
		pipe.setExpressionValue("");
		pipe.configure();
		pipe.start();
		
		PipeRunResult pipeRunResult = pipe.doPipe("<root>test123</root>", session);
		Assert.assertEquals(pipeForwardElse, pipeRunResult.getPipeForward().getName());
	}

	@Test
	public void dummyNamedElsePipe() throws ConfigurationException, PipeStartException, PipeRunException{
		exception.expect(PipeRunException.class);
		pipe.setXpathExpression("/test");
		pipe.setElseForwardName("test");
		pipe.configure();
		pipe.start();
		pipe.doPipe("<root>test123</root>", session);
	}

	@Test
	public void dummyNamedThenPipe() throws ConfigurationException, PipeStartException, PipeRunException{
		exception.expect(PipeRunException.class);
		pipe.setXpathExpression("/root");
		pipe.setThenForwardName("test");
		pipe.configure();
		pipe.start();
		pipe.doPipe("<root>test123</root>", session);
	}

	@Test
	public void spaceInputOnValidThenPipeTest() throws PipeRunException, ConfigurationException, PipeStartException{
		pipe.setThenForwardName("then");
		pipe.configure();
		pipe.start();

		PipeRunResult prr  = pipe.doPipe(" <test1", session);
		Assert.assertEquals(pipeForwardThen, prr.getPipeForward().getName());
	}

	@Test
	public void spaceInputOnInvalidThenPipeTest() throws PipeRunException, ConfigurationException, PipeStartException{
		exception.expect(PipeRunException.class);

		String pipeName = "test123";
		pipe.setThenForwardName(pipeName);
		pipe.configure();
		pipe.start();

		PipeRunResult prr  = pipe.doPipe(" <test1", session);
		Assert.assertEquals(pipeName, prr.getPipeForward().getName());
	}

	@Test
	public void tabInputOnValidThenPipeTest() throws PipeRunException, ConfigurationException, PipeStartException{
		pipe.setThenForwardName("then");
		pipe.configure();
		pipe.start();

		PipeRunResult prr  = pipe.doPipe("	<test1", session);
		Assert.assertEquals(pipeForwardThen, prr.getPipeForward().getName());
	}

	@Test
	public void tabInputOnInvalidThenPipeTest() throws PipeRunException, ConfigurationException, PipeStartException{
		exception.expect(PipeRunException.class);

		String pipeName = "test1";
		pipe.setThenForwardName(pipeName);
		pipe.configure();
		pipe.start();

		PipeRunResult prr  = pipe.doPipe("	<test1", session);
		Assert.assertEquals(pipeName, prr.getPipeForward().getName());
	}
}