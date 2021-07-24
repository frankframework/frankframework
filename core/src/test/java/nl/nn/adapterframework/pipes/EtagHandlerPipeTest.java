package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunException;

public class EtagHandlerPipeTest extends PipeTestBase<EtagHandlerPipe> {

    @Override
    public EtagHandlerPipe createPipe() {
        return new EtagHandlerPipe();
    }

    @Test
    public void getterSetterAction() {
        String dummyString = "dummyString";
        pipe.setAction(dummyString);
        assertEquals(dummyString.toLowerCase(), pipe.getAction());

        pipe.setAction(null);
        assertNull(pipe.getAction());
    }

    @Test
    public void getterSetterUriPattern() {
        String dummyString = "dummyString";
        pipe.setUriPattern(dummyString);
        assertEquals(dummyString.toLowerCase(), pipe.getUriPattern().toLowerCase());

        pipe.setUriPattern(null);
        assertNull(pipe.getUriPattern());
    }

    @Test
    public void getterSetterRestPath() {
        String dummyString = "dummyString";
        pipe.setRestPath(dummyString);
        assertEquals(dummyString, pipe.getRestPath());

        pipe.setRestPath(null);
        assertNull(pipe.getRestPath());
    }

    @Test(expected = ConfigurationException.class)
    public void testNoActionGiven() throws ConfigurationException {
        pipe.setAction(null);
        pipe.configure();
    }

    @Test(expected = ConfigurationException.class)
    public void testWrongActionGiven() throws ConfigurationException {
        pipe.setAction("dummyAction");
        pipe.configure();
    }

    @Test(expected = ConfigurationException.class)
    public void testNoUriPatternGiven() throws ConfigurationException {
        pipe.setAction("generate");
        pipe.configure();
    }

    @Test(expected = PipeRunException.class)
    public void testInputNull() throws PipeRunException {
        doPipe(pipe, null, session);
    }


    @Test(expected = PipeRunException.class)
    public void testWrongInputFormat() throws PipeRunException {
        doPipe(pipe, 5000, session);
    }

    @Test(expected = PipeRunException.class)
    public void testFailedToLocateCache() throws PipeRunException {
        doPipe(pipe, "dummyString", session);
    }

    @Test(expected = PipeRunException.class)
    public void testFailedToLocateEtag() throws PipeRunException, ConfigurationException {
        pipe.setAction("generate");
        pipe.setUriPattern("dummyPattern");
        pipe.configure();
        doPipe(pipe, "dummyString", session);
    }




}