package nl.nn.adapterframework.pipes;

import static nl.nn.adapterframework.testutil.MatchUtils.assertXmlEquals;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;

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
	public void testNoHeaderNoFieldNames() throws Exception {
		pipe.setFileContainsHeader(false);
		exception.expectMessage("No fieldNames specified, and fileContainsHeader=false");
		configurePipe();
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
	

}
