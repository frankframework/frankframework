package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import org.junit.Test;

import static org.junit.Assert.*;

public class CompareStringPipeTest extends PipeTestBase<CompareStringPipe> {

    @Override
    public CompareStringPipe createPipe() {
        return new CompareStringPipe();
    }

    @Test(expected = ConfigurationException.class)
    public void emptySessionKeys() throws ConfigurationException {
        pipe.setSessionKey1("");
        pipe.setSessionKey2("");
        pipe.configure();
    }

    @Test
    public void setSessionKey1() {
        String dummyKey = "kappa123";
        pipe.setSessionKey1(dummyKey);
        String retrievedKey = pipe.getSessionKey1();
        assertEquals(dummyKey, retrievedKey);
    }

    @Test
    public void setSessionKey2() {
        String dummyKey = "Kappa123";
        pipe.setSessionKey2(dummyKey);
        String retrievedKey = pipe.getSessionKey2();
        assertEquals(dummyKey, retrievedKey);
    }
}