package nl.nn.adapterframework.pipes;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.EscapePipe.Direction;
import nl.nn.adapterframework.testutil.TestAssertions;

public class EscapePipeTest extends PipeTestBase<EscapePipe> {

	@Override
	public EscapePipe createPipe() {
		return new EscapePipe();
	}

	@Test(expected = ConfigurationException.class)
	public void testNullDirectionGiven() throws Exception {
		pipe.setDirection(null);
		pipe.configure();
	}

	@Test(expected = ConfigurationException.class)
	public void testNoSubstringEnd() throws Exception {
		pipe.setSubstringStart("Substring");
		pipe.configure();
	}

	@Test(expected = ConfigurationException.class)
	public void testNoSubstringStart() throws Exception {
		pipe.setSubstringEnd("Substring");
		pipe.configure();
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