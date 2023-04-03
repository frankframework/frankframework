package nl.nn.adapterframework.pipes;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.http.MediaType;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageContext;
import nl.nn.adapterframework.testutil.MatchUtils;

public class Text2XmlPipeTest extends PipeTestBase<Text2XmlPipe> {

	@Override
	public Text2XmlPipe createPipe() {
		return new Text2XmlPipe();
	}

	@Test
	public void testSuccessCDataAndReplaceNonXMLSplitLines() throws Exception {
		pipe.setXmlTag("address");
		pipe.setSplitLines(true);
		configureAndStartPipe();

		String expectedOutput = "<address>"
					+ "<line><![CDATA[this is an example]]></line>\n"
					+ "<line><![CDATA[im in cdata]]></line>"
					+ "</address>";

		PipeRunResult res = doPipe(pipe, "this is an example\nim in cdata", session);
		MatchUtils.assertXmlEquals(expectedOutput, res.getResult().asString());
	}

	@Test
	public void testSuccessCDataAndXMLDeclaration() throws Exception {
		pipe.setXmlTag("address");
		pipe.setSplitLines(false);
		pipe.setUseCdataSection(false);
		configureAndStartPipe();

		String expectedOutput = "<address>this is an example\n" + "im not in cdata</address>";

		PipeRunResult res = doPipe(pipe, "this is an example\nim not in cdata", session);
		assertEquals(expectedOutput, res.getResult().asString());
	}

	@Test
	public void testWithoutXmlDeclarationAndCDATA() throws Exception {
		pipe.setXmlTag("address");
		pipe.setSplitLines(false);
		pipe.setUseCdataSection(false);
		configureAndStartPipe();

		String expectedOutput = "<address>this is an example\nim not in cdata</address>";

		PipeRunResult res = doPipe(pipe, "this is an example\nim not in cdata", session);
		assertEquals(expectedOutput, res.getResult().asString());
	}

	@Test
	public void testSplitLinesWithoutCDATA() throws Exception {
		pipe.setXmlTag("address");
		pipe.setSplitLines(true);
		pipe.setUseCdataSection(false);
		configureAndStartPipe();

		String expectedOutput = "<address>"
					+ "<line>this is an example</line>\n"
					+ "<line>im not in cdata</line>"
					+ "</address>";

		PipeRunResult res = doPipe(pipe, "this is an example\nim not in cdata", session);
		MatchUtils.assertXmlEquals(expectedOutput, res.getResult().asString());
	}

	@Test
	public void testXmlInput() throws Exception {
		pipe.setXmlTag("root");
		pipe.setUseCdataSection(false);
		configureAndStartPipe();

		String expectedOutput = "<root><normal><xml/><input/></normal></root>";

		Message message = new Message("<normal><xml/><input/></normal>", new MessageContext().withMimeType(MediaType.APPLICATION_XML));
		PipeRunResult res = doPipe(pipe, message, session);
		assertEquals(expectedOutput, res.getResult().asString());
		MatchUtils.assertXmlEquals(expectedOutput, res.getResult().asString()); //Parses both strings as XML, uses pretty print filter
	}

	@Test
	public void testMultilineXmlInput() throws Exception {
		pipe.setXmlTag("root");
		pipe.setUseCdataSection(false);
		configureAndStartPipe();

		String expectedOutput = "<root><normal>\n<xml/>\n<input/>\n</normal></root>";

		Message message = new Message("<normal>\n<xml/>\n<input/>\n</normal>", new MessageContext().withMimeType(MediaType.APPLICATION_XML));
		PipeRunResult res = doPipe(pipe, message, session);
		assertEquals(expectedOutput, res.getResult().asString());
		MatchUtils.assertXmlEquals(expectedOutput, res.getResult().asString()); //Parses both strings as XML, uses pretty print filter
	}

	@Test
	public void testInvalidMultilineXmlInput() throws Exception {
		pipe.setXmlTag("root");
		pipe.setUseCdataSection(false);
		configureAndStartPipe();

		PipeRunException e = assertThrows(PipeRunException.class, ()->doPipe(pipe, "<invalid>\n<xml>\n<input>\n</invalid>", session));
		assertThat(e.getMessage(), containsString("The element type \"input\" must be terminated by the matching end-tag \"</input>\""));
	}

