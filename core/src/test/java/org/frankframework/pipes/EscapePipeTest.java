package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.pipes.EscapePipe.Direction;
import org.frankframework.testutil.TestAssertions;

public class EscapePipeTest extends PipeTestBase<EscapePipe> {

	@Override
	public EscapePipe createPipe() {
		return new EscapePipe();
	}

	@Test
	public void testNullDirectionGiven() {
		pipe.setDirection(null);
		assertThrows(ConfigurationException.class, () -> pipe.configure());
	}

	@Test
	public void testNoSubstringEnd() {
		pipe.setSubstringStart("Substring");
		assertThrows(ConfigurationException.class, () -> pipe.configure());
	}

	@Test
	public void testNoSubstringStart() {
		pipe.setSubstringEnd("Substring");
		assertThrows(ConfigurationException.class, () -> pipe.configure());
	}

	@Test
	public void testMultiLineXmlInput() throws Exception {
		configureAndStartPipe();

		PipeRunResult result = doPipe(getResource("multi-line.xml"));
		String expected = getResource("multi-line.escaped").asString();
		TestAssertions.assertEqualsIgnoreCRLF(expected, result.getResult().asString());
	}

	@Test
	public void testPartialEscapedMultiLineXmlInput() throws Exception { //double escapes
		configureAndStartPipe();

		PipeRunResult result = doPipe(getResource("multi-line-partial-escaped.xml"));
		String expected = getResource("multi-line-partial-escaped.result").asString();
		TestAssertions.assertEqualsIgnoreCRLF(expected, result.getResult().asString());
	}

	@Test
	public void testPartialEscapedMultiLineXmlInputMultiOccurences() throws Exception {
		pipe.setSubstringStart("<message>");
		pipe.setSubstringEnd("</message>");
		configureAndStartPipe();

		PipeRunResult result = doPipe(getResource("multi-line-partial-escaped-multi-occurences.xml"));
		String expected = getResource("multi-line-partial-escaped-multi-occurences.result").asString();
		TestAssertions.assertEqualsIgnoreCRLF(expected, result.getResult().asString());
	}

	@Test
	public void testPartialEscapedMultiLineXmlInputMultiOccurencesWithDoubleOpeningTagsConfigured() throws Exception {
		pipe.setSubstringStart("<message>");
		pipe.setSubstringEnd("</message>");
		configureAndStartPipe();

		PipeRunResult result = doPipe(getResource("double-opening-tag-configured.xml"));
		String expected = getResource("double-opening-tag-configured.result").asString();
		TestAssertions.assertEqualsIgnoreCRLF(expected, result.getResult().asString());
	}

	@Test
	public void testPartialEscapedMultiLineXmlInputMultiOccurencesMissingClosingTag() throws Exception {
		pipe.setSubstringStart("<message>");
		pipe.setSubstringEnd("</message>");
		configureAndStartPipe();

		PipeRunResult result = doPipe(getResource("no-closing-tag-configured.xml"));
		String expected = getResource("no-closing-tag-configured.result").asString();
		TestAssertions.assertEqualsIgnoreCRLF(expected, result.getResult().asString());
	}

	@Test
	public void nothingGetsEscapedWhenNoSubstringEndConfigured() throws Exception {
		pipe.setSubstringStart("<message>");
		pipe.setSubstringEnd("");

		assertThrows(ConfigurationException.class, pipe::configure);
	}

	@Test
	public void nothingGetsEscapedWhenNoSubstringStartConfigured() throws Exception {
		pipe.setSubstringStart("");
		pipe.setSubstringEnd("</message>");

		assertThrows(ConfigurationException.class, pipe::configure);
	}

	@Test
	public void testPartialEscapedMultiLineXmlInputSubString() throws Exception { //double escapes
		pipe.setSubstringStart("<multi>");
		pipe.setSubstringEnd("</multi>");
		pipe.setEncodeSubstring(true);
		configureAndStartPipe();

		PipeRunResult result = doPipe(getResource("multi-line-partial-escaped.result2"));
		String expected = getResource("multi-line-partial-escaped.xml").asString();
		TestAssertions.assertEqualsIgnoreCRLF(expected, result.getResult().asString());
	}

	@Test
	public void testDecodeEscapedMultiLineXmlInputWithSubString() throws Exception {
		pipe.setSubstringStart("<test>");
		pipe.setSubstringEnd("</test>");
		pipe.setDirection(Direction.DECODE);
		configureAndStartPipe();

		PipeRunResult result = doPipe(getResource("multi-line-partial-escaped.xml"));
		String expected = getResource("multi-line.xml").asString();
		TestAssertions.assertEqualsIgnoreCRLF(expected, result.getResult().asString());
	}

	@Test //input-output is unchanged / substring is not found, nothing is being de/en-coded
	public void testDecodeEscapedMultiLineXmlInputWithEncodedSubString() throws Exception {
		pipe.setSubstringStart("<test>");
		pipe.setSubstringEnd("</test>");
		pipe.setEncodeSubstring(true);
		pipe.setDirection(Direction.DECODE);
		configureAndStartPipe();

		PipeRunResult result = doPipe(getResource("multi-line-partial-escaped.xml"));
		String expected = getResource("multi-line-partial-escaped.xml").asString();
		TestAssertions.assertEqualsIgnoreCRLF(expected, result.getResult().asString());
	}

