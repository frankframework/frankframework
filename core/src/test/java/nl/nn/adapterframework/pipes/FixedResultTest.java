package nl.nn.adapterframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hamcrest.Matchers;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.testutil.ParameterBuilder;

import org.junit.jupiter.api.Test;


/**
 * FixedResultPipe Tester.
 *
 * @author <Sina Sen>
 */
public class FixedResultTest extends PipeTestBase<FixedResultPipe> {

	private static final String sourceFolderPath = "/Pipes/2.txt";

	@Override
	public FixedResultPipe createPipe() {
		return new FixedResultPipe();
	}

	public Parameter setUp(String name){
		session.put(name,"value");
		return ParameterBuilder.create().withName(name).withValue("abs").withSessionKey("*");
	}

	@Test
	public void testSuccess() throws Exception {
		Parameter param = setUp("param1");
		pipe.addParameter(param);
		pipe.setFilename(sourceFolderPath);
		pipe.setReplaceFrom("param1");
		pipe.setReplaceTo("kar");
		pipe.setReturnString("${param1}andandandparam2");
		pipe.configure();
		PipeRunResult res = doPipe(pipe, "whatisthis", session);
		assertEquals("inside the file", res.getResult().asString());
	}

	@Test
	public void testFailAsWrongDirectory() {
		Parameter param = setUp("param1");
		pipe.addParameter(param);
		pipe.setFilename(sourceFolderPath + "/something");
		pipe.setReplaceFrom("param1");
		pipe.setReplaceTo("kar");
		pipe.setReturnString("${param1}andandandparam2");

		ConfigurationException e = assertThrows(ConfigurationException.class, this::configurePipe);
		assertThat(e.getMessage(), Matchers.endsWith("cannot find resource [/Pipes/2.txt/something]"));
	}

	@Test
	public void testEmptyFileName(){
		ConfigurationException e = assertThrows(ConfigurationException.class, this::configurePipe);
		assertThat(e.getMessage(), Matchers.endsWith("has neither filename nor filenameSessionKey nor returnString specified"));
	}

	@Test
	public void xsltSuccess() throws Exception{
		Parameter param = setUp("param1");
		pipe.addParameter(param);
		pipe.setSubstituteVars(true);
		pipe.setStyleSheetName("/Xslt/importNotFound/name.xsl");
		pipe.setReplaceFrom("param1");
		pipe.setReplaceTo("kar");
		pipe.setReturnString("${param1}andandandparam2");
		pipe.configure();
		PipeRunResult res = doPipe(pipe, "whatisthis", session);
		assertEquals("success", res.getPipeForward().getName());
	}

	@Test
	public void xsltFailForTransformation() throws Exception{
		Parameter param = setUp("param1");
		pipe.addParameter(param);
		pipe.setStyleSheetName("/Xslt/importNotFound/name2.xsl");
		pipe.setReplaceFrom("param1");
		pipe.setReplaceTo("kar");
		pipe.setReturnString("${param1}andandandparam2");
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
		pipe.setReturnString("${param1}");
		pipe.configure();

		PipeRunResult res = doPipe(pipe, "dummy", session);
		assertEquals("success", res.getPipeForward().getName());
		assertEquals("empty", res.getResult().asString());
	}

	@Test
	public void substitudeVarsFromFile() throws Exception{
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
	public void substitudeVarsFromFileConfigureTimeLookup() throws Exception{
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
	public void testFileNotFound() throws Exception{
		pipe.setFilename("nofile");
		assertThrows(nl.nn.adapterframework.configuration.ConfigurationException.class, () -> pipe.configure());
	}

	@Test
	public void substitudeVarsWithFileSessionKey() throws Exception{
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
	public void substitudeVarsInCombinationOfReplacePair() throws Exception{
		Parameter param = ParameterBuilder.create().withName("param");
		param.setDefaultValue("DefaultValue");
		pipe.addParameter(param);

		pipe.setReturnString("This is replaceFrom ${param}");
		pipe.setReplaceFrom("replaceFrom");
		pipe.setReplaceTo("replaceTo");
		pipe.configure();

		PipeRunResult res = doPipe(pipe, "propValueFromInput", session);
		assertEquals("success", res.getPipeForward().getName());
		assertEquals("This is replaceTo propValueFromInput", res.getResult().asString());
	}

	@Test
	public void substitudeVarsInCombinationOfReplacePairWithReplaceFixedParams() throws Exception{
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
		pipe.registerForward(new PipeForward("filenotfound", "dummy"));
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
}
