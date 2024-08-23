package org.frankframework.pipes;

import static org.frankframework.testutil.MatchUtils.assertXmlEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URL;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.PipeStartException;
import org.frankframework.pipes.CsvParserPipe.HeaderCase;
import org.frankframework.stream.UrlMessage;
import org.frankframework.testutil.TestFileUtils;

public class CsvParserPipeTest extends PipeTestBase<CsvParserPipe> {

	@Override
	public CsvParserPipe createPipe() {
		return new CsvParserPipe();
	}

	@Test
	public void testConfigureAndStart() throws ConfigurationException, PipeStartException {
		configureAndStartPipe();
	}

	@Test
	public void testBasic() throws Exception {
		configureAndStartPipe();
		String csv ="a,b,c\n1,2,3\nx,\"y,y\"";
		String expected="<csv><record><a>1</a><b>2</b><c>3</c></record><record><a>x</a><b>y,y</b></record></csv>";

		PipeRunResult prr = doPipe(csv);
		assertXmlEquals(expected,prr.getResult().asString());
	}

	@Test
	public void testNoHeaderNoFieldNames() {
		pipe.setFileContainsHeader(false);

		ConfigurationException e = assertThrows(ConfigurationException.class, this::configurePipe);
		assertThat(e.getMessage(), Matchers.endsWith("No fieldNames specified, and fileContainsHeader=false"));
	}

	@Test
	public void test2CharFieldSeparator() {
		pipe.setFieldSeparator("; ");

		ConfigurationException e = assertThrows(ConfigurationException.class, this::configurePipe);
		assertThat(e.getMessage(), Matchers.endsWith("], can only be a single character"));
	}

	@Test
	public void testFieldSeparatorAndControlCodes() {
		pipe.setFieldSeparator(";");
		pipe.setUseControlCodes(true);

		ConfigurationException e = assertThrows(ConfigurationException.class, this::configurePipe);
		assertThat(e.getMessage(), Matchers.endsWith("cannot use fieldSeparator in combination with useControlCodes"));
	}

	@Test
	public void testFieldNames() throws Exception {
		pipe.setFieldNames("p,q,r");
		configureAndStartPipe();
		String csv ="a,b,c\n1,2,3\nx,\"y,y\"";
		String expected="<csv><record><p>a</p><q>b</q><r>c</r></record><record><p>1</p><q>2</q><r>3</r></record><record><p>x</p><q>y,y</q></record></csv>";

		PipeRunResult prr = doPipe(csv);
		assertXmlEquals(expected,prr.getResult().asString());
	}

	@ParameterizedTest
	@ValueSource(strings = {"naam,woonplaats+postcode,land", "naam,woonplaats postcode,land"})
	public void testFieldNamesWithInvalidElementName(String fieldNames) throws Exception {
		pipe.setFieldNames(fieldNames);
		configureAndStartPipe();
		String csv = "Frank,Rotterdam+3014GT,Frankland\nFrank2,Rotterdam+1234AB,Nederland";
		String expected = "<csv><record><naam>Frank</naam><woonplaats_postcode>Rotterdam+3014GT</woonplaats_postcode><land>Frankland</land></record><record><naam>Frank2</naam><woonplaats_postcode>Rotterdam+1234AB</woonplaats_postcode><land>Nederland</land></record></csv>";

		PipeRunResult prr = doPipe(csv);
		assertXmlEquals(expected, prr.getResult().asString());
	}

	@Test
	public void testFieldNamesAndSkipHeader() throws Exception {
		pipe.setFieldNames("p,q,r");
		pipe.setFileContainsHeader(true);
		configureAndStartPipe();
		String csv ="a,b,c\n1,2,3\nx,\"y,y\"";
		String expected="<csv><record><p>1</p><q>2</q><r>3</r></record><record><p>x</p><q>y,y</q></record></csv>";

		PipeRunResult prr = doPipe(csv);
		assertXmlEquals(expected,prr.getResult().asString());
	}

	@Test
	public void testFieldSeparator() throws Exception {
		pipe.setFieldSeparator("|");
		configureAndStartPipe();
		String csv ="a|b|c\n1|2|3\nx|\"y|y\"";
		String expected="<csv><record><a>1</a><b>2</b><c>3</c></record><record><a>x</a><b>y|y</b></record></csv>";

		PipeRunResult prr = doPipe(csv);
		assertXmlEquals(expected,prr.getResult().asString());
	}

