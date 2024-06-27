package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeRunResult;

/**
 * @author <Sina Sen>
 */
public class PasswordGeneratorPipeTest extends PipeTestBase<PasswordGeneratorPipe> {

    @Override
    public PasswordGeneratorPipe createPipe() {
        return new PasswordGeneratorPipe();
    }

    @Test
    public void testGenerate() throws Exception {
        pipe.configure();
        String res = pipe.generate(3, 4, 2, 1);
		assertEquals(10, res.length());
        assertFalse(res.isEmpty());
    }

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
        pipe.configure();

        PipeRunResult res = doPipe(pipe, "pipey", session);
        assertEquals(11, res.getResult().asString().length());
    }

}
