package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import org.junit.Test;

import static org.junit.Assert.*;

public class DigesterPipeTest extends PipeTestBase<DigesterPipe> {

    private IPipeLineSession session = new PipeLineSessionBase();

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

    @Test
    public void testRightDigesterRulesFile() throws ConfigurationException {
        pipe.setDigesterRulesFile("digester-rules.xml");
        pipe.configure();
    }

    @Test(expected = PipeRunException.class)
    public void testWrongInputForDigester() throws ConfigurationException, PipeRunException {
        Object input = "dummyInput";
        pipe.setDigesterRulesFile("digester-rules.xml");
        pipe.configure();
        pipe.doPipe(input, session);
    }

}