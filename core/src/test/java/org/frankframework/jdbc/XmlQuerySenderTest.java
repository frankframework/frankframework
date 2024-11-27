package org.frankframework.jdbc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.SQLException;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.frankframework.core.ConfiguredTestBase;
import org.frankframework.core.PipeLineSession;
import org.frankframework.dbms.JdbcException;
import org.frankframework.stream.Message;
import org.frankframework.testutil.JdbcTestUtil;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.junit.DatabaseTest;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.WithLiquibase;

@WithLiquibase(tableName = XmlQuerySenderTest.TEST_TABLE, file = "Migrator/ChangelogBlobTests.xml")
public class XmlQuerySenderTest {

	protected static final String TEST_TABLE = "temp";

	private XmlQuerySender xmlQuerySender = new XmlQuerySender();
	private PipeLineSession session;
	private DatabaseTestEnvironment env;

	@BeforeEach
	public void setup(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		TestConfiguration configuration = databaseTestEnvironment.getConfiguration();
		env = databaseTestEnvironment;
		session = new PipeLineSession();
		session.put(PipeLineSession.MESSAGE_ID_KEY, ConfiguredTestBase.testMessageId);
		session.put(PipeLineSession.CORRELATION_ID_KEY, ConfiguredTestBase.testCorrelationId);

		xmlQuerySender.setDatasourceName(databaseTestEnvironment.getDataSourceName());
		xmlQuerySender.setName(TEST_TABLE);
		xmlQuerySender.setIncludeFieldDefinition(false);
		configuration.autowireByName(xmlQuerySender);
		configuration.autowireByName(xmlQuerySender);
	}

	@AfterEach
	public void teardown() {
		session.close();
	}

	private void insert(int key, String value) throws JdbcException, SQLException {
		try (Connection connection = env.getConnection()) {
			JdbcTestUtil.executeStatement(connection, ("INSERT INTO " + TEST_TABLE + " (TKEY, TVARCHAR, TINT) VALUES ('%d', '%s', '0')").formatted(key, value));
		}
	}

	@DatabaseTest
	public void testSelectQuery() throws Exception {
		insert(1, "value1");
		insert(2, "value2");
		xmlQuerySender.configure();
		xmlQuerySender.start();

		Message result = xmlQuerySender.sendMessage(new Message("""
				<select><tableName>temp</tableName>
					<columns>
						<column><name>TKEY</name><type>VARCHAR</type></column>
					</columns>
					<where>1=1</where>
				</select>"""), session).getResult();
		assertThat(result.asString(), Matchers.containsString("<row number=\"0\"><field name=\"TKEY\">1</field></row><row number=\"1\"><field name=\"TKEY\">2</field></row></rowset>"));
	}

	@DatabaseTest
	public void testInsertQuery() throws Exception {
		insert(1, "value1");
		insert(2, "value2");
		xmlQuerySender.configure();
		xmlQuerySender.start();

		Message result = xmlQuerySender.sendMessage(new Message("""
				<insert><tableName>temp</tableName>
					<columns>
						<column><name>TKEY</name><value>3</value></column>
						<column><name>TVARCHAR</name><value>hello</value></column>
					</columns>
				</insert>"""), session).getResult();
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());
	}

	@DatabaseTest
	public void testDeleteQuery() throws Exception {
		insert(1, "value1");
		insert(2, "value2");
		xmlQuerySender.configure();
		xmlQuerySender.start();

		Message result = xmlQuerySender.sendMessage(new Message("""
				<delete><tableName>temp</tableName>
					<where>TKEY=2</where>
				</delete>"""), session).getResult();
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());
	}

	@DatabaseTest
	public void testUpdateQuery() throws Exception {
		insert(1, "value1");
		insert(2, "value2");
		xmlQuerySender.configure();
		xmlQuerySender.start();

		Message result = xmlQuerySender.sendMessage(new Message("""
				<update><tableName>temp</tableName>
					<columns>
						<column><name>TVARCHAR</name><value>value5</value></column>
					</columns>
					<where>TKEY=2</where>
				</update>"""), session).getResult();
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());
	}

}
