package nl.nn.adapterframework.parameters;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSessionBase;

public class ParameterTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void testPatternUsername() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("{username}");
		p.setUserName("fakeUsername");
		p.configure();
		
		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		ParameterResolutionContext prc = new ParameterResolutionContext(null, new PipeLineSessionBase());
		
		assertEquals("fakeUsername", p.getValue(alreadyResolvedParameters, prc));
	}

	@Test
	public void testPatternPassword() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("{password}");
		p.setPassword("fakePassword");
		p.configure();
		
		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		ParameterResolutionContext prc = new ParameterResolutionContext(null, new PipeLineSessionBase());
		
		assertEquals("fakePassword", p.getValue(alreadyResolvedParameters, prc));
	}

	@Test
	public void testPatternSessionVariable() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("{sessionKey}");
		p.configure();
		
		IPipeLineSession session = new PipeLineSessionBase();
		session.put("sessionKey", "fakeSessionVariable");
		
		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		ParameterResolutionContext prc = new ParameterResolutionContext(null, session);
		
		assertEquals("fakeSessionVariable", p.getValue(alreadyResolvedParameters, prc));
	}

	@Test
	public void testPatternParameter() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("{siblingParameter}");
		p.configure();
		
		IPipeLineSession session = new PipeLineSessionBase();

		ParameterResolutionContext prc = new ParameterResolutionContext(null, session);
		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Parameter siblingParameter = new Parameter();
		siblingParameter.setName("siblingParameter");
		siblingParameter.setValue("fakeParameterValue");
		siblingParameter.configure();
		alreadyResolvedParameters.add(new ParameterValue(siblingParameter, siblingParameter.getValue(alreadyResolvedParameters, prc)));
		
		assertEquals("fakeParameterValue", p.getValue(alreadyResolvedParameters, prc));
	}

	@Test
	public void testPatternCombined() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("param [{siblingParameter}] sessionKey [{sessionKey}] username [{username}] password [{password}]");
		p.setUserName("fakeUsername");
		p.setPassword("fakePassword");
		p.configure();
		
		
		IPipeLineSession session = new PipeLineSessionBase();
		session.put("sessionKey", "fakeSessionVariable");
		
		ParameterResolutionContext prc = new ParameterResolutionContext(null, session);
		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		Parameter siblingParameter = new Parameter();
		siblingParameter.setName("siblingParameter");
		siblingParameter.setValue("fakeParameterValue");
		siblingParameter.configure();
		alreadyResolvedParameters.add(new ParameterValue(siblingParameter, siblingParameter.getValue(alreadyResolvedParameters, prc)));
		
		assertEquals("param [fakeParameterValue] sessionKey [fakeSessionVariable] username [fakeUsername] password [fakePassword]", p.getValue(alreadyResolvedParameters, prc));
	}

	@Test
	public void testPatternUnknownSessionVariableOrParameter() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("{unknown}");
		p.configure();
		
		IPipeLineSession session = new PipeLineSessionBase();
		
		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		ParameterResolutionContext prc = new ParameterResolutionContext(null, session);
		
		exception.expectMessage("Parameter or session variable with name [unknown] in pattern [{unknown}] cannot be resolved");
		p.getValue(alreadyResolvedParameters, prc);
	}

}
