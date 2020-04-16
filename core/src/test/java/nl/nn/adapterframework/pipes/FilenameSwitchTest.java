package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Before; 
import org.junit.After;
import org.mockito.Mock;

import static org.junit.Assert.*;
/** 
* FilenameSwitch Tester. 
* 
* @author <Sina Sen>
* @since <pre>Feb 28, 2020</pre> 
* @version 1.0 
*/ 
public class FilenameSwitchTest extends PipeTestBase<FilenameSwitch>{

    @Mock
    private IPipeLineSession session = new PipeLineSessionBase();

    @Override
    public FilenameSwitch createPipe() {
        return new FilenameSwitch();
    }


    /**
     * Method: setNotFoundForwardName(String notFound)
     */
    @Test
    public void testGetSetNotFoundForwardName() throws Exception {
        pipe.setNotFoundForwardName("input_not_found");
        assertEquals(pipe.getNotFoundForwardName(), "input_not_found");
        pipe.configure();
    }



    /**
     * Method: setToLowercase(boolean b)
     */
    @Test
    public void testSetToLowercase() throws Exception {
        pipe.setToLowercase(true);
        assertEquals(pipe.isToLowercase(), true);
        pipe.configure();
    }

    /**
     * Method: configure()
     */
    @Test
    public void testConfigureWithoutForwardNameAndWithoutAlternativeForward() throws Exception {
        exception.expect(PipeRunException.class);
        exception.expectMessage("Pipe [FilenameSwitch under test] msgId [null] cannot find forward or pipe named []");
        pipe.setNotFoundForwardName(null);
        pipe.configure();
        PipeRunResult res = pipe.doPipe("", session);
    }

    @Test
    public void testConfigureWithNullForwardName()  throws Exception {
        exception.expect(NullPointerException.class);
        pipe.configure();
        PipeRunResult res = pipe.doPipe(null, session);
    }

    @Test
    public void testValidForwardName() throws Exception {
        PipeRunResult res = pipe.doPipe("CreateHelloWorld/success", session);
        assertEquals(res.getPipeForward().getName(), "success");

    }

    @Test
    public void testValidForwardNameToLowerCase() throws Exception {
        pipe.setToLowercase(true);
        PipeRunResult res = pipe.doPipe("https:\\www.delft.nl/corona-besmettingsgeval-gevonden-in-delft/a\\SUCCESS", session);
        assertEquals(res.getPipeForward().getName(), "success");
    }

    @Test
    public void testValidForwardNameToLowerCaseFalse() throws Exception {
        exception.expectMessage("Pipe [FilenameSwitch under test] msgId [null] cannot find forward or pipe named [SUCCESS]");
        pipe.setToLowercase(false);
        PipeRunResult res = pipe.doPipe("https:\\www.delft.nl/corona-besmettingsgeval-gevonden-in-delft/a\\SUCCESS", session);
        assertNotEquals(res.getPipeForward().getName(), "success");
    }

    @Test
    public void testWorkWithNothFoundForwardName() throws Exception {
        pipe.setNotFoundForwardName("success");
        pipe.configure();
        PipeRunResult res = pipe.doPipe("https:\\www.delft.nl\\/corona-besmettingsgeval-gevonden-in-delft/asdSUCCasdESS", session);
        assertEquals(res.getPipeForward().getName(), "success");
    }




} 
