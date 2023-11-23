package nl.nn.adapterframework.pipes;

import static nl.nn.adapterframework.testutil.MatchUtils.assertXmlEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

import org.hamcrest.Matchers;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.pipes.CsvParserPipe.HeaderCase;

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
	public void testFieldNames() throws Exception {
		pipe.setFieldNames("p,q,r");
		configureAndStartPipe();
		String csv ="a,b,c\n1,2,3\nx,\"y,y\"";
		String expected="<csv><record><p>a</p><q>b</q><r>c</r></record><record><p>1</p><q>2</q><r>3</r></record><record><p>x</p><q>y,y</q></record></csv>";

		PipeRunResult prr = doPipe(csv);
		assertXmlEquals(expected,prr.getResult().asString());
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
}
