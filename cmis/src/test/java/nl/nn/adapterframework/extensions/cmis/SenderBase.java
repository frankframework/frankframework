package nl.nn.adapterframework.extensions.cmis;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.pipes.XmlValidator;
import nl.nn.adapterframework.util.LogUtil;

public abstract class SenderBase<S extends ISender> extends Mockito {

	protected Logger log = LogUtil.getLogger(this);
	protected S sender;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Mock
	protected PipeLineSession session = new PipeLineSession();

	public abstract S createSender() throws ConfigurationException;

	@Before
	public void setup() throws ConfigurationException, PipeStartException, SenderException {
		sender = createSender();
	}

	@After
	public void setdown() throws SenderException {
		if (sender!=null) {
			sender.close();
		}
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

	protected void assertEqualsIgnoreRN(String a, String b) {
		assertEquals(removeRegexCharactersFromInput(a, "\r\n"), removeRegexCharactersFromInput(b, "\r\n"));
	}
	protected void assertEqualsIgnoreRNTSpace(String a, String b) {
		assertEquals(removeRegexCharactersFromInput(a, "[\\n\\t\\r ]"), removeRegexCharactersFromInput(b, "[\\n\\t\\r ]"));
	}

	private String removeRegexCharactersFromInput(String input, String regex) {
		if(input == null) return null;
		return input.replaceAll(regex, "");
	}
}
