package nl.nn.adapterframework.parameters;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSessionBase;

public class ParameterTest {

	@Test
	public void testUsername() throws ConfigurationException, ParameterException {
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
	public void testPassword() throws ConfigurationException, ParameterException {
		Parameter p = new Parameter();
		p.setName("dummy");
		p.setPattern("{password}");
		p.setPassword("fakePassword");
		p.configure();
		
		ParameterValueList alreadyResolvedParameters=new ParameterValueList();
		ParameterResolutionContext prc = new ParameterResolutionContext(null, new PipeLineSessionBase());
		
		assertEquals("fakePassword", p.getValue(alreadyResolvedParameters, prc));
	}
}
