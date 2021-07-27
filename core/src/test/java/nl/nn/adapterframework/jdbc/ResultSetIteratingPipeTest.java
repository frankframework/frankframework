package nl.nn.adapterframework.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.senders.EchoSender;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.StreamUtil;

public class ResultSetIteratingPipeTest extends JdbcEnabledPipeTestBase<ResultSetIteratingPipe> {

	@Override
	public ResultSetIteratingPipe createPipe() {
		return new ResultSetIteratingPipe();
	}

	private void readSqlInsertFile(URL url) throws Exception {
		Reader reader = StreamUtil.getCharsetDetectingInputStreamReader(url.openStream());
		BufferedReader buf = new BufferedReader(reader);
		String line = buf.readLine();
		int i = 1234;
		while (line != null) {
			insert(i++, line);
			line = buf.readLine();
		}
	}

	private void insert(int key, String value) throws JdbcException {
		JdbcUtil.executeStatement(connection, String.format("INSERT INTO TEMP (TKEY, TVARCHAR, TINT) VALUES ('%d', '%s', '0')", key, value));
	}

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();

		URL url = TestFileUtils.getTestFileURL("/Pipes/ResultSetIteratingPipe/sqlInserts.txt");
		assertNotNull(url);
		readSqlInsertFile(url);
	}

	@Test
	public void testWithStylesheetNoCollectResultsAndIgnoreExceptions() throws Exception {
		pipe.setQuery("SELECT TKEY, TVARCHAR FROM TEMP ORDER BY TKEY");
		pipe.setStyleSheetName("Pipes/ResultSetIteratingPipe/CreateMessage.xsl");
		pipe.setCollectResults(false);
		pipe.setIgnoreExceptions(true);
		pipe.setDatasourceName(getDataSourceName());

		ResultCollectingSender sender = new ResultCollectingSender();
		pipe.setSender(sender);

		configurePipe();
		pipe.start();

		PipeRunResult result = doPipe("since query attribute is set, this should be ignored");
		assertEquals("<results count=\"10\"/>", result.getResult().asString());
		String xmlResult = TestFileUtils.getTestFile("/Pipes/ResultSetIteratingPipe/result.xml");
		TestAssertions.assertEqualsIgnoreWhitespaces(xmlResult, sender.collectResults());
	}

	@Test
	public void testWithStylesheetNoCollectResultsAndIgnoreExceptionsParallel() throws Exception {
		pipe.setQuery("SELECT TKEY, TVARCHAR FROM TEMP ORDER BY TKEY");
		pipe.setStyleSheetName("Pipes/ResultSetIteratingPipe/CreateMessage.xsl");
		pipe.setCollectResults(false);
		pipe.setParallel(true);
		pipe.setIgnoreExceptions(true);
		pipe.setDatasourceName(getDataSourceName());
		pipe.setTaskExecutor(new SimpleAsyncTaskExecutor());

		ResultCollectingSender sender = new ResultCollectingSender();
		pipe.setSender(sender);

		configurePipe();
		pipe.start();

		PipeRunResult result = doPipe("since query attribute is set, this should be ignored");
		assertEquals("<results count=\"10\"/>", result.getResult().asString());
		String xmlResult = TestFileUtils.getTestFile("/Pipes/ResultSetIteratingPipe/result.xml");
		TestAssertions.assertEqualsIgnoreWhitespaces(xmlResult, sender.collectResults());
	}

	@Test
	public void testWithStylesheetNoCollectResultsAndIgnoreExceptionsWithUpdateInSameTable() throws Exception {
		pipe.setQuery("SELECT TKEY, TVARCHAR FROM TEMP ORDER BY TKEY");
		pipe.setStyleSheetName("Pipes/ResultSetIteratingPipe/CreateMessage.xsl");
		pipe.setCollectResults(false);
		pipe.setIgnoreExceptions(true);
		pipe.setDatasourceName(getDataSourceName());

		FixedQuerySender sender = new FixedQuerySender();
		sender.setQuery("UPDATE TEMP SET TINT = '4', TDATE = CURRENT_TIMESTAMP WHERE TKEY = ?");
		Parameter param = new Parameter();
		param.setName("ID");
		param.setXpathExpression("result/id");
		sender.addParameter(param);
		sender.setDatasourceName(getDataSourceName());
		autowireByType(sender);
		pipe.setSender(sender);

		configurePipe();
		pipe.start();

		PipeRunResult result = doPipe("since query attribute is set, this should be ignored");
		assertEquals("<results count=\"10\"/>", result.getResult().asString());
		String jdbcResult = JdbcUtil.executeStringQuery(connection, "SELECT COUNT('TKEY') FROM TEMP WHERE TINT = '4'");
		assertEquals("10", jdbcResult);
	}

	private static class ResultCollectingSender extends EchoSender {
		public List<Message> data = new LinkedList<>();
		@Override
		public Message sendMessage(Message message, IPipeLineSession session) throws SenderException, TimeOutException {
			data.add(message);
			return super.sendMessage(message, session);
		}
		public String collectResults() {
			return "<xml>\n"+data.stream().map(this::mapMessage).collect(Collectors.joining())+"\n</xml>";
		}
		private String mapMessage(Message message) {
			try {
				return message.asString();
			} catch (IOException e) {
				return String.format("<exception>%s</exception>", e.getMessage());
			}
		}
	}
}
