package org.frankframework.pipes;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;
import org.frankframework.testutil.MatchUtils;

public class Text2XmlPipeTest extends PipeTestBase<Text2XmlPipe> {

	@Override
	public Text2XmlPipe createPipe() {
		return new Text2XmlPipe();
	}

	@Test
	void testSuccessCDataAndReplaceNonXMLSplitLines() throws Exception {
		pipe.setXmlTag("address");
		pipe.setSplitLines(true);
		configureAndStartPipe();

		String expectedOutput = """
					<address>\
					<line><![CDATA[this is an example]]></line>
					<line><![CDATA[im in cdata]]></line>\
					</address>\
					""";

		PipeRunResult res = doPipe(pipe, "this is an example\nim in cdata", session);
		MatchUtils.assertXmlEquals(expectedOutput, res.getResult().asString());
	}

	@Test
	void testSuccessCDataAndXMLDeclaration() throws Exception {
		pipe.setXmlTag("address");
		pipe.setSplitLines(false);
		pipe.setUseCdataSection(false);
		configureAndStartPipe();

		String expectedOutput = "<address>this is an example\n" + "im not in cdata</address>";

		PipeRunResult res = doPipe(pipe, "this is an example\nim not in cdata", session);
		assertEquals(expectedOutput, res.getResult().asString());
	}

	@Test
	void testWithoutXmlDeclarationAndCDATA() throws Exception {
		pipe.setXmlTag("address");
		pipe.setSplitLines(false);
		pipe.setUseCdataSection(false);
		configureAndStartPipe();

		String expectedOutput = "<address>this is an example\nim not in cdata</address>";

		PipeRunResult res = doPipe(pipe, "this is an example\nim not in cdata", session);
		assertEquals(expectedOutput, res.getResult().asString());
	}

	@Test
	void testSplitLinesWithoutCDATA() throws Exception {
		pipe.setXmlTag("address");
		pipe.setSplitLines(true);
		pipe.setUseCdataSection(false);
		configureAndStartPipe();

		String expectedOutput = """
					<address>\
					<line>this is an example</line>
					<line>im not in cdata</line>\
					</address>\
					""";

		PipeRunResult res = doPipe(pipe, "this is an example\nim not in cdata", session);
		MatchUtils.assertXmlEquals(expectedOutput, res.getResult().asString());
	}

	@Test
	void testXmlInput() throws Exception {
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
	void testMultilineXmlInput() throws Exception {
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
	void testInvalidMultilineXmlInput() throws Exception {
		pipe.setXmlTag("root");
		pipe.setUseCdataSection(false);
		configureAndStartPipe();

		PipeRunException e = assertThrows(PipeRunException.class, ()->doPipe(pipe, "<invalid>\n<xml>\n<input>\n</invalid>", session));
		assertThat(e.getMessage(), containsString("The element type \"input\" must be terminated by the matching end-tag \"</input>\""));
	}

	@Test
	void testInvalidXmlInput() throws Exception {
		pipe.setXmlTag("root");
		pipe.setUseCdataSection(false);
		configureAndStartPipe();

		PipeRunException e = assertThrows(PipeRunException.class, ()->doPipe(pipe, "<invalid><xml><input></invalid>", session));
		assertThat(e.getMessage(), containsString("The element type \"input\" must be terminated by the matching end-tag \"</input>\""));
	}

	@Test
	void testInvalidXmlCharsReplacedWithoutCDATA() throws Exception {
		pipe.setXmlTag("address");
		pipe.setUseCdataSection(false);
		configureAndStartPipe();

		String expectedOutput = "<address>¿</address>";

		PipeRunResult res = doPipe(pipe, "\b", session);
		assertEquals(expectedOutput, res.getResult().asString());
	}

	@Test
	void testEmptyInputAndNoReplacementForInvalidXmlChars() throws Exception {
		pipe.setXmlTag("address");
		pipe.setUseCdataSection(false);
		pipe.setReplaceNonXmlChars(false);
		configureAndStartPipe();

		String expectedOutput = "<address/>";

		PipeRunResult res = doPipe(pipe, "", session);
		MatchUtils.assertXmlEquals(expectedOutput, res.getResult().asString());
	}

	@Test
	void testSuccessWithoutAdditionalProperties() throws Exception {
		pipe.setXmlTag("address");
		configureAndStartPipe();

		PipeRunResult res = doPipe(pipe, "this will be in cdata tag\b", session);
		assertEquals("<address><![CDATA[this will be in cdata tag¿]]></address>", res.getResult().asString());
	}

	@Test
	void testSuccessSplitWithoutReplacingNonXMLChars() throws Exception {
		pipe.setXmlTag("address");
		pipe.setSplitLines(true);
		pipe.setUseCdataSection(true);
		pipe.setReplaceNonXmlChars(false);
		configureAndStartPipe();

		String expectedOutput = """
				<address><line><![CDATA[this is an example]]></line>
				<line><![CDATA[im in cdata]]></line></address>\
				""";

		PipeRunResult res = doPipe(pipe, "this is an example\nim in cdata", session);
		MatchUtils.assertXmlEquals(expectedOutput, res.getResult().asString());
	}

	@Test
	void testCdataWithoutReplacingNonXMLChars() throws Exception {
		pipe.setXmlTag("address");
		pipe.setUseCdataSection(true);
		pipe.setReplaceNonXmlChars(false);
		configureAndStartPipe();

		String expectedOutput = "<address><![CDATA[this is an example\nim in cdata]]></address>";

		PipeRunResult res = doPipe(pipe, "this is an example\nim in cdata", session);
		MatchUtils.assertXmlEquals(expectedOutput, res.getResult().asString());
	}

	@Test
	void testCdataWithReplacingNonXMLChars() throws Exception {
		pipe.setXmlTag("address");
		pipe.setUseCdataSection(true);
		configureAndStartPipe();

		String expectedOutput = "<address><![CDATA[this is an¿ example\nim in cdata]]></address>";

		PipeRunResult res = doPipe(pipe, "this is an\b example\nim in cdata", session);
		MatchUtils.assertXmlEquals(expectedOutput, res.getResult().asString());
	}

	@Test
	void testEmptyXmlTag() {
		ConfigurationException e = assertThrows(ConfigurationException.class, this::configureAndStartPipe);
		assertThat(e.getMessage(), Matchers.containsString("Attribute [xmlTag] must be specified"));
	}

	@Test
	void testEmptyInput() throws Exception {
		pipe.setXmlTag("tests");
		pipe.setUseCdataSection(false);
		pipe.setSplitLines(true);
		configureAndStartPipe();

		PipeRunResult res = doPipe(new Message(""));
		MatchUtils.assertXmlEquals("<tests/>", res.getResult().asString());
	}

	@Test
	void testEmptyInputInCDATA() throws Exception {
		pipe.setXmlTag("tests");
		configureAndStartPipe();

		PipeRunResult res = doPipe(new Message(""));
		assertEquals("<tests><![CDATA[]]></tests>", res.getResult().asString());
	}

	@Test
	void testNullInput() throws Exception {
		pipe.setXmlTag("tests");
		pipe.setSplitLines(true);
		pipe.setUseCdataSection(false);
		configureAndStartPipe();

		PipeRunResult res = doPipe(Message.nullMessage());
		assertEquals("<tests nil=\"true\" />", res.getResult().asString());
	}
}
