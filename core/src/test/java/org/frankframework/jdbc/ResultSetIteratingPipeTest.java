package org.frankframework.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.DeflaterInputStream;

import jakarta.annotation.Nonnull;

import org.springframework.core.task.SimpleAsyncTaskExecutor;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.dbms.JdbcException;
import org.frankframework.parameters.Parameter;
import org.frankframework.senders.EchoSender;
import org.frankframework.stream.Message;
import org.frankframework.testutil.JdbcTestUtil;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.MessageTestUtils;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.testutil.junit.DatabaseTest;
import org.frankframework.testutil.junit.WithLiquibase;
import org.frankframework.util.StreamUtil;

@WithLiquibase(file = "Migrator/ChangelogBlobTests.xml", tableName = ResultSetIteratingPipeTest.TEST_TABLE)
public class ResultSetIteratingPipeTest extends JdbcEnabledPipeTestBase<ResultSetIteratingPipe> {
	static final String TEST_TABLE = "temp";
	private static final int PARALLEL_DELAY = 200;

	@Override
	public ResultSetIteratingPipe createPipe() {
		return new ResultSetIteratingPipe();
	}

	private void preFillDatabaseTable() throws Exception {
		URL url = TestFileUtils.getTestFileURL("/Pipes/ResultSetIteratingPipe/sqlInserts.txt");
		assertNotNull(url);

		Reader reader = StreamUtil.getCharsetDetectingInputStreamReader(url.openStream());
		BufferedReader buf = new BufferedReader(reader);
		String line = buf.readLine();
		int i = 1234;
		while (line != null) {
			insert(i++, line);
			line = buf.readLine();
		}
	}

	private void insert(int key, String value) throws JdbcException, SQLException {
		try(Connection connection = env.getConnection()) {
			JdbcTestUtil.executeStatement(connection, ("INSERT INTO " + TEST_TABLE + " (TKEY, TVARCHAR, TINT) VALUES ('%d', '%s', '0')").formatted(key, value));
		}
	}

	@DatabaseTest
	public void testWithStylesheetNoCollectResultsAndIgnoreExceptions() throws Exception {
		preFillDatabaseTable();

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

	@DatabaseTest
	public void testWithStylesheetNoCollectResultsAndIgnoreExceptionsParallel() throws Exception {
		preFillDatabaseTable();

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
		assertTrue(duration < PARALLEL_DELAY + 100, "Test took "+(duration- (PARALLEL_DELAY + 100))+"ms too long.");
		assertEquals("<results count=\"10\"/>", result.getResult().asString());
		String expectedXml = TestFileUtils.getTestFile("/Pipes/ResultSetIteratingPipe/result.xml");

		MatchUtils.assertXmlEquals(expectedXml, sender.collectResults()); //to display clean diff
	}

	@DatabaseTest
	public void testWithStylesheetNoCollectResultsAndIgnoreExceptionsWithUpdateInSameTable() throws Exception {
		preFillDatabaseTable();

		pipe.setQuery("SELECT TKEY, TVARCHAR FROM "+TEST_TABLE+" ORDER BY TKEY");
		pipe.setStyleSheetName("Pipes/ResultSetIteratingPipe/CreateMessage.xsl");
		pipe.setCollectResults(false);
		pipe.setIgnoreExceptions(true);
		pipe.setDatasourceName(getDataSourceName());

		FixedQuerySender sender = env.createBean(FixedQuerySender.class);
		sender.setQuery("UPDATE "+TEST_TABLE+" SET TINT = '4', TDATE = CURRENT_TIMESTAMP WHERE TKEY = ?");
		Parameter param = new Parameter();
		param.setName("ID");
		param.setXpathExpression("result/id");
		sender.addParameter(param);
		pipe.setSender(sender);

		configurePipe();
		pipe.start();

		PipeRunResult result = doPipe("since query attribute is set, this should be ignored");
		assertEquals("<results count=\"10\"/>", result.getResult().asString());
		try(Connection connection = env.getConnection()) {
			String jdbcResult = JdbcTestUtil.executeStringQuery(connection, "SELECT COUNT('TKEY') FROM "+TEST_TABLE+" WHERE TINT = '4'");
			assertEquals("10", jdbcResult);
		}
	}

	private void insertBlob(int key, InputStream blob, boolean compressBlob) throws Exception {
		try(Connection connection = env.getConnection();
				PreparedStatement stmt = connection.prepareStatement("INSERT INTO "+TEST_TABLE+" (TKEY, TBLOB, TBOOLEAN, TVARCHAR) VALUES (?, ?, ?, ?)")) {
			stmt.setInt(1, key);
			stmt.setBinaryStream(2, (compressBlob ? new DeflaterInputStream(blob) : blob));
			stmt.setBoolean(3, compressBlob);
			stmt.setString(4, "blobtest");
			stmt.execute();
		}
	}

	@DatabaseTest
	public void testWithCompressedAndDecompressedBlob() throws Exception {
		preFillDatabaseTable();

		// Arrange
		Message xmlMessage = MessageTestUtils.getMessage("/file.xml");
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
		public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
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
				return "<exception>%s</exception>".formatted(e.getMessage());
			}
		}
	}
}
