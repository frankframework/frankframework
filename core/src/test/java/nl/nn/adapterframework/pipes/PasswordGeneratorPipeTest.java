package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.PipeRunResult;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


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
        assertEquals(11, res.getResult().asString().length());
    }






}
