package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.util.XmlUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class EscapePipeTest extends PipeTestBase<EscapePipe> {

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
        pipe.setSubstringEnd("Substring");
        pipe.setSubstringStart("Substring");
        pipe.setEncodeSubstring(true);
        pipe.configure();
        assertEquals(pipe.getSubstringStart(), "Substring");
        assertEquals(pipe.getSubstringEnd(), "Substring");

    }

    @Test
    public void testNoSubstringWithEncode() throws ConfigurationException {
        pipe.setEncodeSubstring(true);
        pipe.configure();
        assertNull(pipe.getSubstringStart());
        assertNull(pipe.getSubstringEnd());
    }

    @Test
    public void testSubstringWithEncode() throws ConfigurationException {
        pipe.setEncodeSubstring(true);
        pipe.setSubstringStart("Substring");
        pipe.setSubstringEnd("Substring");
        pipe.configure();
        assertEquals(pipe.getSubstringStart(), XmlUtils.encodeChars("Substring"));
        assertEquals(pipe.getSubstringEnd(), XmlUtils.encodeChars("Substring"));
    }

    @Test
    public void testRightSubstringsEncode() throws PipeRunException {
        pipe.setSubstringStart("Kappa");
        pipe.setSubstringEnd("Pride");
        Object input = "KappaPride";
        pipe.setDirection("encode");

        assertNotNull(doPipe(pipe, input, session).getResult()); // TODO should assert proper return value
    }

    @Test
    public void testRightSubstringsDecode() throws PipeRunException {
        pipe.setSubstringStart("Kappa");
        pipe.setSubstringEnd("Pride");
        Object input = "KappaPride";
        pipe.setDirection("decode");

        assertNotNull(doPipe(pipe, input, session).getResult()); // TODO should assert proper return value
    }
    @Test
    public void testRightSubstringsNoDirection() throws PipeRunException {
        pipe.setSubstringStart("Kappa");
        pipe.setSubstringEnd("Pride");
        Object input = "KappaPride";
        pipe.setDirection("");

        assertNotNull(doPipe(pipe, input, session)); // TODO should assert proper return value
    }

    @Test
    public void testNoSubstringEncode() throws PipeRunException {
        pipe.setDirection("encode");
        Object input = "dummyString";

        assertNotNull(doPipe(pipe, input, session).getResult()); // TODO should assert proper return value
    }

    @Test
    public void testNoSubstringDecode() throws PipeRunException {
        pipe.setDirection("decode");
        Object input = "dummyString";

        assertNotNull(doPipe(pipe, input, session).getResult()); // TODO should assert proper return value
    }

    @Test
    public void testNoSubstringNoDirection() throws PipeRunException {
        pipe.setDirection("");
        Object input = "dummyString";

        assertNotNull(doPipe(pipe, input, session)); // TODO should assert proper return value
    }


}