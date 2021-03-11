package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunResult;

public class UUIDGeneratorPipeTest extends PipeTestBase<UUIDGeneratorPipe> {

	private Object input = new Object();


    @Override
    public UUIDGeneratorPipe createPipe() {
        return new UUIDGeneratorPipe();
    }
    


    @Test(expected = ConfigurationException.class)
    public void testTypeIsNull() throws ConfigurationException {
    	pipe.setType(null);
    	pipe.configure();
    }
    
    @Test
    public void testTypeIsNormal() throws ConfigurationException {
    	String type = "numeric";
    	pipe.setType(type);
    	pipe.configure();
    	assertEquals(type, "numeric");
    }
    
    @Test(expected = ConfigurationException.class)
    public void wrongTypeGiven() throws ConfigurationException {
    	pipe.setType("dummy");
    	pipe.configure();
    }
    
    @Test
    public void checkResultNotRightType() throws Exception {
    	pipe.setType("");
    	PipeRunResult prr = doPipe(pipe, input, session);
    	String result = prr.getResult().asString();
    	assertNotNull(result);
    	assertEquals(result.length(), 31);
    }
    
    @Test
    public void checkResultRightType() throws Exception {
    	pipe.setType("alphanumeric");
    	PipeRunResult first = doPipe(pipe, input, session);
		PipeRunResult second = doPipe(pipe, input, session);

		String resultFirst = first.getResult().asString();
		String resultSecond = second.getResult().asString();

		assertEquals(resultFirst.length(), resultSecond.length());
    }
    
}
