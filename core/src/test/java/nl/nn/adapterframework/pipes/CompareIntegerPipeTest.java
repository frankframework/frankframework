package nl.nn.adapterframework.pipes;

import net.sf.saxon.event.PipelineConfiguration;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.*;
import org.junit.Test;

import static org.junit.Assert.*;

public class CompareIntegerPipeTest extends PipeTestBase<CompareIntegerPipe> {

    private IPipeLineSession session = new PipeLineSessionBase();

    @Override
    public CompareIntegerPipe createPipe() {
        return new CompareIntegerPipe();
    }

    @Test(expected = ConfigurationException.class)
    public void emptySessionKey1() throws ConfigurationException {
        pipe.setSessionKey1("");
        pipe.setSessionKey2("Session 2");
        pipe.configure();
    }

    @Test(expected = ConfigurationException.class)
    public void emptySessionKey2() throws ConfigurationException {
        pipe.setSessionKey1("Session 1");
        pipe.setSessionKey2("");
        pipe.configure();
    }

    @Test(expected = PipeRunException.class)
    public void wrongSessionKey() throws PipeRunException {
        pipe.setSessionKey1("1");
        pipe.setSessionKey2("2");
        pipe.doPipe("input", session);
    }

    @Test(expected = PipeRunException.class)
    public void nullSessionKey() throws PipeRunException {
        pipe.setSessionKey1(null);
        pipe.setSessionKey2(null);
        pipe.doPipe("input", session);
    }

    @Test
    public void getSessionKey1() {
        String dummy = "dummy123";
        pipe.setSessionKey1(dummy);
        String key = pipe.getSessionKey1();
        assertEquals(dummy, key);
    }

    @Test
    public void getSessionKey2() {
        String dummy = "dummy123";
        pipe.setSessionKey2(dummy);
        String key = pipe.getSessionKey2();
        assertEquals(dummy, key);
    }

}