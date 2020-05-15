package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.PipeRunResult;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import static org.junit.Assert.*;

/**
 * PasswordGeneratorPipe Tester.
 *
 * @author <Sina Sen>
 */
public class PasswordGeneratorPipeTest extends PipeTestBase<PasswordGeneratorPipe> {


    @Override
    public PasswordGeneratorPipe createPipe() {
        return new PasswordGeneratorPipe();
    }

    /**
     * Method: configure()
     */
    @Test
    public void testConfigure() throws Exception {
        pipe.setUseSecureRandom(true);
        pipe.configure();

    }
    /**
     * Method: generate(int numOfLCharacters, int numOfUCharacters, int numOfSigns, int numOfNumbers)
     */
    @Test
    public void testGenerate() throws Exception {
        pipe.configure();
        String res = pipe.generate(3, 4, 2, 1);
        assertEquals(res.length(), 10);
        assertFalse(res.isEmpty());
    }

    /**
     * Method: doPipe(Object input, IPipeLineSession session)
     */
    @Test
    public void testDoPipe() throws Exception {
        pipe.setLCharacters("abcd");
        pipe.setNumbers("12342");
        pipe.setUCharacters("ASDF");
        pipe.setSigns("!@#@");
        pipe.setNumOfDigits(3);
        pipe.setNumOfLCharacters(4);
        pipe.setNumOfUCharacters(2);
        pipe.setNumOfSigns(2);
        pipe.setUseSecureRandom(false);
        pipe.configure();
        PipeRunResult res = doPipe(pipe, "pipey", session);
        assertEquals(res.getResult().toString().length(), 11);
    }






}