	@Test
	public void testHeaderCaseUpper() throws Exception {
		pipe.setFieldNames("p,q,r");
		pipe.setHeaderCase(HeaderCase.UPPERCASE);
		pipe.setFileContainsHeader(true);
		configureAndStartPipe();
		String csv ="a,b,c\n1,2,3\nx,\"y,y\"";
		String expected="<csv><record><P>1</P><Q>2</Q><R>3</R></record><record><P>x</P><Q>y,y</Q></record></csv>";

		PipeRunResult prr = doPipe(csv);
		assertXmlEquals(expected,prr.getResult().asString());
	}

	@Test
	public void testHeaderCaseLower() throws Exception {
		pipe.setFieldNames("P,q,R");
		pipe.setHeaderCase(HeaderCase.LOWERCASE);
		pipe.setFileContainsHeader(true);
		configureAndStartPipe();
		String csv ="a,b,c\n1,2,3\nx,\"y,y\"";
		String expected="<csv><record><p>1</p><q>2</q><r>3</r></record><record><p>x</p><q>y,y</q></record></csv>";

		PipeRunResult prr = doPipe(csv);
		assertXmlEquals(expected,prr.getResult().asString());
	}

	@Test
	public void testHeaderCaseNotSpecified() throws Exception {
		pipe.setFieldNames("p,q,R");
		pipe.setFileContainsHeader(true);
		configureAndStartPipe();
		String csv ="a,b,c\n1,2,3\nx,\"y,y\"";
		String expected="<csv><record><p>1</p><q>2</q><R>3</R></record><record><p>x</p><q>y,y</q></record></csv>";

		PipeRunResult prr = doPipe(csv);
		assertXmlEquals(expected,prr.getResult().asString());
	}

	@Test
	public void testHeaderFromFileCaseLower() throws Exception {
		pipe.setHeaderCase(HeaderCase.LOWERCASE);
		pipe.setFileContainsHeader(true);
		configureAndStartPipe();
		String csv ="A,b,c\n1,2,3\nx,\"y,y\"";
		String expected="<csv><record><a>1</a><b>2</b><c>3</c></record><record><a>x</a><b>y,y</b></record></csv>";

		PipeRunResult prr = doPipe(csv);
		assertXmlEquals(expected,prr.getResult().asString());
	}

	@Test
	public void testNoHeaderFromFileCaseLower() throws Exception {
		pipe.setHeaderCase(HeaderCase.LOWERCASE);
		configureAndStartPipe();
		String csv ="A,b,c\n1,2,3\nx,\"y,y\"";
		String expected="<csv><record><a>1</a><b>2</b><c>3</c></record><record><a>x</a><b>y,y</b></record></csv>";

		PipeRunResult prr = doPipe(csv);
		assertXmlEquals(expected,prr.getResult().asString());
	}

	@Test
	public void testUseFieldNameAsHeaderFileContainesHeaderFalse() throws Exception {
		pipe.setFieldNames("p,q,r");
		pipe.setFileContainsHeader(false);
		configureAndStartPipe();
		String csv ="a,b,c\n1,2,3\nx,\"y,y\"";
		String expected="<csv><record><p>a</p><q>b</q><r>c</r></record><record><p>1</p><q>2</q><r>3</r></record><record><p>x</p><q>y,y</q></record></csv>";

		PipeRunResult prr = doPipe(csv);
		assertXmlEquals(expected,prr.getResult().asString());
	}

	@Test
	public void testCsvFile() throws Exception {
		pipe.setFieldSeparator(";");
		pipe.setPrettyPrint(true);
		configureAndStartPipe();

		URL input = TestFileUtils.getTestFileURL("/CsvParser/codes.csv");
		assertNotNull(input, "cannot find test file");

		PipeRunResult prr = doPipe(new UrlMessage(input));
		String expected = TestFileUtils.getTestFile("/CsvParser/codes.xml");
		assertXmlEquals(expected, prr.getResult().asString());
	}

	@Test
	public void test0x1fCsvFile() throws Exception {
		pipe.setUseControlCodes(true);
		pipe.setPrettyPrint(true);
		configureAndStartPipe();

		URL input = TestFileUtils.getTestFileURL("/CsvParser/0x1f_example.csv");
		assertNotNull(input, "cannot find test file");

		PipeRunResult prr = doPipe(new UrlMessage(input));
		String expected = TestFileUtils.getTestFile("/CsvParser/0x1f_example.xml");
		assertXmlEquals(expected, prr.getResult().asString());
	}
}
