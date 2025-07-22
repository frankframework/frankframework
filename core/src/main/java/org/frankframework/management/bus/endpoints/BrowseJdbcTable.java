/*
   Copyright 2022-2024 WeAreFrank!

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
package org.frankframework.management.bus.endpoints;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;

import jakarta.annotation.security.RolesAllowed;

import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.Message;
import org.xml.sax.SAXException;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.dbms.Dbms;
import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.dbms.JdbcException;
import org.frankframework.jdbc.AbstractJdbcQuerySender;
import org.frankframework.jdbc.DirectQuerySender;
import org.frankframework.jdbc.IDataSourceFactory;
import org.frankframework.jdbc.transformer.QueryOutputToListOfMaps;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.management.bus.ActionSelector;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusAware;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.TopicSelector;
import org.frankframework.management.bus.message.JsonMessage;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.XmlEncodingUtils;
import org.frankframework.util.XmlUtils;

@Log4j2
@BusAware("frank-management-bus")
public class BrowseJdbcTable extends BusEndpointBase {

	private static final String DB2XML_XSLT = "xml/xsl/BrowseJdbcTableExecute.xsl";
	private static final String JDBC_PERMISSION_RULES = AppConstants.getInstance().getProperty("browseJdbcTable.permission.rules");
	private static final String COLUMN_NAME = "COLUMN_NAME";
	private static final String DATA_TYPE = "DATA_TYPE";
	private static final String COLUMN_SIZE = "COLUMN_SIZE";
	private static final String COUNT_COLUMN_NAME = "ROWCOUNTER";
	private static final String RNUM_COLUMN_NAME = "RNUM";
	private Transformer transformer = null;

	@Override
	protected void doAfterPropertiesSet() {
		URL url = ClassLoaderUtils.getResourceURL(DB2XML_XSLT);
		if (url != null) { //Should never be null but...
			try {
				transformer = XmlUtils.createTransformer(url, 2);
			} catch (TransformerConfigurationException | IOException e) {
				log.error("unable to create Transformer", e);
			}
		}
	}

	@TopicSelector(BusTopic.JDBC)
	@ActionSelector(BusAction.FIND)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public Message<String> handleBrowseDatabase(Message<?> message) {
		String datasource = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME);
		String tableName = BusMessageUtils.getHeader(message, "table");
		String where = BusMessageUtils.getHeader(message, "where");
		String order = BusMessageUtils.getHeader(message, "order"); // the form field named 'order' is only used for 'group by', when number of rows only is true.
		boolean numberOfRowsOnly = BusMessageUtils.getBooleanHeader(message, "numberOfRowsOnly", false);
		int minRow = Math.max(BusMessageUtils.getIntHeader(message, "minRow", 1), 0);
		int maxRow = Math.max(BusMessageUtils.getIntHeader(message, "maxRow", 100), 1);

		if (!readAllowed(tableName)) {
			throw new BusException("Access to table ["+tableName+"] not allowed");
		}

		if(maxRow < minRow)
			throw new BusException("Rownum max must be greater than or equal to Rownum min");
		if (maxRow - minRow >= 100) {
			throw new BusException("Difference between Rownum max and Rownum min must be less than hundred");
		}

		if (transformer == null) {
			throw new BusException("unable to create query transformer ["+DB2XML_XSLT+"]");
		}

		return doAction(datasource, tableName, where, order, numberOfRowsOnly, minRow, maxRow);
	}

	private Message<String> doAction(String datasource, String table, String where, String order, boolean numberOfRowsOnly, int minRow, int maxRow) {
		final List<RowDefinition> rowDefinitions;
		final org.frankframework.stream.Message result;
		final String query;

		DirectQuerySender qs = createQuerySender(datasource);

		try {
			rowDefinitions = getFieldDefinitions(qs, numberOfRowsOnly, table, order);
			StringBuilder fielddefinition = new StringBuilder("<fielddefinition>");
			rowDefinitions.stream().map(RowDefinition::xmlValue).forEach(fielddefinition::append);
			fielddefinition.append("</fielddefinition>");

			String browseJdbcTableExecuteREQ = browseJdbcTableExecuteREQ(qs.getDbmsSupport().getDbms(), table, where, order, numberOfRowsOnly, minRow, maxRow, fielddefinition.toString());
			query = XmlUtils.transformXml(transformer, browseJdbcTableExecuteREQ);
			try(PipeLineSession session = new PipeLineSession()) {
				result = qs.sendMessageOrThrow(new org.frankframework.stream.Message(query), session);
			} catch (Exception t) {
				throw new BusException("an error occurred on executing jdbc query ["+query+"]", t);
			}
		} catch (Exception t) {
			throw new BusException("an error occurred while determining query to execute", t);
		} finally {
			qs.stop();
		}

		List<Map<String, String>> resultMap = null;
		if(XmlUtils.isWellFormed(result, null)) {
			try {
				resultMap = new QueryOutputToListOfMaps().parseMessage(result);
			} catch (IOException | SAXException e) {
				throw new BusException("query result could not be parsed", e);
			}
		}
		if(resultMap == null) {
			throw new BusException("invalid query result [null]");
		}

		Map<String, Object> resultObject = new HashMap<>();
		resultObject.put("table", table);
		resultObject.put("query", XmlEncodingUtils.encodeChars(query));
		Map<String, String> fDef = rowDefinitions.stream() //confusing collector but this maintains insert order.
				.collect(Collectors.toMap(RowDefinition::name, RowDefinition::jsonValue, (t, u) -> t, LinkedHashMap::new));
		resultObject.put("fielddefinition", fDef);
		resultObject.put("result", resultMap);

		return new JsonMessage(resultObject);
	}

	private List<RowDefinition> getFieldDefinitions(DirectQuerySender qs, boolean numberOfRowsOnly, String table, String order) throws SQLException, JdbcException {
		List<RowDefinition> rowDefinitions = new ArrayList<>();
		rowDefinitions.add(new RowDefinition(numberOfRowsOnly ? COUNT_COLUMN_NAME : RNUM_COLUMN_NAME, JDBCType.INTEGER, 0));

		IDbmsSupport dbmsSupport = qs.getDbmsSupport();
		if(!numberOfRowsOnly || StringUtils.isNotEmpty(order)) {
			try (Connection conn = qs.getConnection();
					ResultSet rs = numberOfRowsOnly ? dbmsSupport.getTableColumns(conn, null, table, order) : dbmsSupport.getTableColumns(conn, table)) {
				while(rs != null && rs.next()) {
					rowDefinitions.add(new RowDefinition(rs.getString(COLUMN_NAME), rs.getInt(DATA_TYPE), rs.getInt(COLUMN_SIZE)));
				}
			}
		}
		return rowDefinitions;
	}

	private record RowDefinition(String name, JDBCType type, int size) {

		private RowDefinition(String name, int type, int size) {
			this(name.toUpperCase(), JDBCType.valueOf(type), size);
		}

		public String jsonValue() {
			return String.format("%s(%d)", type.getName(), size);
		}

		public String xmlValue() {
			return String.format("<field name=\"%s\" type=\"%s\" size=\"%d\" />", name, type.getName(), size);
		}
	}

	private DirectQuerySender createQuerySender(String datasource) {
		DirectQuerySender qs = createBean(DirectQuerySender.class);
		try {
			qs.setName("BrowseTable QuerySender");
			qs.setDatasourceName(datasource);

			qs.setQueryType(AbstractJdbcQuerySender.QueryType.SELECT);
			qs.setSqlDialect("Oracle");
			qs.setBlobSmartGet(true);
			qs.setIncludeFieldDefinition(true);
			qs.configure(true);
			qs.start();
			return qs;
		} catch (ConfigurationException | LifecycleException e) {
			throw new BusException("unable to create QuerySender", e);
		}
	}

	private String browseJdbcTableExecuteREQ(Dbms dbms, String table, String where, String order, boolean numberOfRowsOnly, int minRow, int maxRow, String fieldDefinition) {
		return	"<browseJdbcTableExecuteREQ>"
				+ "<dbmsName>"
				+ dbms.getKey()
				+ "</dbmsName>"
				+ "<countColumnName>"
				+ COUNT_COLUMN_NAME
				+ "</countColumnName>"
				+ "<rnumColumnName>"
				+ RNUM_COLUMN_NAME
				+ "</rnumColumnName>"
				+ "<tableName>"
				+ table
				+ "</tableName>"
				+ "<where>"
				+ (where==null? "": XmlEncodingUtils.encodeChars(where))
				+ "</where>"
				+ "<numberOfRowsOnly>"
				+ numberOfRowsOnly
				+ "</numberOfRowsOnly>"
				+ "<order>"
				+ (order==null?"":order)
				+ "</order>"
				+ "<rownumMin>"
				+ minRow
				+ "</rownumMin>"
				+ "<rownumMax>"
				+ maxRow
				+ "</rownumMax>"
				+ fieldDefinition
				+ "<maxColumnSize>1000</maxColumnSize>"
				+ "</browseJdbcTableExecuteREQ>";
	}

	private boolean readAllowed(String tableName) {
		if(tableName == null) return false;

		String table = tableName.toLowerCase();
		List<String> rulesList = Arrays.asList(JDBC_PERMISSION_RULES.split("\\|"));
		for (String rule: rulesList) {
			List<String> parts = Arrays.asList(rule.trim().split("\\s+"));
			if (parts.size() != 3) {
				log.debug("invalid rule [{}] contains {} part(s): {}", rule, parts.size(), parts);
				continue;
			}

			String tablePattern = parts.get(0).toLowerCase();
			if (tablePattern != null) {
				String role = parts.get(1);
				String type = parts.get(2);
				log.debug("check allow read table [{}] with rule table [{}] role [{}] and type [{}]", table, tablePattern, role, type);
				if ("*".equals(tablePattern) || table.equals(tablePattern)) {
					log.debug("table match");
					if ("*".equals(role) || BusMessageUtils.hasRole(role)) {
						log.debug("role match, type [{}]", type);
						if ("allow".equals(type)) {
							return true;
						} else if ("deny".equals(type)) {
							return false;
						} else {
							log.error("invalid rule type");
						}
					}
				}
			}
		}

		log.debug("deny");
		return false;
	}
}
