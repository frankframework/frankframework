package nl.nn.adapterframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import org.hamcrest.Matchers;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunResult;

/**
 * ReplacerPipe Tester.
 *
 * @author <Sina Sen>
 */
public class ReplacerPipeTest extends PipeTestBase<ReplacerPipe> {

	@Override
	public ReplacerPipe createPipe() {
		return new ReplacerPipe();
	}

	@Test
	public void everythingNull() throws Exception {
		pipe.setFind("laa");

		ConfigurationException e = assertThrows(ConfigurationException.class, this::configurePipe);
		assertThat(e.getMessage(), Matchers.containsString("cannot have a null replace-attribute"));
	}

	@Test
	public void getFindEmpty() throws Exception {
		pipe.setFind("");
		configureAndStartPipe();

		PipeRunResult res = doPipe(pipe, "dsf", session);
		assertFalse(res.getPipeForward().getName().isEmpty());

	}

	@Test
	public void testConfigureWithSeperator() throws Exception {
		pipe.setFind("sina/murat/niels");
		pipe.setLineSeparatorSymbol("/");
		pipe.setReplace("yo");
		pipe.setAllowUnicodeSupplementaryCharacters(true);
		configureAndStartPipe();

		doPipe(pipe, pipe.getFind(), session);
		assertFalse(pipe.getFind().isEmpty());
	}

	@Test
	public void replaceNonXMLChar() throws Exception {
		pipe.setFind("test");
		pipe.setReplace("head");
		pipe.setReplaceNonXmlChar("l");
		pipe.setReplaceNonXmlChars(true);
		pipe.setAllowUnicodeSupplementaryCharacters(true);
		configureAndStartPipe();

		PipeRunResult res = doPipe(pipe, "<test>\bolo</test>/jacjac:)", session);
		assertEquals("<head>lolo</head>/jacjac:)", res.getResult().asString());
	}

	@Test
	public void replaceStringSuccess() throws Exception {
		pipe.setReplaceNonXmlChars(false);
		configureAndStartPipe();

		PipeRunResult res = doPipe(pipe, "\b", session);
		assertEquals("\b", res.getResult().asString());
	}

	@Test
	public void replaceNonXMLCharLongerThanOne() throws Exception {
		pipe.setFind("test");
		pipe.setReplace("head");
		pipe.setReplaceNonXmlChar("klkl");
		pipe.setReplaceNonXmlChars(true);

		ConfigurationException e = assertThrows(ConfigurationException.class, this::configurePipe);
		assertThat(e.getMessage(), Matchers.containsString("replaceNonXmlChar [klkl] has to be one character"));
	}

}
