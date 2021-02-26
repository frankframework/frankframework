package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunException;

public class DigesterPipeTest extends PipeTestBase<DigesterPipe> {

    @Override
    public DigesterPipe createPipe() {
        return new DigesterPipe();
    }

    @Test
    public void getterSetterDigesterRuleFile() {
        String dummyString = "dummyString";
        pipe.setDigesterRulesFile(dummyString);
        assertEquals(pipe.getDigesterRulesFile(), dummyString);
    }

    @Test(expected = ConfigurationException.class)
    public void testNonExistingDigesterRulesFile() throws ConfigurationException {
        pipe.configure();
    }

    @Test(expected = ConfigurationException.class)
    public void testWrongDigesterRulesFile() throws ConfigurationException {
        pipe.setDigesterRulesFile("Hello World");
        pipe.configure();
    }

    @Test(expected = PipeRunException.class)
    public void testWrongInputForDigester() throws ConfigurationException, PipeRunException {
        Object input = "dummyInput";
        pipe.setDigesterRulesFile("digester-rules.xml");
        pipe.configure();
        doPipe(pipe, input, session);
    }

}