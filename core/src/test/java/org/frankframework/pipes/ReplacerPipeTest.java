package org.frankframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.processors.CorePipeLineProcessor;
import org.frankframework.processors.CorePipeProcessor;
import org.frankframework.processors.InputOutputPipeProcessor;
import org.frankframework.processors.PipeProcessor;
import org.frankframework.stream.Message;
import org.frankframework.testutil.ParameterBuilder;

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
	public void everythingNull() {
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
	public void testConfigureWithSeparator() throws Exception {
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
		pipe.setNonXmlReplacementCharacter("l");
		pipe.setReplaceNonXmlChars(true);
		pipe.setAllowUnicodeSupplementaryCharacters(true);
		configureAndStartPipe();

		PipeRunResult res = doPipe(pipe, "<test>&\bolo\f&'</test>/jacjac:)", session);
		assertEquals("<head>&lolol&'</head>/jacjac:)", res.getResult().asString());
	}

	@Test
	public void stripNonXMLChar() throws Exception {
		pipe.setReplaceNonXmlChars(true);
		pipe.setAllowUnicodeSupplementaryCharacters(true);
		configureAndStartPipe();

		PipeRunResult res = doPipe(pipe, "<test>&\bolo\f'&</test>/jacjac:)", session);
		assertEquals("<test>&olo'&</test>/jacjac:)", res.getResult().asString());
	}

	@Test
	public void replaceStringSuccess() throws Exception {
		pipe.setReplaceNonXmlChars(false);
		configureAndStartPipe();

		PipeRunResult res = doPipe(pipe, "\b", session);
		assertEquals("\b", res.getResult().asString());
	}

	@Test
	public void replaceNonXMLCharLongerThanOne() {
		pipe.setFind("test");
		pipe.setReplace("head");
		pipe.setNonXmlReplacementCharacter("klkl");
		pipe.setReplaceNonXmlChars(true);

		ConfigurationException e = assertThrows(ConfigurationException.class, this::configurePipe);
		assertThat(e.getMessage(), Matchers.containsString("replaceNonXmlChar [klkl] has to be one character"));
	}

	@Test
	public void testReplaceParameters() throws Exception {
		pipe.addParameter(ParameterBuilder.create()
				.withName("varToSubstitute")
				.withValue("substitutedValue"));

		pipe.addParameter(ParameterBuilder.create()
				.withName("secondVarToSubstitute")
				.withValue("secondSubstitutedValue"));

		pipe.setFind("test");
		pipe.setReplace("head");
		pipe.configure();

		PipeRunResult res = doPipe(pipe, "<test>?{varToSubstitute} and ?{secondVarToSubstitute}</test>)", session);
		assertEquals("<head>substitutedValue and secondSubstitutedValue</head>)", res.getResult().asString());
	}

	@Test
	public void testSubstituteVarsViaSessionNotSupportedAnymore() throws Exception {
		session.put("varToSubstitute", "substitutedValue");
		session.put("secondVarToSubstitute", "secondSubstitutedValue");

		pipe.setFind("test");
		pipe.setReplace("head");
		pipe.setSubstituteVars(true);
		pipe.configure();

		PipeRunResult res = doPipe(pipe, "<test>${varToSubstitute} and ${secondVarToSubstitute}</test>)", session);
		assertEquals("<head>${varToSubstitute} and ${secondVarToSubstitute}</head>)", res.getResult().asString());
	}

	@Test
	public void testSubstituteVars() throws Exception {
		pipe.setFind("test");
		pipe.setReplace("head");
		pipe.setSubstituteVars(true);
		pipe.configure();

		// application.name is always set in AppConstants.properties
		PipeRunResult res = doPipe(pipe, "<test>${application.name}</test>)", session);
		assertEquals("<head>IAF</head>)", res.getResult().asString());
	}

	@Test
	@DisplayName("Combine search/replace, parameter substitution and variable resolving")
	public void combinedSearchAndReplace() throws Exception {
		pipe.addParameter(ParameterBuilder.create()
				.withName("parameter1")
				.withValue("[Parameter value 1]"));

		pipe.addParameter(ParameterBuilder.create()
				.withName("parameter2")
				.withValue("[Parameter value 2]"));

		pipe.setFind("test");
		pipe.setReplace("head");
		pipe.setSubstituteVars(true);
		pipe.configure();

		// application.name is always set in AppConstants.properties
		PipeRunResult res = doPipe(pipe, "<test>?{parameter1} and ${application.name}<br />?{parameter2}</test>", session);
		assertEquals(
				"<head>[Parameter value 1] and IAF<br />[Parameter value 2]</head>",
				res.getResult().asString()
		);
	}

	@Test
	public void testPropertyReferenceInProperty() throws Exception {
		pipe.setSubstituteVars(true);
		pipe.configure();

		PipeRunResult res = doPipe(pipe, "<test>${unresolved.property}<br /></test>", session);
		assertEquals("<test>123<br /></test>", res.getResult().asString());
	}

	@Test
	@DisplayName("Make sure that nothing is replaced if the ${} / ?{} syntax isn't closed")
	public void testSubstituteVarsIncorrectSyntax() throws Exception {
		session.put("varToSubstitute", "substitutedValue");
		pipe.addParameter(ParameterBuilder.create()
				.withName("secondVarToSubstitute")
				.withValue("secondSubstitutedValue"));

		pipe.setSubstituteVars(true);
		pipe.configure();

		PipeRunResult res = doPipe(pipe, "<test>${varToSubstitute and ?{secondVarToSubstitute</test>)", session);
		assertEquals("<test>${varToSubstitute and ?{secondVarToSubstitute</test>)", res.getResult().asString());
	}

	@Test
	@DisplayName("Test whether markSupported is doing what it should by calling peek in Message")
	public void testMarkSupportedIsFalse() throws Exception {
		session.put("exit", "Exit201");
		pipe.addParameter(ParameterBuilder.create()
				.withName("exit")
				.withSessionKey("exit"));

		pipe.configure();

		PipeRunResult res = doPipe(pipe, "statuscodeselectable: [?{exit}]", session);

		Message result = res.getResult();
		result.peek(10);

		assertEquals("statuscodeselectable: [Exit201]", result.asString());
	}

	@Test
	public void replaceSystemVarWithParamVar() throws Exception {
		session.put("prefix.value.suffix", "ignore me");
		System.setProperty("prefix.value.suffix", "replacedPropertyValue");

		try (Message message = new Message("dummy")) {
			// Correctly chain the pipe processors
			CorePipeLineProcessor pipeLineProcessor = new CorePipeLineProcessor();
			InputOutputPipeProcessor inputOutputPipeProcessor = new InputOutputPipeProcessor();
			PipeProcessor pipeProcessor = new CorePipeProcessor();
			inputOutputPipeProcessor.setPipeProcessor(pipeProcessor);

			pipeLineProcessor.setPipeProcessor(inputOutputPipeProcessor);

			pipeline.setPipeLineProcessor(pipeLineProcessor);

			pipe.setGetInputFromFixedValue("^{prefix.?{variable}.suffix}");
			pipe.setFind("^");
			pipe.setReplace("$");
			pipe.setSubstituteVars(true);
			pipe.addParameter(ParameterBuilder.create("variable", "value"));
			configureAndStartPipe();

			Message result = pipeline.process("123-456", message, session).getResult();
			assertEquals("replacedPropertyValue", result.asString());
		} finally {
			session.remove("prefix.value.suffix");
			System.clearProperty("prefix.value.suffix");
		}
	}
}