	@Test
	public void testDecodeEscapedMultiLineXmlInputWithEncodedSubString2() throws Exception {
		pipe.setSubstringStart("<multi>");
		pipe.setSubstringEnd("</multi>");
		pipe.setEncodeSubstring(true);
		pipe.setDirection(Direction.DECODE);
		configureAndStartPipe();

		PipeRunResult result = doPipe(getResource("multi-line-partial-escaped.xml"));
		String expected = getResource("multi-line-partial-escaped.result2").asString();
		TestAssertions.assertEqualsIgnoreCRLF(expected, result.getResult().asString());
	}

	@Test
	public void testDecodeMultiLineXmlInput() throws Exception {
		pipe.setDirection(Direction.DECODE);
		configureAndStartPipe();

		PipeRunResult result = doPipe(getResource("multi-line.escaped"));
		String expected = getResource("multi-line.xml").asString();
		TestAssertions.assertEqualsIgnoreCRLF(expected, result.getResult().asString());
	}

	@Test
	public void testDecodePartialEncodedMultiLineXmlInput() throws Exception {
		pipe.setDirection(Direction.DECODE);
		configureAndStartPipe();

		PipeRunResult result = doPipe(getResource("multi-line-partial-escaped.xml"));
		String expected = getResource("multi-line.xml").asString();
		TestAssertions.assertEqualsIgnoreCRLF(expected, result.getResult().asString());
	}

	@Test //input-output is unchanged / substring is not found, nothing is being de/en-coded
	public void testDecodePartialEncodedMultiLineXmlInputWithSubString() throws Exception {
		pipe.setSubstringStart("<root>");
		pipe.setSubstringEnd("</root>");
		pipe.setDirection(Direction.DECODE);
		configureAndStartPipe();

		PipeRunResult result = doPipe(getResource("multi-line-partial-escaped.xml"));
		String expected = getResource("multi-line-partial-escaped.xml").asString();
		TestAssertions.assertEqualsIgnoreCRLF(expected, result.getResult().asString());
	}

	@Test //input-output is unchanged / substring is not found, nothing is being de/en-coded
	public void testDecodePartialDoubleEncodedMultiLineXmlInput() throws Exception {
		pipe.setDirection(Direction.DECODE);
		configureAndStartPipe();

		PipeRunResult result = doPipe(getResource("multi-line-partial-escaped.result"));
		String expected = getResource("multi-line-partial-escaped.xml").asString();
		TestAssertions.assertEqualsIgnoreCRLF(expected, result.getResult().asString());
	}

	@Test
	public void testDecodePartialEncodedMultiLineXmlInputWithEncodedSubString() throws Exception {
		pipe.setSubstringStart("<multi>");
		pipe.setSubstringEnd("</multi>");
		pipe.setEncodeSubstring(true);
		pipe.setDirection(Direction.DECODE);
		configureAndStartPipe();

		PipeRunResult result = doPipe(getResource("multi-line-partial-escaped.xml"));
		String expected = getResource("multi-line-partial-escaped.result2").asString();
		TestAssertions.assertEqualsIgnoreCRLF(expected, result.getResult().asString());
	}

	@Test
	public void testCdata2Text() throws Exception {
		pipe.setDirection(Direction.CDATA2TEXT);
		configureAndStartPipe();

		PipeRunResult result = doPipe(getResource("multi-line-with-cdata.xml"));
		String expected = getResource("multi-line-partial-escaped.xml").asString();
		TestAssertions.assertEqualsIgnoreCRLF(expected, result.getResult().asString());
	}

	@Test
	public void testCdata2TextWithoutCdataInput() throws Exception {
		pipe.setDirection(Direction.CDATA2TEXT);
		configureAndStartPipe();

		PipeRunResult result = doPipe(getResource("multi-line.xml"));
		String expected = getResource("multi-line.xml").asString();
		TestAssertions.assertEqualsIgnoreRNTSpace(expected, result.getResult().asString());
	}

	@Test //The substring is the cdata part, without root, which returns null
	public void testCdata2TextSubStringOnRoot() throws Exception {
		pipe.setSubstringStart("<test>");
		pipe.setSubstringEnd("</test>");
		pipe.setDirection(Direction.CDATA2TEXT);
		configureAndStartPipe();

		PipeRunResult result = doPipe(getResource("multi-line-with-cdata.xml"));
		TestAssertions.assertEqualsIgnoreCRLF("<test>null</test>", result.getResult().asString());
	}


	@Test
	public void testCdata2TextSubString() throws Exception {
		pipe.setSubstringStart("<test>");
		pipe.setSubstringEnd("</test>");
		pipe.setDirection(Direction.CDATA2TEXT);
		configureAndStartPipe();

		PipeRunResult result = doPipe(getResource("multi-line-with-cdata2.xml"));
		String expected = getResource("multi-line-partial-escaped.result3").asString();
		TestAssertions.assertEqualsIgnoreRNTSpace(expected, result.getResult().asString());
	}
}
