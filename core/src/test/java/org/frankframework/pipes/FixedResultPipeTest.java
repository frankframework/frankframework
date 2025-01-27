package org.frankframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.Parameter;
import org.frankframework.stream.Message;
import org.frankframework.testutil.ParameterBuilder;

/**
 * FixedResultPipe Tester.
 *
 * @author <Sina Sen>
 */
public class FixedResultPipeTest extends PipeTestBase<FixedResultPipe> {

	private static final String PIPES_2_TXT = "/Pipes/2.txt";
	private static final String PIPES_FILE_PDF = "/Pipes/file.pdf";

	@Override
	public FixedResultPipe createPipe() {
		return new FixedResultPipe();
	}

	@Test
	public void testSuccessWithAttribute() throws Exception {
		pipe.setFilename(PIPES_2_TXT);
		pipe.configure();

		PipeRunResult pipeRunResult = doPipe(pipe, "whatisthis", session);

		try (InputStream inputStream = pipeRunResult.getResult().asInputStream()) {
			String fileContents = new String(inputStream.readAllBytes());

			assertEquals("inside the file", fileContents);
		}
	}

	@Test
	public void testFailureWithAttribute() {
		pipe.setFilename(PIPES_2_TXT + "/something.txt");

		assertThrows(ConfigurationException.class, this::configurePipe);
	}

	@Test
	public void testFailureWithParam() {
		Parameter filename = ParameterBuilder.create().withName("filename").withValue(PIPES_2_TXT + "/something.txt");
		pipe.addParameter(filename);

		assertThrows(ConfigurationException.class, this::configurePipe);
	}

	@Test
	public void testBinaryContent() throws Exception {
		pipe.setFilename(PIPES_FILE_PDF);
		pipe.configure();

		PipeRunResult pipeRunResult = doPipe(pipe, "whatisthis", session);

		try (InputStream inputStreamFromPipe = pipeRunResult.getResult().asInputStream()) {
			URL resourceFromClasspath = FixedResultPipe.class.getResource(PIPES_FILE_PDF);
			InputStream inputStream = resourceFromClasspath.openStream();
			boolean contentEquals = IOUtils.contentEquals(inputStreamFromPipe, inputStream);

			inputStream.close();

			assertTrue(contentEquals, "File contents differ");
		}
	}

	private Parameter getParameter(String name){
		session.put(name,"value");
		return ParameterBuilder.create()
				.withName(name)
				.withValue("abs")
				.withSessionKey("*");
	}

	@Test
	public void testSuccess() throws Exception {
		Parameter param = getParameter("param1");
		pipe.addParameter(param);
		pipe.setFilename(PIPES_2_TXT);
		pipe.setReplaceFrom("param1");
		pipe.setReplaceTo("kar");
		pipe.setReturnString("?{param1}andandandparam2");
		pipe.configure();
		PipeRunResult res = doPipe(pipe, "whatisthis", session);
		assertEquals("inside the file", res.getResult().asString());
	}

	@Test
	public void testFailAsWrongDirectory() {
		Parameter param = getParameter("param1");
		pipe.addParameter(param);
		pipe.setFilename(PIPES_2_TXT + "/something");
		pipe.setReplaceFrom("param1");
		pipe.setReplaceTo("kar");
		pipe.setReturnString("?{param1}andandandparam2");

		ConfigurationException e = assertThrows(ConfigurationException.class, this::configurePipe);
		assertThat(e.getMessage(), Matchers.endsWith("cannot find resource [/Pipes/2.txt/something]"));
	}

	@Test
	public void xsltSuccess() throws Exception{
		Parameter param = getParameter("param1");
		pipe.addParameter(param);
		pipe.setSubstituteVars(true);
		pipe.setStyleSheetName("/Xslt/importNotFound/name.xsl");
		pipe.setReplaceFrom("param1");
		pipe.setReplaceTo("kar");
		pipe.setReturnString("?{param1}andandandparam2");
		pipe.configure();
		PipeRunResult res = doPipe(pipe, "whatisthis", session);
		assertEquals("success", res.getPipeForward().getName());
	}

