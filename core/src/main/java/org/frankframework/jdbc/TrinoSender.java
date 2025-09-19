/*
   Copyright 2024 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.dbms.JdbcException;
import org.frankframework.stream.Message;

import lombok.Getter;

/**
 * Specialized JDBC sender for executing SQL queries against Trino databases.
 * <p>
 * The TrinoSender provides enhanced functionality specifically optimized for Trino database 
 * connections. It handles catalog and schema management, session property configuration,
 * and optimizes query execution for the Trino environment.
 * <p>
 * Key features include:
 * <ul>
 *   <li>Automatic catalog and schema prefixing for table references</li>
 *   <li>Support for Trino-specific session properties</li>
 *   <li>Batch processing capabilities</li>
 *   <li>Smart query type detection and optimization</li>
 * </ul>
 * <p>
 * This sender uses a fixed query approach, meaning the same query is executed
 * for each message with only parameter values changing between executions.
 *
 * @ff.info Designed specifically for Trino JDBC connections
 * @ff.parameters All parameters present are applied to the query to be executed
 *
 * @author Daan Bom
 */
public class TrinoSender extends AbstractJdbcQuerySender<QueryExecutionContext> {

    /**
     * The SQL query text to be executed for each message.
     * This query can contain named parameters if useNamedParams is enabled.
     */
    private @Getter String query;
    
    /**
     * The default catalog to use for Trino queries.
     * This is automatically prefixed to table references when enabled.
     */
    private @Getter String catalog;
    
    /**
     * The default schema to use for Trino queries.
     * This is automatically prefixed to table references when enabled.
     */
    private @Getter String schema;
    
    /**
     * Session properties for the Trino connection in format: "key1=value1,key2=value2".
     * These properties are applied directly to the Trino connection.
     */
    private @Getter String sessionProperties;
    
    /**
     * Controls whether automatic query prefixing with catalog and schema is disabled.
     * When false (default), the sender will attempt to add catalog and schema prefixes
     * to table references in the query.
     */
    private @Getter boolean disableQueryPrefixing = false;
    
    /**
     * Batch size for query execution when used with an IteratingPipe.
     * When set larger than 0, database calls are made in batches of this size.
     * Only applies for queryType=other.
     */
    private @Getter int batchSize;
    
    /**
     * Pattern for detecting SELECT queries.
     * Used for identifying query type to apply appropriate prefixing.
     */
    private static final Pattern SELECT_PATTERN = Pattern.compile("\\s*SELECT\\s+", Pattern.CASE_INSENSITIVE);
    
    /**
     * Pattern for detecting INSERT queries.
     * Used for identifying query type to apply appropriate prefixing.
     */
    private static final Pattern INSERT_PATTERN = Pattern.compile("\\s*INSERT\\s+INTO\\s+", Pattern.CASE_INSENSITIVE);
    
    /**
     * Pattern for detecting UPDATE queries.
     * Used for identifying query type to apply appropriate prefixing.
     */
    private static final Pattern UPDATE_PATTERN = Pattern.compile("\\s*UPDATE\\s+", Pattern.CASE_INSENSITIVE);
    
    /**
     * Pattern for detecting DELETE queries.
     * Used for identifying query type to apply appropriate prefixing.
     */
    private static final Pattern DELETE_PATTERN = Pattern.compile("\\s*DELETE\\s+FROM\\s+", Pattern.CASE_INSENSITIVE);
    
    /**
     * Configures this TrinoSender instance.
     * <p>
     * Validates that required properties are set and performs initial setup:
     * <ul>
     *   <li>Verifies that a query is specified</li>
     *   <li>Automatically enables named parameters if UNP_START tokens are found in the query</li>
     *   <li>Warns about missing catalog/schema references in the query if they're configured</li>
     * </ul>
     *
     * @throws ConfigurationException if the configuration is invalid (e.g., missing required query)
     */
    @Override
    public void configure() throws ConfigurationException {
        if (StringUtils.isEmpty(getQuery())) {
            throw new ConfigurationException("query must be specified for TrinoSender");
        }
        
        if (getUseNamedParams() == null && getQuery().contains(UNP_START)) {
            setUseNamedParams(true);
        }
        
        super.configure();
        
        if (StringUtils.isNotEmpty(getCatalog()) && StringUtils.isNotEmpty(getSchema())) {
            String lowerQuery = getQuery().toLowerCase();
            
            // Only check if the query seems to be a type that would benefit from catalog/schema
            if ((lowerQuery.contains(" from ") || lowerQuery.contains("insert into ")) && 
                !lowerQuery.contains("." + getSchema().toLowerCase() + ".") && 
                !lowerQuery.contains(getCatalog().toLowerCase() + ".")) {
                log.warn("Query does not explicitly reference the configured catalog [{}] or schema [{}]. Consider adding them to your query for clarity.", getCatalog(), getSchema());
            }
        }
    }

