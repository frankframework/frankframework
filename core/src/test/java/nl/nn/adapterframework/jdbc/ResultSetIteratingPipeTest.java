package nl.nn.adapterframework.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.DeflaterInputStream;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.senders.EchoSender;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.StreamUtil;

public class ResultSetIteratingPipeTest extends JdbcEnabledPipeTestBase<ResultSetIteratingPipe> {

	private static final int PARALLEL_DELAY = 200;

	@Override
	public ResultSetIteratingPipe createPipe() {
		return new ResultSetIteratingPipe();
	}

	//Read a file, each row will be added to the database as (TKEY, TVARCHAR) with a new unique key.
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
		JdbcUtil.executeStatement(connection, String.format("INSERT INTO "+TEST_TABLE+" (TKEY, TVARCHAR, TINT) VALUES ('%d', '%s', '0')", key, value));
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
		pipe.setQuery("SELECT TKEY, TVARCHAR FROM "+TEST_TABLE+" ORDER BY TKEY");
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
		MatchUtils.assertXmlEquals(xmlResult, sender.collectResults());
	}

	@Test
	public void testWithStylesheetNoCollectResultsAndIgnoreExceptionsParallel() throws Exception {
		pipe.setQuery("SELECT TKEY, TVARCHAR FROM "+TEST_TABLE+" ORDER BY TKEY");
		pipe.setStyleSheetName("Pipes/ResultSetIteratingPipe/CreateMessage.xsl");
		pipe.setCollectResults(false);
		pipe.setParallel(true);
		pipe.setIgnoreExceptions(true);
		pipe.setDatasourceName(getDataSourceName());
		pipe.setTaskExecutor(new SimpleAsyncTaskExecutor());

		ResultCollectingSender sender = new ResultCollectingSender(PARALLEL_DELAY);
		pipe.setSender(sender);

		configurePipe();
		pipe.start();

		long startTime = System.currentTimeMillis();
		PipeRunResult result = doPipe("since query attribute is set, this should be ignored");
		long duration = System.currentTimeMillis() - startTime;
		assertTrue("Test took "+(duration- (PARALLEL_DELAY + 100))+"ms too long.", duration < PARALLEL_DELAY + 100);
		assertEquals("<results count=\"10\"/>", result.getResult().asString());
		String expectedXml = TestFileUtils.getTestFile("/Pipes/ResultSetIteratingPipe/result.xml");

		MatchUtils.assertXmlEquals(expectedXml, sender.collectResults()); //to display clean diff
	}

	@Test
	public void testWithStylesheetNoCollectResultsAndIgnoreExceptionsWithUpdateInSameTable() throws Exception {
		pipe.setQuery("SELECT TKEY, TVARCHAR FROM "+TEST_TABLE+" ORDER BY TKEY");
		pipe.setStyleSheetName("Pipes/ResultSetIteratingPipe/CreateMessage.xsl");
		pipe.setCollectResults(false);
		pipe.setIgnoreExceptions(true);
		pipe.setDatasourceName(getDataSourceName());

		FixedQuerySender sender = new FixedQuerySender();
		sender.setQuery("UPDATE "+TEST_TABLE+" SET TINT = '4', TDATE = CURRENT_TIMESTAMP WHERE TKEY = ?");
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
		String jdbcResult = JdbcUtil.executeStringQuery(connection, "SELECT COUNT('TKEY') FROM "+TEST_TABLE+" WHERE TINT = '4'");
		assertEquals("10", jdbcResult);
	}

	private void insertBlob(int key, InputStream blob, boolean compressBlob) throws Exception {
		try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO "+TEST_TABLE+" (TKEY, TBLOB, TBOOLEAN, TVARCHAR) VALUES (?, ?, ?, ?)")) {
			stmt.setInt(1, key);
			stmt.setBinaryStream(2, (compressBlob ? new DeflaterInputStream(blob) : blob));
			stmt.setBoolean(3, compressBlob);
			stmt.setString(4, "blobtest");
			stmt.execute();
		}
	}

	@Test
	public void testWithCompressedAndDecompressedBlob() throws Exception {
		// Arrange
		Message xmlMessage = TestFileUtils.getTestFileMessage("/file.xml");
		xmlMessage.preserve();
		insertBlob(1, xmlMessage.asInputStream(), false);
		insertBlob(2, xmlMessage.asInputStream(), true);
		insertBlob(3, xmlMessage.asInputStream(), false);

		// Act
		pipe.setQuery("SELECT TKEY, TBLOB FROM "+TEST_TABLE+" WHERE TVARCHAR='blobtest' ORDER BY TKEY");
		pipe.setBlobSmartGet(true);
		pipe.setDatasourceName(getDataSourceName());

		EchoSender sender = new EchoSender();
		pipe.setSender(sender);

		configurePipe();
		pipe.start();

		// Assert
		PipeRunResult result = doPipe("since query attribute is set, this should be ignored");
		String expected = TestFileUtils.getTestFile("/Pipes/ResultSetIteratingPipe/testCompressedAndUncompressedBlob.xml");
		MatchUtils.assertXmlEquals(expected, result.getResult().asString());
	}

	private static class ResultCollectingSender extends EchoSender {
		private List<Message> data = Collections.synchronizedList(new ArrayList<>());
		private int delay = 0;
		public ResultCollectingSender() {
			this(0);
		}

		public ResultCollectingSender(int delay) {
			this.delay = delay;
		}

		@Override
		public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
			if(delay > 0) {
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
					e.printStackTrace();
					return new SenderResult(Message.nullMessage());
				}
			}

			data.add(message);
			return super.sendMessage(message, session);
		}
		public String collectResults() throws InterruptedException {
			while(data.size() < 10) {
				log.info("sleeping, result count [{}]", data::size);
				Thread.sleep(200);
			}
			return "<xml>\n"+data.stream().map(this::mapMessage).sorted().collect(Collectors.joining())+"\n</xml>";
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
