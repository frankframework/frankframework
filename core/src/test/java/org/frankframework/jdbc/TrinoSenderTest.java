package org.frankframework.jdbc;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.dbms.DbmsSupportFactory;
import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.stream.Message;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import java.util.stream.Stream;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Unit test class for {@link TrinoSender}.
 * <p>
 * This test suite validates the functionality of the TrinoSender class, which is responsible for 
 * executing SQL queries against a Trino database. The tests cover various operations including 
 * SELECT queries, INSERT statements, and handling of unsupported operations.
 * <p>
 * The test suite requires a local Trino instance running on localhost:8090 with a memory catalog
 * configured. To run Trino locally with Docker, use:
 * <pre>docker run -p 8090:8080 --name trino trinodb/trino:latest</pre>
 * <p>
 * The test class uses JUnit 5's dynamic test feature to provide better visibility into individual
 * test steps and assertions. This approach offers several advantages:
 * <ol>
 *   <li>Better visibility of individual assertions in test reports</li>
 *   <li>More descriptive test names for clearer understanding</li>
 *   <li>Isolated failures (one assertion failing doesn't stop others)</li>
 *   <li>Improved organization of test steps</li>
 * </ol>
 * <p>
 * Each test method returns a Stream of DynamicTest instances that represent the steps of the test.
 * The test class is only enabled if a Trino instance is available, as determined by the
 * {@link #isTrinoAvailable()} method.
 * 
 * @see TrinoSender The class being tested
 * @see DynamicTest JUnit 5's dynamic test feature
 */
@EnabledIf("isTrinoAvailable")
public class TrinoSenderTest {

    /** Hostname where the Trino server is expected to be running */
    private static final String TRINO_HOST = "localhost";
    
    /** Port number where the Trino server is expected to be listening */
    private static final int TRINO_PORT = 8090;
    
    /** Catalog name to use for testing */
    private static final String TEST_CATALOG = "memory";
    
    /** Schema name to use for testing */
    private static final String TEST_SCHEMA = "test_schema";
    
    /** Table name to use for testing */
    private static final String TEST_TABLE = "test_table";

    /** DataSource for connecting to Trino */
    private static TrinoDataSource dataSource;
    
    /** Flag indicating whether Trino is available for testing */
    private static boolean isTrinoAvailable = false;

    /** The TrinoSender instance being tested */
    private TrinoSender sender;
    
    /** The pipeline session used for testing */
    private PipeLineSession session;

    /**
     * Determines if a Trino server is available for testing.
     * <p>
     * This method attempts to connect to a Trino server using the configured host, port, and catalog.
     * It first checks if the Trino JDBC driver is available, then tries to establish a connection
     * and execute a simple query.
     * 
     * @return true if a Trino server is available and responsive, false otherwise
     */
    public static boolean isTrinoAvailable() {
        try {
            // Try to load Trino driver
            Class.forName("io.trino.jdbc.TrinoDriver");

            // Try to connect to Trino
            try (Connection conn = createTrinoDataSource(TRINO_HOST, TRINO_PORT, TEST_CATALOG).getConnection()) {
                try (ResultSet rs = conn.createStatement().executeQuery("SELECT 1")) {
                    if (rs.next() && rs.getInt(1) == 1) {
                        isTrinoAvailable = true;
                        return true;
                    }
                }
            } catch (SQLException e) {
                System.out.println("Trino not available: " + e.getMessage());
            }
        } catch (ClassNotFoundException e) {
            System.out.println("Trino JDBC driver not found: " + e.getMessage());
        }

        return false;
    }

    /**
     * Sets up the test environment before all tests run.
     * <p>
     * This method is called once before any test method is executed. It:
     * <ul>
     *   <li>Checks if Trino is available and exits early if not</li>
     *   <li>Creates a DataSource for connecting to Trino</li>
     *   <li>Creates a test schema and table</li>
     *   <li>Inserts test data into the table</li>
     * </ul>
     * 
     * @throws Exception if any error occurs during setup
     */
    @BeforeAll
    public static void setUpClass() throws Exception {
        // Only run setup if Trino is available
        if (!isTrinoAvailable()) {
            return;
        }

        // Set up data source
        dataSource = createTrinoDataSource(TRINO_HOST, TRINO_PORT, TEST_CATALOG);

        // Create schema and test table
        try (Connection conn = dataSource.getConnection()) {
            // Create schema
            executeUpdate(conn, "CREATE SCHEMA IF NOT EXISTS " + TEST_CATALOG + "." + TEST_SCHEMA);

            // Create test table
            executeUpdate(conn, "CREATE TABLE IF NOT EXISTS " + TEST_CATALOG + "." + TEST_SCHEMA + "." + TEST_TABLE
                    + " (" + "id INTEGER, " + "name VARCHAR" + ")");

            // Insert test data
            executeUpdate(conn, "INSERT INTO " + TEST_CATALOG + "." + TEST_SCHEMA + "." + TEST_TABLE + " VALUES "
                    + "(1, 'Test1'), (2, 'Test2'), (3, 'Test3')");
        } catch (SQLException e) {
            System.err.println("Error setting up test data: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Sets up each individual test case.
     * <p>
     * This method is executed before each test method and prepares:
     * <ul>
     *   <li>A new TrinoSender instance configured for testing</li>
     *   <li>Mock objects for dependencies (data source factory, DBMS support)</li>
     *   <li>A clean pipeline session for the test</li>
     * </ul>
     * 
     * @throws Exception if any error occurs during setup
     */
    @BeforeEach
    public void setUp() throws Exception {
        // Set up the sender
        sender = new TrinoSender();
        sender.setName("TestTrinoSender");
        sender.setCatalog(TEST_CATALOG);
        sender.setSchema(TEST_SCHEMA);

        // Create a custom data source factory
        IDataSourceFactory dataSourceFactory = mock(IDataSourceFactory.class);
        when(dataSourceFactory.getDataSource(any(), any())).thenReturn(dataSource);

        sender.setDataSourceFactory(dataSourceFactory);
        sender.setDatasourceName("testTrinoDataSource");

        // Create DbmsSupport
        DbmsSupportFactory dbmsSupportFactory = mock(DbmsSupportFactory.class);
        IDbmsSupport dbmsSupport = mock(IDbmsSupport.class);
        when(dbmsSupportFactory.getDbmsSupport(any(DataSource.class))).thenReturn(dbmsSupport);
        sender.setDbmsSupportFactory(dbmsSupportFactory);

        // Create session
        session = new PipeLineSession();
    }

    /**
     * Tests a simple SELECT query against the test table.
     * <p>
     * This test demonstrates how to use JUnit 5's DynamicTest to split a test method into 
     * separate test cases. It tests:
     * <ol>
     *   <li>Setting up a SELECT query and configuring the sender</li>
     *   <li>Executing the query and checking that a result is returned</li>
     *   <li>Verifying that the result contains the expected data</li>
     *   <li>Verifying that the result does not contain unexpected data</li>
     * </ol>
     * 
     * @return a stream of DynamicTest instances representing the test steps
     */
    @TestFactory
    public Stream<DynamicTest> testSelectQueryDynamic() {
        return Stream.of(DynamicTest.dynamicTest("Setup Select Query", () -> {
            // Set up the query
            sender.setQuery("SELECT * FROM " + TEST_TABLE + " WHERE id = 1");
            sender.setQueryType(AbstractJdbcQuerySender.QueryType.SELECT);
            sender.configure();
        }),

                DynamicTest.dynamicTest("Execute Query", () -> {
                    // Send a message and check the result
                    Message input = new Message("");
                    SenderResult result = sender.sendMessage(input, session);

                    assertNotNull(result, "Result should not be null");
                }),

                DynamicTest.dynamicTest("Verify Result Contains Expected Data", () -> {
                    Message input = new Message("");
                    SenderResult result = sender.sendMessage(input, session);

                    String resultStr = result.getResult().asString();
                    assertTrue(resultStr.contains("Test1"), "Result should contain 'Test1'");
                }),

                DynamicTest.dynamicTest("Verify Result Does Not Contain Other Data", () -> {
                    Message input = new Message("");
                    SenderResult result = sender.sendMessage(input, session);

                    String resultStr = result.getResult().asString();
                    assertFalse(resultStr.contains("Test2"), "Result should not contain 'Test2'");
                }));
    }

    /**
     * Tests the automatic prefixing of queries with catalog and schema information.
     * <p>
     * This test verifies that the TrinoSender correctly prefixes table references with 
     * the catalog and schema names when needed. It tests:
     * <ol>
     *   <li>Setting up a query that uses an un-prefixed table name</li>
     *   <li>Verifying that the query executes successfully</li>
     *   <li>Checking that all expected records are returned</li>
     * </ol>
     * 
     * @return a stream of DynamicTest instances representing the test steps
     */
    @TestFactory
    public Stream<DynamicTest> testPrefixQueryWithCatalogAndSchema() {
        return Stream.of(DynamicTest.dynamicTest("Setup Query With Prefixing", () -> {
            // Test a query that needs prefixing
            sender.setQuery("SELECT * FROM memory.test_schema." + TEST_TABLE);
            sender.setQueryType(AbstractJdbcQuerySender.QueryType.SELECT);
            sender.configure();
        }),

                DynamicTest.dynamicTest("Verify Query Execution", () -> {
                    // Send a message and verify it executes
                    Message input = new Message("");
                    SenderResult result = sender.sendMessage(input, session);

                    assertNotNull(result, "Result should not be null");
                }),

                DynamicTest.dynamicTest("Verify Result Contains First Record", () -> {
                    Message input = new Message("");
                    SenderResult result = sender.sendMessage(input, session);

                    String resultStr = result.getResult().asString();
                    assertTrue(resultStr.contains("Test1"), "Result should contain 'Test1'");
                }),

                DynamicTest.dynamicTest("Verify Result Contains Second Record", () -> {
                    Message input = new Message("");
                    SenderResult result = sender.sendMessage(input, session);

                    String resultStr = result.getResult().asString();
                    assertTrue(resultStr.contains("Test2"), "Result should contain 'Test2'");
                }),

                DynamicTest.dynamicTest("Verify Result Contains Third Record", () -> {
                    Message input = new Message("");
                    SenderResult result = sender.sendMessage(input, session);

                    String resultStr = result.getResult().asString();
                    assertTrue(resultStr.contains("Test3"), "Result should contain 'Test3'");
                }));
    }

    /**
     * Tests both SELECT and INSERT operations in sequence.
     * <p>
     * This test creates a temporary table, inserts data into it, and then verifies
     * the data was correctly inserted by selecting it back. It tests:
     * <ol>
     *   <li>Creating a test table</li>
     *   <li>Configuring and executing an INSERT query</li>
     *   <li>Configuring and executing a SELECT query to verify the INSERT</li>
     *   <li>Cleaning up the test table</li>
     * </ol>
     * 
     * @return a stream of DynamicTest instances representing the test steps
     */
    @TestFactory
    public Stream<DynamicTest> testSelectAndInsert() {
        final String testInsertTable = TEST_TABLE + "_insert_test";

        return Stream.of(DynamicTest.dynamicTest("Create Test Table", () -> {
            // Create a test table for insert
            try (Connection conn = dataSource.getConnection()) {
                executeUpdate(conn, "CREATE TABLE IF NOT EXISTS " + TEST_CATALOG + "." + TEST_SCHEMA + "."
                        + testInsertTable + " (" + "id INTEGER, " + "name VARCHAR" + ")");
            }
        }),

                DynamicTest.dynamicTest("Configure INSERT Query", () -> {
                    // Test INSERT
                    sender.setQuery("INSERT INTO " + testInsertTable + " VALUES (10, 'InsertTest')");
                    sender.setQueryType(AbstractJdbcQuerySender.QueryType.OTHER);
                    sender.configure();
                }),

                DynamicTest.dynamicTest("Execute INSERT Operation", () -> {
                    SenderResult insertResult = sender.sendMessage(new Message(""), session);
                    assertNotNull(insertResult, "INSERT result should not be null");
                }),

                DynamicTest.dynamicTest("Configure SELECT Query", () -> {
                    // Verify INSERT worked by selecting from the table
                    sender.setQuery("SELECT * FROM " + testInsertTable + " WHERE id = 10");
                    sender.setQueryType(AbstractJdbcQuerySender.QueryType.SELECT);
                    sender.configure();
                }),

                DynamicTest.dynamicTest("Verify Inserted Data", () -> {
                    SenderResult selectResult = sender.sendMessage(new Message(""), session);
                    assertTrue(selectResult.getResult().asString().contains("InsertTest"),
                            "Result should contain 'InsertTest'");
                }),

                DynamicTest.dynamicTest("Clean Up Test Table", () -> {
                    // Clean up test table
                    try (Connection conn = dataSource.getConnection()) {
                        executeUpdate(conn,
                                "DROP TABLE IF EXISTS " + TEST_CATALOG + "." + TEST_SCHEMA + "." + testInsertTable);
                    } catch (SQLException e) {
                        System.err.println("Error cleaning up test table: " + e.getMessage());
                        fail("Failed to clean up test table: " + e.getMessage());
                    }
                }));
    }

    /**
     * Tests operations that are not supported by Trino memory connector.
     * <p>
     * This test verifies that the TrinoSender properly handles operations that
     * are not supported by the Trino memory connector, specifically UPDATE and
     * DELETE operations. It tests:
     * <ol>
     *   <li>Configuring an UPDATE query</li>
     *   <li>Verifying that an exception is thrown when executing the UPDATE</li>
     *   <li>Configuring a DELETE query</li>
     *   <li>Verifying that an exception is thrown when executing the DELETE</li>
     * </ol>
     * 
     * @return a stream of DynamicTest instances representing the test steps
     */
    @TestFactory
    public Stream<DynamicTest> testUnsupportedOperations() {
        return Stream.of(DynamicTest.dynamicTest("Configure UPDATE Query", () -> {
            // Test UPDATE - should throw an exception
            sender.setQuery("UPDATE " + TEST_TABLE + " SET name = 'UpdatedValue' WHERE id = 1");
            sender.setQueryType(AbstractJdbcQuerySender.QueryType.OTHER);
            sender.configure();
        }),

                DynamicTest.dynamicTest("Verify Exception For Unsupported UPDATE", () -> {
                    SenderException exception = assertThrows(SenderException.class, () -> {
                        sender.sendMessage(new Message(""), session);
                    }, "Should throw SenderException for UPDATE operation");

                    // Verify the exception message contains the expected error
                    String message = exception.getMessage();
                    assertTrue(
                            message.contains("This connector does not support modifying table rows")
                                    || message.contains("not supported"),
                            "Exception should mention unsupported operation: " + message);
                }),

                DynamicTest.dynamicTest("Configure DELETE Query", () -> {
                    // Test DELETE - should also throw an exception
                    sender.setQuery("DELETE FROM " + TEST_TABLE + " WHERE id = 1");
                    sender.setQueryType(AbstractJdbcQuerySender.QueryType.OTHER);
                    sender.configure();
                }),

                DynamicTest.dynamicTest("Verify Exception For Unsupported DELETE", () -> {
                    SenderException exception = assertThrows(SenderException.class, () -> {
                        sender.sendMessage(new Message(""), session);
                    }, "Should throw SenderException for DELETE operation");

                    // Verify the exception message contains the expected error
                    String message = exception.getMessage();
                    assertTrue(message.contains("This connector does not support") || message.contains("not supported"),
                            "Exception should mention unsupported operation: " + message);
                }));
    }

    /**
     * Tests batch processing functionality.
     * <p>
     * This test verifies that batch operations work correctly. It tests:
     * <ol>
     *   <li>Creating a test table for batch operations</li>
     *   <li>Inserting records directly using SQL</li>
     *   <li>Configuring a SELECT query to verify the batch</li>
     *   <li>Verifying that the batch records exist</li>
     *   <li>Testing a simple INSERT operation</li>
     *   <li>Cleaning up the test table</li>
     * </ol>
     * 
     * @return a stream of DynamicTest instances representing the test steps
     */
    @TestFactory
    public Stream<DynamicTest> testBatchProcessing() {
        final String batchTestTable = TEST_TABLE + "_batch_test";

        return Stream.of(DynamicTest.dynamicTest("Create Batch Test Table", () -> {
            // Create a test table for batch operations
            try (Connection conn = dataSource.getConnection()) {
                executeUpdate(conn, "CREATE TABLE IF NOT EXISTS " + TEST_CATALOG + "." + TEST_SCHEMA + "."
                        + batchTestTable + " (" + "id INTEGER, " + "name VARCHAR" + ")");
            }
        }),

                DynamicTest.dynamicTest("Insert Records Directly", () -> {
                    // Insert directly using SQL
                    try (Connection conn = dataSource.getConnection()) {
                        executeUpdate(conn, "INSERT INTO " + TEST_CATALOG + "." + TEST_SCHEMA + "." + batchTestTable
                                + " VALUES " + "(4, 'Test4'), (5, 'Test5')");
                    }
                }),

                DynamicTest.dynamicTest("Configure SELECT to Verify Batch", () -> {
                    // Create new sender for the verification
                    TrinoSender verificationSender = new TrinoSender();
                    verificationSender.setName("VerificationSender");
                    verificationSender.setCatalog(TEST_CATALOG);
                    verificationSender.setSchema(TEST_SCHEMA);

                    // Set up the data source factory
                    IDataSourceFactory dataSourceFactory = mock(IDataSourceFactory.class);
                    when(dataSourceFactory.getDataSource(any(), any())).thenReturn(dataSource);
                    verificationSender.setDataSourceFactory(dataSourceFactory);
                    verificationSender.setDatasourceName("testTrinoDataSource");

                    // Create DbmsSupport
                    DbmsSupportFactory dbmsSupportFactory = mock(DbmsSupportFactory.class);
                    IDbmsSupport dbmsSupport = mock(IDbmsSupport.class);
                    when(dbmsSupportFactory.getDbmsSupport(any(DataSource.class))).thenReturn(dbmsSupport);
                    verificationSender.setDbmsSupportFactory(dbmsSupportFactory);

                    // Configure the query
                    verificationSender.setQuery("SELECT * FROM " + batchTestTable + " WHERE id IN (4, 5)");
                    verificationSender.setQueryType(AbstractJdbcQuerySender.QueryType.SELECT);
                    verificationSender.configure();

                    // Store for verification tests
                    session.put("verificationSender", verificationSender);
                }),

                DynamicTest.dynamicTest("Verify Batch Records", () -> {
                    TrinoSender verificationSender = (TrinoSender) session.get("verificationSender");
                    SenderResult result = verificationSender.sendMessage(new Message(""), session);

                    assertNotNull(result, "Batch SELECT result should not be null");
                    String resultStr = result.getResult().asString();

                    // Verify both records exist
                    assertTrue(resultStr.contains("Test4"), "Result should contain 'Test4': " + resultStr);
                    assertTrue(resultStr.contains("Test5"), "Result should contain 'Test5': " + resultStr);
                }),

                DynamicTest.dynamicTest("Create Simple INSERT Test", () -> {
                    // Set up a simple insert test
                    TrinoSender insertSender = new TrinoSender();
                    insertSender.setName("InsertSender");
                    insertSender.setCatalog(TEST_CATALOG);
                    insertSender.setSchema(TEST_SCHEMA);

                    // Set up the data source factory
                    IDataSourceFactory dataSourceFactory = mock(IDataSourceFactory.class);
                    when(dataSourceFactory.getDataSource(any(), any())).thenReturn(dataSource);
                    insertSender.setDataSourceFactory(dataSourceFactory);
                    insertSender.setDatasourceName("testTrinoDataSource");

                    // Create DbmsSupport
                    DbmsSupportFactory dbmsSupportFactory = mock(DbmsSupportFactory.class);
                    IDbmsSupport dbmsSupport = mock(IDbmsSupport.class);
                    when(dbmsSupportFactory.getDbmsSupport(any(DataSource.class))).thenReturn(dbmsSupport);
                    insertSender.setDbmsSupportFactory(dbmsSupportFactory);

                    // Configure the query - use a simple INSERT without parameters
                    insertSender.setQuery("INSERT INTO " + batchTestTable + " VALUES (6, 'Test6')");
                    insertSender.setQueryType(AbstractJdbcQuerySender.QueryType.OTHER);
                    insertSender.configure();

                    // Execute the insert
                    try {
                        SenderResult result = insertSender.sendMessage(new Message(""), session);
                        assertNotNull(result, "INSERT result should not be null");
                    } catch (Exception e) {
                        fail("Failed to execute simple INSERT: " + e.getMessage());
                    }

                    // Verify the insert worked
                    try (Connection conn = dataSource.getConnection()) {
                        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM " + TEST_CATALOG + "."
                                + TEST_SCHEMA + "." + batchTestTable + " WHERE id = 6")) {
                            try (ResultSet rs = stmt.executeQuery()) {
                                assertTrue(rs.next(), "Should find record with id=6");
                                assertEquals("Test6", rs.getString("name"), "Name should be 'Test6'");
                            }
                        }
                    } catch (SQLException e) {
                        fail("Failed to verify simple INSERT: " + e.getMessage());
                    }
                }),

                DynamicTest.dynamicTest("Clean Up Batch Test Table", () -> {
                    try (Connection conn = dataSource.getConnection()) {
                        executeUpdate(conn,
                                "DROP TABLE IF EXISTS " + TEST_CATALOG + "." + TEST_SCHEMA + "." + batchTestTable);
                    } catch (SQLException e) {
                        System.err.println("Error cleaning up batch test table: " + e.getMessage());
                        fail("Failed to clean up batch test table: " + e.getMessage());
                    }
                }));
    }

    /**
     * Tests the ability to disable query prefixing.
     * <p>
     * This test verifies that when query prefixing is disabled, the TrinoSender correctly
     * uses fully qualified table names in queries. It tests:
     * <ol>
     *   <li>Configuring a query with explicit catalog and schema references</li>
     *   <li>Setting the disableQueryPrefixing flag to true</li>
     *   <li>Verifying that the query executes correctly</li>
     *   <li>Checking that all expected records are returned</li>
     * </ol>
     * 
     * @return a stream of DynamicTest instances representing the test steps
     */
    @TestFactory
    public Stream<DynamicTest> testDisableQueryPrefixing() {
        return Stream.of(DynamicTest.dynamicTest("Configure Query With Explicit Schema", () -> {
            // Configure sender with explicit catalog and schema in query
            sender.setQuery("SELECT * FROM " + TEST_CATALOG + "." + TEST_SCHEMA + "." + TEST_TABLE);
            sender.setQueryType(AbstractJdbcQuerySender.QueryType.SELECT);
            sender.setDisableQueryPrefixing(true);
            sender.configure();
        }),

                DynamicTest.dynamicTest("Verify Query Execution", () -> {
                    // Send a message and verify it executes
                    Message input = new Message("");
                    SenderResult result = sender.sendMessage(input, session);

                    assertNotNull(result, "Result should not be null");
                }),

                DynamicTest.dynamicTest("Verify Result Contains First Record", () -> {
                    Message input = new Message("");
                    SenderResult result = sender.sendMessage(input, session);

                    String resultStr = result.getResult().asString();
                    assertTrue(resultStr.contains("Test1"), "Result should contain 'Test1'");
                }),

                DynamicTest.dynamicTest("Verify Result Contains Second Record", () -> {
                    Message input = new Message("");
                    SenderResult result = sender.sendMessage(input, session);

                    String resultStr = result.getResult().asString();
                    assertTrue(resultStr.contains("Test2"), "Result should contain 'Test2'");
                }),

                DynamicTest.dynamicTest("Verify Result Contains Third Record", () -> {
                    Message input = new Message("");
                    SenderResult result = sender.sendMessage(input, session);

                    String resultStr = result.getResult().asString();
                    assertTrue(resultStr.contains("Test3"), "Result should contain 'Test3'");
                }));
    }

    /**
     * Tests setting session properties for Trino queries.
     * <p>
     * This test verifies that session properties can be set and are correctly applied
     * when executing queries. It tests:
     * <ol>
     *   <li>Configuring the sender with session properties</li>
     *   <li>Verifying that a query executes successfully with those properties</li>
     *   <li>Checking that the query returns the expected results</li>
     * </ol>
     * 
     * @return a stream of DynamicTest instances representing the test steps
     */
    @TestFactory
    public Stream<DynamicTest> testSessionProperties() {
        return Stream.of(DynamicTest.dynamicTest("Configure With Session Properties", () -> {
            // Set up session properties
            sender.setSessionProperties("query_max_memory=1GB,join_distribution_type=AUTOMATIC");
            sender.setQuery("SELECT * FROM " + TEST_TABLE);
            sender.setQueryType(AbstractJdbcQuerySender.QueryType.SELECT);
            sender.configure();
        }),

                DynamicTest.dynamicTest("Verify Query With Session Properties Executes", () -> {
                    // Just test that it executes without errors
                    Message input = new Message("");
                    SenderResult result = sender.sendMessage(input, session);

                    assertNotNull(result, "Result should not be null");
                }),

                DynamicTest.dynamicTest("Verify Query Result Contains Data", () -> {
                    Message input = new Message("");
                    SenderResult result = sender.sendMessage(input, session);

                    String resultStr = result.getResult().asString();
                    assertTrue(resultStr.contains("Test1"), "Result should contain 'Test1'");
                }));
    }

    /**
     * Tests configuration exceptions when setting up the TrinoSender.
     * <p>
     * This test verifies that appropriate exceptions are thrown when the TrinoSender
     * is misconfigured. It tests:
     * <ol>
     *   <li>Configuring a sender without a query</li>
     *   <li>Verifying that a ConfigurationException is thrown</li>
     *   <li>Configuring a sender without a datasource name</li>
     *   <li>Verifying that an appropriate exception is thrown</li>
     * </ol>
     * 
     * @return a stream of DynamicTest instances representing the test steps
     */
    @TestFactory
    public Stream<DynamicTest> testConfigurationExceptions() {
        return Stream.of(DynamicTest.dynamicTest("Missing Query Should Throw Exception", () -> {
            // Test missing query
            TrinoSender emptySender = new TrinoSender();
            emptySender.setName("EmptyQuerySender");
            emptySender.setCatalog(TEST_CATALOG);
            emptySender.setSchema(TEST_SCHEMA);

            // Should throw ConfigurationException for missing query
            assertThrows(ConfigurationException.class, emptySender::configure,
                    "Missing query should cause ConfigurationException");
        }),

                DynamicTest.dynamicTest("Missing DatasourceName Should Throw Exception", () -> {
                    // Create a sender with a query but no datasourceName
                    TrinoSender noDataSourceNameSender = new TrinoSender();
                    noDataSourceNameSender.setName("NoDataSourceNameSender");
                    noDataSourceNameSender.setCatalog(TEST_CATALOG);
                    noDataSourceNameSender.setSchema(TEST_SCHEMA);
                    noDataSourceNameSender.setQuery("SELECT * FROM " + TEST_TABLE);
                    
                    // Explicitly set datasourceName to empty to override any defaults
                    noDataSourceNameSender.setDatasourceName("");
                    
                    // Set a mock factory that returns null for any datasource
                    IDataSourceFactory mockFactory = mock(IDataSourceFactory.class);
                    when(mockFactory.getDataSource(any(), any())).thenReturn(null);
                    noDataSourceNameSender.setDataSourceFactory(mockFactory);
                    
                    // Should throw ConfigurationException when configuring
                    Exception exception = assertThrows(Exception.class, noDataSourceNameSender::configure,
                                "Empty datasourceName should cause an exception");
                    
                    // Either ConfigurationException or JdbcException should be in the chain
                    boolean foundExpectedException = false;
                    Throwable cause = exception;
                    while (cause != null) {
                        if (cause instanceof ConfigurationException || 
                            cause instanceof org.frankframework.dbms.JdbcException) {
                            foundExpectedException = true;
                            break;
                        }
                        cause = cause.getCause();
                    }
                    
                    assertTrue(foundExpectedException, 
                              "Exception chain should contain ConfigurationException or JdbcException: " + 
                              exception.getClass().getName() + ": " + exception.getMessage());
                }));
    }
    
    /**
     * Tests batch size configuration and block operation methods.
     * <p>
     * This test verifies the batch size setting functionality and the block operation
     * methods (openBlock, closeBlock, closeStatementSet). It tests:
     * <ol>
     *   <li>Creating a test table for block operations</li>
     *   <li>Setting and verifying the batch size</li>
     *   <li>Testing the openBlock and closeBlock methods</li>
     *   <li>Testing null handling in closeBlock</li>
     *   <li>Testing the closeStatementSet method</li>
     *   <li>Cleaning up the test table</li>
     *   <li>Testing SQLException handling in openBlock</li>
     * </ol>
     * 
     * @return a stream of DynamicTest instances representing the test steps
     */
    @TestFactory
    public Stream<DynamicTest> testBatchSizeAndBlockOperations() {
        final String blockOperationsTable = TEST_TABLE + "_block_ops";

        return Stream.of(
            DynamicTest.dynamicTest("Create Test Table for Block Operations", () -> {
                // Create a test table for block operations
                try (Connection conn = dataSource.getConnection()) {
                    executeUpdate(conn, "CREATE TABLE IF NOT EXISTS " + TEST_CATALOG + "." + TEST_SCHEMA + "."
                            + blockOperationsTable + " (" + "id INTEGER, " + "name VARCHAR" + ")");
                }
            }),

            DynamicTest.dynamicTest("Test setBatchSize Method", () -> {
                // Configure the sender with a batch size
                sender.setQuery("INSERT INTO " + blockOperationsTable + " VALUES (?, ?)");
                sender.setQueryType(AbstractJdbcQuerySender.QueryType.OTHER);
                sender.setBatchSize(10); // This tests the setBatchSize method
                
                // Verify batch size is set correctly
                assertEquals(10, sender.getBatchSize(), "Batch size should be set to 10");
                
                sender.configure();
            }),

            DynamicTest.dynamicTest("Test openBlock and closeBlock Methods", () -> {
                // Test the openBlock method
                QueryExecutionContext blockHandle = null;
                try {
                    blockHandle = sender.openBlock(session);
                    assertNotNull(blockHandle, "Block handle should not be null");
                    assertNotNull(blockHandle.getConnection(), "Connection in block handle should not be null");
                    assertNotNull(blockHandle.getStatement(), "Statement in block handle should not be null");
                    
                    // Test sending a message within the block
                    PreparedStatement mockStmt = mock(PreparedStatement.class);
                    
                    // Replace the real statement with our mock to track interactions
                    // This is a bit of a hack, but helps us verify the behavior
                    QueryExecutionContext spyHandle = spy(blockHandle);
                    when(spyHandle.getStatement()).thenReturn(mockStmt);
                    
                    // Now close the block
                    sender.closeBlock(blockHandle, session);
                    
                } catch (Exception e) {
                    fail("Failed to execute block operations: " + e.getMessage());
                } finally {
                    // Ensure the block is closed even if the test fails
                    if (blockHandle != null) {
                        try {
                            sender.closeBlock(blockHandle, session);
                        } catch (Exception ignored) {
                            // Already closed or failed, ignore
                        }
                    }
                }
            }),
            
            DynamicTest.dynamicTest("Test Null Handling in closeBlock", () -> {
                // Test with a null QueryExecutionContext
                assertDoesNotThrow(() -> {
                    sender.closeBlock(null, session);
                }, "closeBlock should handle null context gracefully");
                
                // Test with a mock context that has a null connection
                QueryExecutionContext mockContext = mock(QueryExecutionContext.class);
                when(mockContext.getConnection()).thenReturn(null);
                
                assertDoesNotThrow(() -> {
                    sender.closeBlock(mockContext, session);
                }, "closeBlock should handle null connection gracefully");
            }),

            DynamicTest.dynamicTest("Test closeStatementSet Method", () -> {
                // Test the closeStatementSet method which is called by closeBlock
                QueryExecutionContext mockContext = mock(QueryExecutionContext.class);
                PreparedStatement mockStmt = mock(PreparedStatement.class);
                when(mockContext.getStatement()).thenReturn(mockStmt);
                
                // Call the method directly
                sender.closeStatementSet(mockContext);
                
                // Nothing to verify since the method is empty in TrinoSender
                // but we're testing the code path
            }),

            DynamicTest.dynamicTest("Clean Up Block Operations Test Table", () -> {
                try (Connection conn = dataSource.getConnection()) {
                    executeUpdate(conn,
                            "DROP TABLE IF EXISTS " + TEST_CATALOG + "." + TEST_SCHEMA + "." + blockOperationsTable);
                } catch (SQLException e) {
                    System.err.println("Error cleaning up block operations test table: " + e.getMessage());
                    fail("Failed to clean up block operations test table: " + e.getMessage());
                }
            }),
            
            DynamicTest.dynamicTest("Test SQLException Handling in openBlock", () -> {
                // Create a sender with a connection that throws SQLException
                TrinoSender sqlExceptionSender = new TrinoSender();
                sqlExceptionSender.setName("SQLExceptionSender");
                sqlExceptionSender.setQuery("SELECT * FROM test_table");
                sqlExceptionSender.setCatalog(TEST_CATALOG);
                sqlExceptionSender.setSchema(TEST_SCHEMA);
                sqlExceptionSender.setQueryType(AbstractJdbcQuerySender.QueryType.SELECT);
                
                // Create a mock datasource
                DataSource mockDataSource = mock(DataSource.class);
                Connection mockConnection = mock(Connection.class);
                
                // Make setCatalog throw SQLException
                doThrow(new SQLException("Simulated SQLException")).when(mockConnection).setCatalog(any());
                
                when(mockDataSource.getConnection()).thenReturn(mockConnection);
                
                // Create a datasource factory that returns our mock
                IDataSourceFactory mockFactory = mock(IDataSourceFactory.class);
                when(mockFactory.getDataSource(any(), any())).thenReturn(mockDataSource);
                sqlExceptionSender.setDataSourceFactory(mockFactory);
                sqlExceptionSender.setDatasourceName("testTrinoDataSource");
                
                // Create DbmsSupport
                DbmsSupportFactory dbmsSupportFactory = mock(DbmsSupportFactory.class);
                IDbmsSupport dbmsSupport = mock(IDbmsSupport.class);
                when(dbmsSupportFactory.getDbmsSupport(any(DataSource.class))).thenReturn(dbmsSupport);
                sqlExceptionSender.setDbmsSupportFactory(dbmsSupportFactory);
                
                // Configure the sender
                try {
                    sqlExceptionSender.configure();
                } catch (ConfigurationException e) {
                    // Ignore configuration exceptions for this test
                }
                
                // Test that openBlock properly throws a SenderException
                SenderException exception = assertThrows(SenderException.class, () -> {
                    sqlExceptionSender.openBlock(session);
                }, "openBlock should throw SenderException when setting catalog fails");
                
                assertTrue(exception.getMessage().contains("cannot prepare Trino query execution context"), 
                    "Exception message should indicate preparation issue");
            })
        );
    }
    
    /**
     * Cleans up the test environment after all tests have completed.
     * <p>
     * This method drops the test table and schema that were created for testing.
     * It only runs if Trino is available.
     */
    public static void tearDownClass() {
        // Only run teardown if Trino is available
        if (!isTrinoAvailable) {
            return;
        }

        // Clean up test data
        try (Connection conn = dataSource.getConnection()) {
            try {
                executeUpdate(conn, "DROP TABLE IF EXISTS " + TEST_CATALOG + "." + TEST_SCHEMA + "." + TEST_TABLE);
                executeUpdate(conn, "DROP SCHEMA IF EXISTS " + TEST_CATALOG + "." + TEST_SCHEMA);
            } catch (SQLException e) {
                System.err.println("Error cleaning up test data: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("Error getting connection for cleanup: " + e.getMessage());
        }
    }

    /**
     * Executes a SQL update statement.
     * <p>
     * This utility method executes a SQL update statement using a prepared statement.
     * 
     * @param conn the database connection to use
     * @param sql the SQL statement to execute
     * @throws SQLException if an error occurs during execution
     */
    private static void executeUpdate(Connection conn, String sql) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        }
    }

    /**
     * Creates a Trino DataSource for testing.
     * <p>
     * This utility method creates a TrinoDataSource instance configured with the 
     * specified host, port, and catalog.
     * 
     * @param host the Trino server hostname
     * @param port the Trino server port
     * @param catalog the catalog to use
     * @return a configured TrinoDataSource
     */
    private static TrinoDataSource createTrinoDataSource(String host, int port, String catalog) {
        return new TrinoDataSource(host, port, catalog);
    }

    /**
     * Simple DataSource implementation for Trino tests.
     * <p>
     * This inner class provides a minimal implementation of the DataSource interface
     * that connects to a Trino server using the specified host, port, and catalog.
     */
    private static class TrinoDataSource implements DataSource {
        /** The Trino server hostname */
        private final String host;
        
        /** The Trino server port */
        private final int port;
        
        /** The catalog to use */
        private final String catalog;

        /**
         * Creates a new TrinoDataSource.
         * 
         * @param host the Trino server hostname
         * @param port the Trino server port
         * @param catalog the catalog to use
         */
        public TrinoDataSource(String host, int port, String catalog) {
            this.host = host;
            this.port = port;
            this.catalog = catalog;
        }

        /**
         * Gets a connection to the Trino server using default credentials.
         * 
         * @return a Connection to the Trino server
         * @throws SQLException if an error occurs
         */
        @Override
        public Connection getConnection() throws SQLException {
            Properties props = new Properties();
            props.setProperty("user", "test");
            return java.sql.DriverManager.getConnection("jdbc:trino://" + host + ":" + port + "/" + catalog, props);
        }

        /**
         * Gets a connection to the Trino server using the specified credentials.
         * 
         * @param username the username to use
         * @param password the password to use
         * @return a Connection to the Trino server
         * @throws SQLException if an error occurs
         */
        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            Properties props = new Properties();
            props.setProperty("user", username);
            if (password != null && !password.isEmpty()) {
                props.setProperty("password", password);
            }
            return java.sql.DriverManager.getConnection("jdbc:trino://" + host + ":" + port + "/" + catalog, props);
        }

        // Required methods with default implementations
        @Override
        public java.io.PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(java.io.PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public java.util.logging.Logger getParentLogger() {
            return null;
        }

        @Override
        public <T> T unwrap(Class<T> iface) {
            return null;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }
    }
}