    /**
     * Returns the SQL query to be executed.
     * <p>
     * For TrinoSender, this is always the fixed query configured via the setQuery method,
     * regardless of the message content.
     *
     * @param message The message being processed (ignored in this implementation)
     * @return The fixed SQL query string
     */
    @Override
    protected String getQuery(Message message) {
        return getQuery();
    }
    
    /**
     * Prepares a SQL statement for execution against a Trino database.
     * <p>
     * This method applies Trino-specific optimizations:
     * <ul>
     *   <li>Prefixes queries with catalog and schema information when appropriate</li>
     *   <li>Applies any configured session properties to the connection</li>
     *   <li>Uses read-only result sets since Trino doesn't support updatable result sets</li>
     * </ul>
     *
     * @param con The database connection
     * @param query The SQL query to prepare
     * @param queryType The type of query being executed
     * @return A prepared statement ready for execution
     * @throws SQLException If a database access error occurs
     * @throws JdbcException If a JDBC-specific error occurs
     */
    @Override
    protected PreparedStatement prepareQuery(Connection con, String query, QueryType queryType) throws SQLException, JdbcException {
        // Apply Trino-specific optimizations to the query
        String optimizedQuery = query;
        
        if (!isDisableQueryPrefixing()) {
            optimizedQuery = prefixQueryWithCatalogAndSchema(optimizedQuery);
        }
        
        // Apply any session properties
        if (StringUtils.isNotEmpty(getSessionProperties())) {
            applySessionProperties(con);
        }
        String convertedQuery = convertQuery(optimizedQuery);
        PreparedStatement stmt = con.prepareStatement(convertedQuery,java.sql.ResultSet.TYPE_FORWARD_ONLY, 
                java.sql.ResultSet.CONCUR_READ_ONLY);
        return stmt;

    }
    
    /**
     * Modifies the query to include catalog and schema if they're not already specified.
     * <p>
     * This method analyzes the query type (SELECT, INSERT, UPDATE, DELETE) and
     * intelligently adds catalog and schema prefixes to table references where appropriate.
     * It avoids adding duplicates if the table reference is already fully qualified.
     *
     * @param query The original SQL query
     * @return The modified query with catalog and schema prefixes added where needed
     */
    protected String prefixQueryWithCatalogAndSchema(String query) {
        if (StringUtils.isEmpty(getCatalog()) || StringUtils.isEmpty(getSchema())) {
            return query;
        }
        
        String trimmedQuery = query.trim();
        
        // Handle different query types
        if (SELECT_PATTERN.matcher(trimmedQuery).lookingAt()) {
            return handleSelectOrDeleteQuery(query);
        } else if (INSERT_PATTERN.matcher(trimmedQuery).lookingAt()) {
            return handleInsertQuery(query);
        } else if (UPDATE_PATTERN.matcher(trimmedQuery).lookingAt()) {
            return handleUpdateQuery(query);
        } else if (DELETE_PATTERN.matcher(trimmedQuery).lookingAt()) {
            return handleSelectOrDeleteQuery(query);
        }
        
        // For unrecognized query types, return as is
        return query;
    }
    
    /**
     * Handles catalog/schema prefixing specifically for SELECT queries.
     * <p>
     * Locates the FROM clause and adds catalog.schema prefixes to table references
     * that appear after the FROM keyword.
     *
     * @param query The original SELECT query
     * @return The modified query with catalog.schema prefixes added where needed
     */
    private String handleSelectOrDeleteQuery(String query) {
        String lowerQuery = query.toLowerCase();
        int fromIndex = lowerQuery.indexOf(" from ");
        
        if (fromIndex < 0) {
            return query;
        }
        
        return addCatalogSchemaToTableReference(query, fromIndex + 6);
    }
    
    /**
     * Handles catalog/schema prefixing specifically for INSERT queries.
     * <p>
     * Locates the INTO clause and adds catalog.schema prefixes to table references
     * that appear after the INTO keyword.
     *
     * @param query The original INSERT query
     * @return The modified query with catalog.schema prefixes added where needed
     */
    private String handleInsertQuery(String query) {
        String lowerQuery = query.toLowerCase();
        int intoIndex = lowerQuery.indexOf(" into ");
        
        if (intoIndex < 0) {
            return query;
        }
        
        return addCatalogSchemaToTableReference(query, intoIndex + 6);
    }
    