	@Test
	public void xsltFailForTransformation() throws Exception{
		Parameter param = getParameter("param1");
		pipe.addParameter(param);
		pipe.setStyleSheetName("/Xslt/importNotFound/name2.xsl");
		pipe.setReplaceFrom("param1");
		pipe.setReplaceTo("kar");
		pipe.setReturnString("?{param1}andandandparam2");
		pipe.configure();

		PipeRunException e = assertThrows(PipeRunException.class, ()->doPipe(pipe, "whatisthis", session));
		assertThat(e.getMessage(), Matchers.containsString("error transforming message"));
	}

	@Test
	public void testXsltWithReturnedStringReplaced() throws Exception{
		Parameter param = ParameterBuilder.create().withName("param").withValue("<result><field>empty</field></result>");
		pipe.addParameter(param);
		pipe.setSubstituteVars(true);
		pipe.setStyleSheetName("/Xslt/extract.xslt");
		pipe.setReplaceFrom("param1");
		pipe.setReplaceTo("param");
		pipe.setReturnString("?{param1}");
		pipe.configure();

		PipeRunResult res = doPipe(pipe, "dummy", session);
		assertEquals("success", res.getPipeForward().getName());
		assertEquals("empty", res.getResult().asString());
	}

	@Test
	public void substituteVarsFromFile() throws Exception{
		Parameter param = ParameterBuilder.create().withName("myprop");
		param.setDefaultValue("DefaultValue");
		pipe.addParameter(param);
		pipe.setFilename("/FixedResult/fixedResultPipeInput.txt");
		pipe.setSubstituteVars(true);
		pipe.configure();

		PipeRunResult res = doPipe(pipe, "propValueFromInput", session);
		assertEquals("success", res.getPipeForward().getName());
		assertEquals("Hello propValueFromInput", res.getResult().asString());
	}

	@Test
	public void substituteVarsFromFileConfigureTimeLookup() throws Exception{
		Parameter param = ParameterBuilder.create().withName("myprop");
		param.setDefaultValue("DefaultValue");
		pipe.addParameter(param);
		pipe.setFilename("/FixedResult/fixedResultPipeInput.txt");
		pipe.setSubstituteVars(true);
		pipe.configure();

		PipeRunResult res = doPipe(pipe, "propValueFromInput", session);
		assertEquals("success", res.getPipeForward().getName());
		assertEquals("Hello propValueFromInput", res.getResult().asString());
	}

	@Test
	public void testFileNotFound() {
		pipe.setFilename("nofile");
		assertThrows(ConfigurationException.class, () -> pipe.configure());
	}

	@Test
	public void substituteVarsWithFileSessionKey() throws Exception{
		Parameter param = ParameterBuilder.create().withName("myprop");
		param.setDefaultValue("DefaultValue");
		pipe.addParameter(param);

		session.put("filename", "/FixedResult/fixedResultPipeInput.txt");

		pipe.setFilenameSessionKey("filename");
		pipe.setReplaceFrom("Hello");
		pipe.setReplaceTo("HelloTo");
		pipe.setSubstituteVars(true);
		pipe.configure();

		PipeRunResult res = doPipe(pipe, "propValueFromInput", session);
		assertEquals("success", res.getPipeForward().getName());
		assertEquals("HelloTo propValueFromInput", res.getResult().asString());
	}

	@Test
	public void replaceFixedParamsTest() throws Exception{
		Parameter param = ParameterBuilder.create().withName("myprop");
		param.setDefaultValue("DefaultValue");
		pipe.addParameter(param);
		pipe.setReturnString("This is myprop");
		pipe.setReplaceFixedParams(true);
		pipe.configure();

		PipeRunResult res = doPipe(pipe, "propValueFromInput", session);
		assertEquals("success", res.getPipeForward().getName());
		assertEquals("This is propValueFromInput", res.getResult().asString());
	}

	@Test
	public void substituteVarsInCombinationOfReplacePair() throws Exception{
		Parameter param = ParameterBuilder.create().withName("param");
		param.setDefaultValue("DefaultValue");
		pipe.addParameter(param);

		pipe.setReturnString("This is replaceFrom ?{param}");
		pipe.setReplaceFrom("replaceFrom");
		pipe.setReplaceTo("replaceTo");
		pipe.configure();

		PipeRunResult res = doPipe(pipe, "propValueFromInput", session);
		assertEquals("success", res.getPipeForward().getName());
		assertEquals("This is replaceTo propValueFromInput", res.getResult().asString());
	}

