package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import org.junit.Test;

import static org.junit.Assert.*;

public class CompareStringPipeTest extends PipeTestBase<CompareStringPipe> {

    @Override
    public CompareStringPipe createPipe() {
        return new CompareStringPipe();
    }

    @Test(expected = ConfigurationException.class)
    public void emptySessionKeys() throws ConfigurationException {
        PipeForward forw = new PipeForward("lessthan", "/Users/apollo11/Desktop/iaf2/core/src/test/resources/Pipes");
        pipe.registerForward(forw);
        pipe.setSessionKey1("");
        pipe.setSessionKey2("");
        pipe.configure();
    }


}