	@Test
	public void testInvalidXmlInput() throws Exception {
		pipe.setXmlTag("root");
		pipe.setUseCdataSection(false);
		configureAndStartPipe();

		PipeRunException e = assertThrows(PipeRunException.class, ()->doPipe(pipe, "<invalid><xml><input></invalid>", session));
		assertThat(e.getMessage(), containsString("The element type \"input\" must be terminated by the matching end-tag \"</input>\""));
	}

	@Test
	public void testInvalidXmlCharsReplacedWithoutCDATA() throws Exception {
		pipe.setXmlTag("address");
		pipe.setUseCdataSection(false);
		configureAndStartPipe();

		String expectedOutput = "<address>¿</address>";

		PipeRunResult res = doPipe(pipe, "\b", session);
		assertEquals(expectedOutput, res.getResult().asString());
	}

	@Test
	public void testEmptyInputAndNoReplacementForInvalidXmlChars() throws Exception {
		pipe.setXmlTag("address");
		pipe.setUseCdataSection(false);
		pipe.setReplaceNonXmlChars(false);
		configureAndStartPipe();

		String expectedOutput = "<address/>";

		PipeRunResult res = doPipe(pipe, "", session);
		MatchUtils.assertXmlEquals(expectedOutput, res.getResult().asString());
	}

	@Test
	public void testSuccessWithoutAdditionalProperties() throws Exception {
		pipe.setXmlTag("address");
		configureAndStartPipe();

		PipeRunResult res = doPipe(pipe, "this will be in cdata tag\b", session);
		assertEquals("<address><![CDATA[this will be in cdata tag¿]]></address>", res.getResult().asString());
	}

	@Test
	public void testSuccessSplitWithoutReplacingNonXMLChars() throws Exception {
		pipe.setXmlTag("address");
		pipe.setSplitLines(true);
		pipe.setUseCdataSection(true);
		pipe.setReplaceNonXmlChars(false);
		configureAndStartPipe();

		String expectedOutput = "<address><line><![CDATA[this is an example]]></line>\n"
				+ "<line><![CDATA[im in cdata]]></line></address>";

		PipeRunResult res = doPipe(pipe, "this is an example\nim in cdata", session);
		MatchUtils.assertXmlEquals(expectedOutput, res.getResult().asString());
	}

	@Test
	public void testCdataWithoutReplacingNonXMLChars() throws Exception {
		pipe.setXmlTag("address");
		pipe.setUseCdataSection(true);
		pipe.setReplaceNonXmlChars(false);
		configureAndStartPipe();

		String expectedOutput = "<address><![CDATA[this is an example\nim in cdata]]></address>";

		PipeRunResult res = doPipe(pipe, "this is an example\nim in cdata", session);
		MatchUtils.assertXmlEquals(expectedOutput, res.getResult().asString());
	}

	@Test
	public void testCdataWithReplacingNonXMLChars() throws Exception {
		pipe.setXmlTag("address");
		pipe.setUseCdataSection(true);
		configureAndStartPipe();

		String expectedOutput = "<address><![CDATA[this is an¿ example\nim in cdata]]></address>";

		PipeRunResult res = doPipe(pipe, "this is an\b example\nim in cdata", session);
		MatchUtils.assertXmlEquals(expectedOutput, res.getResult().asString());
	}

	@Test
	public void testEmptyXmlTag() throws Exception {
		ConfigurationException e = assertThrows(ConfigurationException.class, this::configureAndStartPipe);
		assertThat(e.getMessage(), Matchers.containsString("Attribute [xmlTag] must be specified"));
	}

	@Test
	public void testEmptyInput() throws Exception {
		pipe.setXmlTag("tests");
		pipe.setUseCdataSection(false);
		pipe.setSplitLines(true);
		configureAndStartPipe();

		PipeRunResult res = doPipe(Message.asMessage(""));
		MatchUtils.assertXmlEquals("<tests/>", res.getResult().asString());
	}

	@Test
	public void testEmptyInputInCDATA() throws Exception {
		pipe.setXmlTag("tests");
		configureAndStartPipe();

		PipeRunResult res = doPipe(Message.asMessage(""));
		assertEquals("<tests><![CDATA[]]></tests>", res.getResult().asString());
	}

	@Test
	public void testNullInput() throws Exception {
		pipe.setXmlTag("tests");
		pipe.setSplitLines(true);
		pipe.setUseCdataSection(false);
		configureAndStartPipe();

		PipeRunResult res = doPipe(Message.nullMessage());
		assertEquals("<tests nil=\"true\" />", res.getResult().asString());
	}
}