	@Test
	public void substituteVarsInCombinationOfReplacePairWithReplaceFixedParams() throws Exception{
		Parameter param = ParameterBuilder.create().withName("param");
		param.setDefaultValue("DefaultValue");
		pipe.addParameter(param);

		pipe.setReturnString("This is replaceFrom param");
		pipe.setReplaceFixedParams(true);
		pipe.setReplaceFrom("replaceFrom");
		pipe.setReplaceTo("replaceTo");
		pipe.configure();

		PipeRunResult res = doPipe(pipe, "propValueFromInput", session);
		assertEquals("success", res.getPipeForward().getName());
		assertEquals("This is replaceTo propValueFromInput", res.getResult().asString());
	}

	@Test
	public void testFilenameSessionKeyPointsToNonexistingFile() throws Exception {
		session.put("filename", "nofile");

		pipe.setFilenameSessionKey("filename");
		pipe.addForward(new PipeForward("filenotfound", "dummy"));
		pipe.configure();

		PipeRunResult res = doPipe(pipe, "propValueFromInput", session);
		assertEquals("filenotfound", res.getPipeForward().getName());
	}

	@Test
	public void testEmptyReturnString() throws Exception {
		pipe.setReturnString("");
		pipe.configure();

		PipeRunResult res = doPipe(pipe, "propValueFromInput", session);
		assertEquals("success", res.getPipeForward().getName());
		assertEquals("", res.getResult().asString());
	}

	@Test
	public void substituteVarsOldWithInvalidAttribute() throws Exception{
		Parameter param = ParameterBuilder.create().withName("param");
		param.setDefaultValue("DefaultValue");
		pipe.addParameter(param);

		pipe.setReturnString("This is ${param}");
		pipe.setUseOldSubstitutionStartDelimiter(true);
		ConfigurationException ce = assertThrows(ConfigurationException.class, pipe::configure);
		assertEquals("attribute [useOldSubstitutionStartDelimiter] may only be used in combination with attribute [filename]", ce.getMessage());
	}

	@Test
	public void substituteVarsOldWithFileAndParam() throws Exception{
		pipe.addParameter(ParameterBuilder.create().withName("myprop").withValue("Sinatra"));

		pipe.setFilename("/FixedResult/fixedResultPipeInput_oldStyle.txt");
		pipe.setUseOldSubstitutionStartDelimiter(true);
		pipe.configure();

		PipeRunResult res = doPipe(pipe, "propValueFromInput", session);
		assertEquals("success", res.getPipeForward().getName());
		assertEquals("Hello Sinatra ${instance.name}", res.getResult().asString());
	}

	@Test
	public void substituteVarsOldWithFileAndProperties() throws Exception{
		pipe.setFilename("/FixedResult/fixedResultPipeInput_oldStyle.txt");
		pipe.setUseOldSubstitutionStartDelimiter(true);
		pipe.setSubstituteVars(true);
		pipe.configure();

		PipeRunResult res = doPipe(pipe, "propValueFromInput", session);
		assertEquals("success", res.getPipeForward().getName());
		assertEquals("Hello  TestConfiguration", res.getResult().asString());
	}

	@Test
	@DisplayName("Proves that an unused param will not be replaced")
	public void testUnusedParam() throws Exception {
		// Arguments.of("?", Map.of("param", "parameterValue"), "hello ?{param} world ?{unusedParam}.", "hello parameterValue world ?{unusedParam}."),
		pipe.setReturnString("hello ?{param} world ?{unusedParam}.");
		Parameter param = ParameterBuilder.create()
				.withName("param")
				.withValue("parameterValue");
		pipe.addParameter(param);
		pipe.configure();

		PipeRunResult res = doPipe(pipe, Message.nullMessage(), session);
		assertEquals("hello parameterValue world ?{unusedParam}.", res.getResult().asString());
	}
}