    /**
     * Handles catalog/schema prefixing specifically for UPDATE queries.
     * <p>
     * Locates the table reference after the UPDATE keyword and adds catalog.schema
     * prefixes where needed.
     *
     * @param query The original UPDATE query
     * @return The modified query with catalog.schema prefixes added where needed
     */
    private String handleUpdateQuery(String query) {
        String lowerQuery = query.toLowerCase();
        int updateIndex = lowerQuery.indexOf("update ");
        
        if (updateIndex < 0) {
            return query;
        }
        
        return addCatalogSchemaToTableReference(query, updateIndex + 7);
    }
    
    /**
     * Common helper method to add catalog.schema to a table reference.
     * <p>
     * This method:
     * <ul>
     *   <li>Locates the table reference in the query at the specified position</li>
     *   <li>Determines if the table is already fully qualified (has catalog.schema)</li>
     *   <li>Adds the catalog.schema prefix if the table is not already qualified</li>
     * </ul>
     *
     * @param query The original SQL query
     * @param tableStart The starting position of the table reference in the query
     * @return The modified query with catalog.schema prefix added if needed
     */
    private String addCatalogSchemaToTableReference(String query, int tableStart) {
        // Skip whitespace
        while (tableStart < query.length() && Character.isWhitespace(query.charAt(tableStart))) {
            tableStart++;
        }
        
        // Find the table reference (which might include catalog and schema)
        int tableEnd = tableStart;
        while (tableEnd < query.length() && 
               !Character.isWhitespace(query.charAt(tableEnd)) &&
               query.charAt(tableEnd) != ',' && 
               query.charAt(tableEnd) != '(' &&
               query.charAt(tableEnd) != ')') {
            tableEnd++;
        }
        
        // Get the table reference
        String tableRef = query.substring(tableStart, tableEnd);
        
        // Count the dots to see if this is already a qualified name
        // If it has two dots (catalog.schema.table), it's already fully qualified
        int dotCount = 0;
        for (int i = 0; i < tableRef.length(); i++) {
            if (tableRef.charAt(i) == '.') {
                dotCount++;
            }
        }
        
        // If there are two or more dots, this is already fully qualified
        if (dotCount >= 2) {
            return query;
        }
        
        // Alternatively, check if it contains either catalog or schema explicitly
        if (tableRef.toLowerCase().contains(getCatalog().toLowerCase() + ".") || 
            tableRef.toLowerCase().contains("." + getSchema().toLowerCase())) {
            return query;
        }
        
        // Insert catalog.schema
        String prefix = query.substring(0, tableStart);
        String suffix = query.substring(tableEnd);
        
        return prefix + getCatalog() + "." + getSchema() + "." + tableRef + suffix;
    }
    
    /**
     * Applies Trino-specific session properties to the connection.
     * <p>
     * This method parses the sessionProperties string in the format "key1=value1,key2=value2"
     * and applies each property to the Trino connection if the connection can be unwrapped
     * to a TrinoConnection.
     *
     * @param con The database connection
     * @throws SQLException If a database access error occurs or the connection cannot be unwrapped
     */
    protected void applySessionProperties(Connection con) throws SQLException {
        if (StringUtils.isEmpty(getSessionProperties())) {
            return;
        }
        
        String[] propertyPairs = getSessionProperties().split(",");
        
        if (con.isWrapperFor(io.trino.jdbc.TrinoConnection.class)) {
            io.trino.jdbc.TrinoConnection trinoConnection = con.unwrap(io.trino.jdbc.TrinoConnection.class);
            
            for (String pair : propertyPairs) {
                String[] keyValue = pair.trim().split("=");
                if (keyValue.length == 2) {
                    trinoConnection.setSessionProperty(keyValue[0].trim(), keyValue[1].trim());
                }
            }
        }
    }

    /**
     * Opens a new query execution block for sending multiple messages.
     * <p>
     * This method:
     * <ul>
     *   <li>Gets a database connection</li>
     *   <li>Sets the initial catalog and schema if provided</li>
     *   <li>Applies any session properties</li>
     *   <li>Creates a QueryExecutionContext for statement execution</li>
     *   <li>Initializes batch processing if batchSize > 0</li>
     * </ul>
     *
     * @param session The current pipeline session
     * @return A QueryExecutionContext containing the prepared resources for executing queries
     * @throws SenderException If preparation fails
     * @throws TimeoutException If a timeout occurs while preparing
     */
    @Override
    public QueryExecutionContext openBlock(PipeLineSession session) throws SenderException, TimeoutException {
        try {
            Connection connection = getConnectionForSendMessage();
            QueryExecutionContext result;
            
            try {
                // Set the initial catalog and schema on the connection if provided
                if (StringUtils.isNotEmpty(getCatalog())) {
                    connection.setCatalog(getCatalog());
                }
                
                if (StringUtils.isNotEmpty(getSchema())) {
                    connection.setSchema(getSchema());
                }
                
                // Apply any session properties
                if (StringUtils.isNotEmpty(getSessionProperties())) {
                    applySessionProperties(connection);
                }
                
                QueryExecutionContext result1 = getQueryExecutionContext(connection, null);
                if (getBatchSize() > 0) {
                    result1.getStatement().clearBatch();
                }
                result = result1;
            } catch (JdbcException | SQLException e) {
                closeConnectionForSendMessage(connection, session);
                throw new SenderException("cannot prepare Trino query execution context", e);
            }
            return result;
        } catch (JdbcException e) {
            throw new SenderException("cannot get connection for Trino", e);
        }
    }

