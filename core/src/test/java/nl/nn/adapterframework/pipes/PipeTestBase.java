package nl.nn.adapterframework.pipes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.PipeForward;

public abstract class PipeTestBase<P extends IPipe> {
	protected Log log = LogFactory.getLog(this.getClass());
	
	protected P pipe;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	public abstract P createPipe();
	
	@Before
	public void setup() {
		pipe = createPipe();
		pipe.registerForward(new PipeForward("success",null));
	}

	
	@Test
	public void notConfigured() throws ConfigurationException {
		pipe = createPipe();
		exception.expect(ConfigurationException.class);
		pipe.configure();
	}

	@Test
	public void basicNoAdditionalConfig() throws ConfigurationException {
		pipe.configure();
	}

	protected String readLines(Reader reader) throws IOException {
        BufferedReader buf = new BufferedReader(reader);
        StringBuilder string = new StringBuilder();
        String line = buf.readLine();
        while (line != null) {
            string.append(line);
            line = buf.readLine();
            if (line!=null) {
            	string.append("\n");
            }
        }
        return string.toString();	
	}

    protected String getFile(String file) throws IOException {
        return readLines(new InputStreamReader(XmlValidator.class.getResourceAsStream(file)));
    }
}
