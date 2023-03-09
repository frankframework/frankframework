/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.management.bus.endpoints;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Transformer;

import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.Message;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.jdbc.DirectQuerySender;
import nl.nn.adapterframework.jdbc.dbms.Dbms;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;
import nl.nn.adapterframework.jdbc.transformer.QueryOutputToListOfMaps;
import nl.nn.adapterframework.jndi.JndiDataSourceFactory;
import nl.nn.adapterframework.management.bus.ActionSelector;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusAware;
import nl.nn.adapterframework.management.bus.BusException;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.ResponseMessage;
import nl.nn.adapterframework.management.bus.TopicSelector;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DB2XMLWriter;
import nl.nn.adapterframework.util.XmlEncodingUtils;
import nl.nn.adapterframework.util.XmlUtils;

@BusAware("frank-management-bus")
public class BrowseJdbcTable extends BusEndpointBase {

	private static final String DB2XML_XSLT = "xml/xsl/BrowseJdbcTableExecute.xsl";
	private static final String JDBC_PERMISSION_RULES = AppConstants.getInstance().getResolvedProperty("browseJdbcTable.permission.rules");
	private static final String COLUMN_NAME = "COLUMN_NAME";
	private static final String DATA_TYPE = "DATA_TYPE";
	private static final String COLUMN_SIZE = "COLUMN_SIZE";
	private String countColumnName = "ROWCOUNTER";
	private String rnumColumnName = "RNUM";

	@TopicSelector(BusTopic.JDBC)
	@ActionSelector(BusAction.FIND)
	public Message<String> handleBrowseDatabase(Message<?> message) {
		String datasource = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, JndiDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME);
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

		return doAction(datasource, tableName, where, order, numberOfRowsOnly, minRow, maxRow);
	}

	private Message<String> doAction(String datasource, String table, String where, String order, boolean numberOfRowsOnly, int minRow, int maxRow) {
		Map<String, Object> fieldDef = new LinkedHashMap<>();
		String result = "";
		String query = null;

		DirectQuerySender qs = createBean(DirectQuerySender.class);

		try {
			qs.setName("BrowseTable QuerySender");
			qs.setDatasourceName(datasource);

			qs.setQueryType("select");
			qs.setSqlDialect("Oracle");
			qs.setBlobSmartGet(true);
			qs.setIncludeFieldDefinition(true);
			qs.configure(true);
			qs.open();

			StringBuilder fielddefinition = new StringBuilder("<fielddefinition>");
			String firstColumnName = numberOfRowsOnly ? countColumnName : rnumColumnName;
			String field = "<field name=\""+firstColumnName+"\" type=\"INTEGER\" />";
			fielddefinition.append(field);
			fieldDef.put(firstColumnName, "INTEGER");
			IDbmsSupport dbmsSupport = qs.getDbmsSupport();
			if(!numberOfRowsOnly || StringUtils.isNotEmpty(order)) {
				try (Connection conn =qs.getConnection()) {
					try (ResultSet rs = numberOfRowsOnly ? dbmsSupport.getTableColumns(conn, null, table, order) : dbmsSupport.getTableColumns(conn, table)) {
						while(rs != null && rs.next()) {
							field = "<field name=\"" + rs.getString(COLUMN_NAME).toUpperCase() + "\" type=\"" + DB2XMLWriter.getFieldType(rs.getInt(DATA_TYPE)) + "\" size=\"" + rs.getInt(COLUMN_SIZE) + "\"/>";
							fielddefinition.append(field);
							fieldDef.put(rs.getString(COLUMN_NAME).toUpperCase(), DB2XMLWriter.getFieldType(rs.getInt(DATA_TYPE)) + "("+rs.getInt(COLUMN_SIZE)+")");
						}
					}
				}
			}
			fielddefinition.append("</fielddefinition>");

			String browseJdbcTableExecuteREQ = browseJdbcTableExecuteREQ(dbmsSupport.getDbms(), table, where, order, numberOfRowsOnly, minRow, maxRow, fielddefinition.toString());
			URL url = ClassUtils.getResourceURL(DB2XML_XSLT);
			if (url != null) {
				Transformer t = XmlUtils.createTransformer(url);
				query = XmlUtils.transformXml(t, browseJdbcTableExecuteREQ);
			}
			result = qs.sendMessageOrThrow(new nl.nn.adapterframework.stream.Message(query), null).asString();
		} catch (Exception t) {
			throw new BusException("an error occured on executing jdbc query ["+query+"]", t);
		} finally {
			qs.close();
		}

		List<Map<String, String>> resultMap = null;
		if(XmlUtils.isWellFormed(result)) {
			try {
				resultMap = new QueryOutputToListOfMaps().parseString(result);
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
		resultObject.put("fielddefinition", fieldDef);
		resultObject.put("result", resultMap);

		return ResponseMessage.ok(resultObject);
	}

	private String browseJdbcTableExecuteREQ(Dbms dbms, String table, String where, String order, boolean numberOfRowsOnly, int minRow, int maxRow, String fieldDefinition) {
		return	"<browseJdbcTableExecuteREQ>"
				+ "<dbmsName>"
				+ dbms.getKey()
				+ "</dbmsName>"
				+ "<countColumnName>"
				+ countColumnName
				+ "</countColumnName>"
				+ "<rnumColumnName>"
				+ rnumColumnName
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
