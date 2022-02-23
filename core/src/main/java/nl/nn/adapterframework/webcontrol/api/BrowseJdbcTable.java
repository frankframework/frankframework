/*
Copyright 2016-2021 WeAreFrank!

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
package nl.nn.adapterframework.webcontrol.api;

import nl.nn.adapterframework.jdbc.DirectQuerySender;
import nl.nn.adapterframework.jdbc.transformer.QueryOutputToListOfMaps;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DB2XMLWriter;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;
import org.xml.sax.SAXException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.transform.Transformer;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Path("/")
public final class BrowseJdbcTable extends Base {

	private static final String DB2XML_XSLT = "xml/xsl/BrowseJdbcTableExecute.xsl";
	private static final String permissionRules = AppConstants.getInstance().getResolvedProperty("browseJdbcTable.permission.rules");
	private static final String COLUMN_NAME = "COLUMN_NAME"; 
	private static final String DATA_TYPE = "DATA_TYPE"; 
	private static final String COLUMN_SIZE = "COLUMN_SIZE"; 
	private Logger log = LogUtil.getLogger(this);
	private String countColumnName = "ROWCOUNTER";
	private String rnumColumnName = "RNUM";

	@POST
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/jdbc/browse")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response execute(LinkedHashMap<String, Object> json) throws ApiException {
		String datasource = null, tableName = null, where = "", order = "";
		Boolean numberOfRowsOnly = false;
		int minRow = 1, maxRow = 100;

		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			if(key.equalsIgnoreCase("datasource")) {
				datasource = entry.getValue().toString();
			}
			if(key.equalsIgnoreCase("table")) {
				tableName = entry.getValue().toString();
			}
			if(key.equalsIgnoreCase("where")) {
				where = entry.getValue().toString();
			}
			if(key.equalsIgnoreCase("order")) {
				order = entry.getValue().toString();
			}
			if(key.equalsIgnoreCase("numberOfRowsOnly")) {
				numberOfRowsOnly = Boolean.parseBoolean(entry.getValue().toString());
			}
			if(key.equalsIgnoreCase("minRow")) {
				if(entry.getValue() != "") {
					minRow = Integer.parseInt(entry.getValue().toString());
					minRow = Math.max(minRow, 0);
				}
			}
			if(key.equalsIgnoreCase("maxRow")) {
				if(entry.getValue() != "") {
					maxRow = Integer.parseInt(entry.getValue().toString());
					maxRow = Math.max(maxRow, 1);
				}
			}
		}

		if(datasource == null || tableName == null) {
			throw new ApiException("datasource and/or tableName not defined.", 400);
		}

		if(maxRow < minRow)
			throw new ApiException("Rownum max must be greater than or equal to Rownum min", 400);
		if (maxRow - minRow >= 100) {
			throw new ApiException("Difference between Rownum max and Rownum min must be less than hundred", 400);
		}
		if (!readAllowed(permissionRules, tableName))
			throw new ApiException("Access to table ("+tableName+") not allowed", 400);

		//We have all info we need, lets execute the query!
		Map<String, Object> fieldDef = new LinkedHashMap<>();
		String result = "";
		String query = null;

		DirectQuerySender qs;
		try {
			qs = getIbisContext().createBeanAutowireByName(DirectQuerySender.class);
		} catch (Exception e) {
			throw new ApiException("An error occured on creating or closing the connection!", e);
		}
		
		try {
			qs.setName("QuerySender");
			qs.setDatasourceName(datasource);

			qs.setQueryType("select");
			qs.setSqlDialect("Oracle");
			qs.setBlobSmartGet(true);
			qs.setIncludeFieldDefinition(true);
			qs.configure(true);
			qs.open();

			try (Connection conn =qs.getConnection()) {
				ResultSet rs = null;
				try {
					rs = conn.getMetaData().getColumns(null, null, tableName, null);

					if (!rs.isBeforeFirst()) {
						rs.close();
						rs = conn.getMetaData().getColumns(null, null, tableName.toUpperCase(), null);
					}
	
					StringBuilder fielddefinition = new StringBuilder("<fielddefinition>");
					String field = null;
					if(!numberOfRowsOnly) {
						field = "<field name=\""+rnumColumnName+"\" type=\"INTEGER\" />";
						fielddefinition.append(field);
						fieldDef.put(rnumColumnName, "INTEGER");
						while(rs.next()) {
							field = "<field name=\"" + rs.getString(COLUMN_NAME) + "\" type=\"" + DB2XMLWriter.getFieldType(rs.getInt(DATA_TYPE)) + "\" size=\"" + rs.getInt(COLUMN_SIZE) + "\"/>";
							fielddefinition.append(field);
							fieldDef.put(rs.getString(COLUMN_NAME), DB2XMLWriter.getFieldType(rs.getInt(DATA_TYPE)) + "("+rs.getInt(COLUMN_SIZE)+")");
						}
					} else {
						field = "<field name=\""+countColumnName+"\" type=\"INTEGER\" />";
						fielddefinition.append(field);
						fieldDef.put(countColumnName, "INTEGER");
						if(StringUtils.isNotEmpty(order)) {
							rs = conn.getMetaData().getColumns(null, null, tableName, order);
							while(rs.next()) {
								field = "<field name=\"" + rs.getString(COLUMN_NAME) + "\" type=\"" + DB2XMLWriter.getFieldType(rs.getInt(DATA_TYPE)) + "\" size=\"" + rs.getInt(COLUMN_SIZE) + "\"/>";
								fielddefinition.append(field);
								fieldDef.put(rs.getString(COLUMN_NAME), DB2XMLWriter.getFieldType(rs.getInt(DATA_TYPE)) + "("+rs.getInt(COLUMN_SIZE)+")");
							}
						}
					}

					fielddefinition.append("</fielddefinition>");
	
					String browseJdbcTableExecuteREQ =
						"<browseJdbcTableExecuteREQ>"
							+ "<dbmsName>"
							+ qs.getDbmsSupport().getDbmsName()
							+ "</dbmsName>"
							+ "<countColumnName>"
							+ countColumnName
							+ "</countColumnName>"
							+ "<rnumColumnName>"
							+ rnumColumnName
							+ "</rnumColumnName>"
							+ "<tableName>"
							+ tableName
							+ "</tableName>"
							+ "<where>"
							+ XmlUtils.encodeChars(where)
							+ "</where>"
							+ "<numberOfRowsOnly>"
							+ numberOfRowsOnly
							+ "</numberOfRowsOnly>"
							+ "<order>"
							+ order
							+ "</order>"
							+ "<rownumMin>"
							+ minRow
							+ "</rownumMin>"
							+ "<rownumMax>"
							+ maxRow
							+ "</rownumMax>"
							+ fielddefinition
							+ "<maxColumnSize>1000</maxColumnSize>"
							+ "</browseJdbcTableExecuteREQ>";
					URL url = ClassUtils.getResourceURL(DB2XML_XSLT);
					if (url != null) {
						Transformer t = XmlUtils.createTransformer(url);
						query = XmlUtils.transformXml(t, browseJdbcTableExecuteREQ);
					}
					result = qs.sendMessage(new Message(query), null).asString();
				} finally {
					if (rs!=null) {
						rs.close();
					}
				}
			}
		} catch (Throwable t) {
			throw new ApiException("An error occured on executing jdbc query ["+query+"]", t);
		} finally {
			qs.close();
		}

		List<Map<String, String>> resultMap = null;
		if(XmlUtils.isWellFormed(result)) {
			try {
				resultMap = new QueryOutputToListOfMaps().parseString(result);
			} catch (IOException | SAXException e) {
				throw new ApiException("Query result could not be parsed.", e);
			}
		}
		if(resultMap == null)
			throw new ApiException("Invalid query result [null].", 400);

		Map<String, Object> resultObject = new HashMap<String, Object>();
		resultObject.put("table", tableName);
		resultObject.put("query", XmlUtils.encodeChars(query));
		resultObject.put("fielddefinition", fieldDef);
		resultObject.put("result", resultMap);

		return Response.status(Response.Status.CREATED).entity(resultObject).build();
	}

	@Context HttpServletRequest request;
	private boolean readAllowed(String rules, String tableName) {
		tableName = tableName.toLowerCase();
		List<String> rulesList = Arrays.asList(rules.split("\\|"));
		for (String rule: rulesList) {
			List<String> parts = Arrays.asList(rule.trim().split("\\s+"));
			if (parts.size() != 3) {
				log.debug("invalid rule '" + rule + "' contains " + parts.size() + " part(s): " + parts);
			} else {
				String tablePattern = parts.get(0).toLowerCase();
				if (tableName != null && tablePattern != null) {
					String role = parts.get(1);
					String type = parts.get(2);
					log.debug("check allow read table '" + tableName + "' with rule table '" + tablePattern + "', role '" + role + "' and type '" + type + "'");
					if ("*".equals(tablePattern) || tableName.equals(tablePattern)) {
						log.debug("table match");
						if ("*".equals(role) || request.isUserInRole(role)) {
							log.debug("role match");
							if ("allow".equals(type)) {
								log.debug("allow");
								return true;
							} else if ("deny".equals(type)) {
								log.debug("deny");
								return false;
							} else {
								log.error("invalid rule type");
							}
						}
					}
				}
			}
		}
		log.debug("deny");
		return false;
	}
}
