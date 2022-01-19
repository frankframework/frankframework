package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.Message;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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

		String expectedOutput = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
					+ "<address>"
					+ "<line><![CDATA[this is an example]]></line>"
					+ "<line><![CDATA[im in cdata]]></line>"
					+ "</address>";

		PipeRunResult res = doPipe(pipe, "this is an example\nim in cdata", session);
		assertEquals(expectedOutput, res.getResult().asString());
	}

	@Test
	public void testSuccessCDataAndXMLDeclaration() throws Exception {
		pipe.setXmlTag("address");
		pipe.setSplitLines(false);
		pipe.setUseCdataSection(false);
		configureAndStartPipe();
		
		String expectedOutput = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<address>this is an example\n" + "im not in cdata</address>";

		PipeRunResult res = doPipe(pipe, "this is an example\nim not in cdata", session);
		assertEquals(expectedOutput, res.getResult().asString());
	}

	@Test
	public void testWithoutXmlDeclarationAndCDATA() throws Exception {
		pipe.setXmlTag("address");
		pipe.setSplitLines(false);
		pipe.setUseCdataSection(false);
		pipe.setIncludeXmlDeclaration(false);
		configureAndStartPipe();

		String expectedOutput = "<address>this is an example\n" + "im not in cdata</address>";

		PipeRunResult res = doPipe(pipe, "this is an example\nim not in cdata", session);
		assertEquals(expectedOutput, res.getResult().asString());
	}

	@Test
	public void testSplitLinesWithoutCDATA() throws Exception {
		pipe.setXmlTag("address");
		pipe.setSplitLines(true);
		pipe.setUseCdataSection(false);
		configureAndStartPipe();

		String expectedOutput = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
					+ "<address>"
					+ "<line>this is an example</line>"
					+ "<line>im not in cdata</line>"
					+ "</address>";

		PipeRunResult res = doPipe(pipe, "this is an example\nim not in cdata", session);
		assertEquals(expectedOutput, res.getResult().asString());
	}

	@Test
	public void testInvalidXmlCharsReplacedWithoutCDATA() throws Exception {
		pipe.setXmlTag("address");
		pipe.setIncludeXmlDeclaration(false);
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

		String expectedOutput = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<address></address>";

		PipeRunResult res = doPipe(pipe, "", session);
		assertEquals(expectedOutput, res.getResult().asString());
	}

	@Test
	public void testSuccessWithoutAdditionalProperties() throws Exception {
		pipe.setXmlTag("address");
		configureAndStartPipe();

		PipeRunResult res = doPipe(pipe, "this will be in cdata tag\b", session);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><address><![CDATA[this will be in cdata tag¿]]></address>", res.getResult().asString());
	}

	@Test
	public void testSuccessSplitWithoutReplacingNonXMLChars() throws Exception {
		pipe.setXmlTag("address");
		pipe.setSplitLines(true);
		pipe.setUseCdataSection(true);
		pipe.setIncludeXmlDeclaration(true);
		pipe.setReplaceNonXmlChars(false);
		configureAndStartPipe();
		
		String expectedOutput = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<address><line><![CDATA[this is an example]]></line>"
				+ "<line><![CDATA[im in cdata]]></line></address>";

		PipeRunResult res = doPipe(pipe, "this is an example\nim in cdata", session);
		assertEquals(expectedOutput, res.getResult().asString());
	}

	@Test
	public void testEmptyXmlTag() throws Exception {
		exception.expect(ConfigurationException.class);
		exception.expectMessage("Attribute [xmlTag] must be specified");
		configureAndStartPipe();
	}

	@Test
	public void testEmptyInput() throws Exception {
		pipe.setIncludeXmlDeclaration(false);
		pipe.setXmlTag("tests");
		pipe.setUseCdataSection(false);
		pipe.setSplitLines(true);
		configureAndStartPipe();

		PipeRunResult res = doPipe(Message.asMessage(""));
		assertEquals("<tests></tests>", res.getResult().asString());
	}

	@Test
	public void testEmptyInputInCDATA() throws Exception {
		pipe.setIncludeXmlDeclaration(false);
		pipe.setXmlTag("tests");
		configureAndStartPipe();

		PipeRunResult res = doPipe(Message.asMessage(""));
		assertEquals("<tests><![CDATA[]]></tests>", res.getResult().asString());
	}

	@Test
	public void testNullInput() throws Exception {
		pipe.setIncludeXmlDeclaration(false);
		pipe.setXmlTag("tests");
		pipe.setSplitLines(true);
		pipe.setUseCdataSection(false);
		configureAndStartPipe();

		PipeRunResult res = doPipe(Message.nullMessage());
		assertEquals("<tests nil=\"true\" />", res.getResult().asString());
	}

	@Test
	public void testNullInputInCDATA() throws Exception {
		pipe.setIncludeXmlDeclaration(false);
		pipe.setXmlTag("tests");
		configureAndStartPipe();

		PipeRunResult res = doPipe(Message.nullMessage());
		assertEquals("<tests><![CDATA[null]]></tests>", res.getResult().asString());
	}

}
