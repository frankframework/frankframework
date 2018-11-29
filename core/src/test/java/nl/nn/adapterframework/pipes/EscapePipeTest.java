package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import org.junit.Test;

import static org.junit.Assert.*;

public class EscapePipeTest extends PipeTestBase<EscapePipe> {

    private IPipeLineSession session = new PipeLineSessionBase();

    @Override
    public EscapePipe createPipe() {
        return new EscapePipe();
    }

    @Test
    public void getterSetterSubstringStart() {
        String dummyString = "dummyString";
        pipe.setSubstringStart(dummyString);
        assertEquals(pipe.getSubstringStart(), dummyString);
    }

    @Test
    public void getterSetterSubstringEnd() {
        String dummyString = "dummyString";
        pipe.setSubstringEnd(dummyString);
        assertEquals(pipe.getSubstringEnd(), dummyString);
    }

    @Test
    public void getterSetterDirection() {
        String dummyString = "dummyString";
        pipe.setDirection(dummyString);
        assertEquals(pipe.getDirection(), dummyString);
    }

    @Test
    public void getterSetterEncodeSubstring() {
        pipe.setEncodeSubstring(true);
        boolean otherBool = pipe.isEncodeSubstring();
        assertEquals(true, otherBool);

        pipe.setEncodeSubstring(false);
        otherBool = pipe.isEncodeSubstring();
        assertEquals(false, otherBool);
    }

    @Test(expected = ConfigurationException.class)
    public void testNoDirectionGiven() throws ConfigurationException {
        pipe.setDirection(null);
        pipe.configure();
    }

    @Test(expected = ConfigurationException.class)
    public void testWrongDirectionGiven() throws ConfigurationException {
        pipe.setDirection("NoDirection");
        pipe.configure();
    }

    @Test(expected = ConfigurationException.class)
    public void testNoSubstringEnd() throws ConfigurationException {
        pipe.setSubstringStart("Substring");
        pipe.configure();
    }

    @Test(expected = ConfigurationException.class)
    public void testNoSubstringStart() throws ConfigurationException {
        pipe.setSubstringEnd("Substring");
        pipe.configure();
    }

    @Test
    public void testNoSubstringAtAll() throws ConfigurationException {
        pipe.configure();

        pipe.setSubstringEnd("Substring");
        pipe.setSubstringStart("Substring");
        pipe.configure();
    }

    @Test
    public void testNoSubstringWithEncode() throws ConfigurationException {
        pipe.setEncodeSubstring(true);
        pipe.configure();
    }

    @Test
    public void testSubstringWithEncode() throws ConfigurationException {
        pipe.setEncodeSubstring(true);
        pipe.setSubstringStart("Substring");
        pipe.setSubstringEnd("Substring");
        pipe.configure();
    }

    @Test
    public void testRightSubstringsEncode() throws PipeRunException {
        pipe.setSubstringStart("Kappa");
        pipe.setSubstringEnd("Pride");
        Object input = "KappaPride";
        pipe.setDirection("encode");

        assertNotNull(pipe.doPipe(input, session).getResult());
    }

    @Test
    public void testRightSubstringsDecode() throws PipeRunException {
        pipe.setSubstringStart("Kappa");
        pipe.setSubstringEnd("Pride");
        Object input = "KappaPride";
        pipe.setDirection("decode");

        assertNotNull(pipe.doPipe(input, session).getResult());
    }
    @Test
    public void testRightSubstringsNoDirection() throws PipeRunException {
        pipe.setSubstringStart("Kappa");
        pipe.setSubstringEnd("Pride");
        Object input = "KappaPride";
        pipe.setDirection("");

        assertNotNull(pipe.doPipe(input, session));
    }

    @Test
    public void testNoSubstringEncode() throws PipeRunException {
        pipe.setDirection("encode");
        Object input = "dummyString";

        assertNotNull(pipe.doPipe(input, session).getResult());
    }

    @Test
    public void testNoSubstringDecode() throws PipeRunException {
        pipe.setDirection("decode");
        Object input = "dummyString";

        assertNotNull(pipe.doPipe(input, session).getResult());
    }

    @Test
    public void testNoSubstringNoDirection() throws PipeRunException {
        pipe.setDirection("");
        Object input = "dummyString";

        assertNotNull(pipe.doPipe(input, session));
    }


}