    /**
     * Closes a query execution block when finished.
     * <p>
     * This method:
     * <ul>
     *   <li>Safely closes the statement if present</li>
     *   <li>Safely closes the connection if present</li>
     * </ul>
     * <p>
     * Any exceptions during closing are logged but not propagated.
     *
     * @param blockHandle The QueryExecutionContext to close
     * @param session The current pipeline session
     */
    @Override
    public void closeBlock(QueryExecutionContext blockHandle, PipeLineSession session) {
    	if (blockHandle == null) {
            return; // Early return if blockHandle is null
        }
    	try {
            // Only call closeStatementSet if we have a valid statement
            if (blockHandle.getStatement() != null) {
                super.closeStatementSet(blockHandle);
            }
        } catch (Exception e) {
            log.warn("Unhandled exception closing statement-set", e);
        }
        
        // Only try to close the connection if it's not null
        if (blockHandle.getConnection() != null) {
            closeConnectionForSendMessage(blockHandle.getConnection(), session);
        }
    }

    /**
     * Closes a statement set after execution.
     * <p>
     * If batch processing is enabled (batchSize > 0), this method executes
     * any pending batch operations before closing.
     *
     * @param statementSet The statement set to close
     */
    @Override
    protected void closeStatementSet(QueryExecutionContext statementSet) {
    	// Check if batch size is set and there is a valid statement before executing batch
        if (statementSet != null && getBatchSize() > 0 && statementSet.getStatement() != null) {
            try {
                statementSet.getStatement().executeBatch();
            } catch (SQLException e) {
                log.warn("Got exception executing batch in Trino closeStatementSet", e);
            }
        }
        // Don't close the statement here - postpone to closeBlock()
    }

    /**
     * Sends a message by executing the prepared query.
     * <p>
     * This method takes the prepared QueryExecutionContext and executes the query
     * with the parameters from the current message.
     *
     * @param blockHandle The prepared QueryExecutionContext
     * @param message The message containing parameters for the query
     * @param session The current pipeline session
     * @return A SenderResult containing the execution result
     * @throws SenderException If execution fails
     * @throws TimeoutException If a timeout occurs during execution
     */
    @Override
    public SenderResult sendMessage(QueryExecutionContext blockHandle, Message message, PipeLineSession session) throws SenderException, TimeoutException {
        return executeStatementSet(blockHandle, message, session);
    }

    /**
     * Sets the SQL query text to be executed each time sendMessage() is called.
     * <p>
     * This is the core query that will be executed, potentially with parameters
     * that are substituted from each message.
     * 
     * @param query The SQL query text
     * @ff.mandatory
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * Sets the default catalog to use for Trino queries.
     * <p>
     * This catalog will be automatically prefixed to table references in queries
     * when automatic prefixing is enabled.
     * 
     * @param catalog The Trino catalog name
     */
    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    /**
     * Sets the default schema to use for Trino queries.
     * <p>
     * This schema will be automatically prefixed to table references in queries
     * when automatic prefixing is enabled.
     * 
     * @param schema The Trino schema name
     */
    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * Sets session properties for the Trino connection.
     * <p>
     * Session properties should be specified in the format: "key1=value1,key2=value2"
     * 
     * @param sessionProperties The session properties string
     */
    public void setSessionProperties(String sessionProperties) {
        this.sessionProperties = sessionProperties;
    }
    
    /**
     * Disables automatic query prefixing with catalog and schema.
     * <p>
     * When set to true, the sender will not attempt to add catalog and schema
     * prefixes to table references in queries.
     * 
     * @param disableQueryPrefixing Whether to disable query prefixing
     * @ff.default false
     */
    public void setDisableQueryPrefixing(boolean disableQueryPrefixing) {
        this.disableQueryPrefixing = disableQueryPrefixing;
    }
    
    /**
     * Sets the batch size for query execution.
     * <p>
     * When set larger than 0 and used as a child of an IteratingPipe,
     * database calls are made in batches of this size. This can improve
     * performance for bulk operations.
     * <p>
     * Only applies for queryType=other.
     * 
     * @param batchSize The batch size
     * @ff.default 0
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}