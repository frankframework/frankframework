/*
Copyright 2016 Integration Partners B.V.

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

import java.net.URL;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.log4j.Logger;
import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.transform.Transformer;
import nl.nn.adapterframework.jdbc.DirectQuerySender;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DB2XMLWriter;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;

/**
* Executes a query.
* 
* @author	Niels Meijer
*/

@Path("/")
public final class BrowseJdbcTable extends Base {
	@Context ServletConfig servletConfig;

	private static final String DB2XML_XSLT = "xml/xsl/BrowseJdbcTableExecute.xsl";
	private static final String permissionRules = AppConstants.getInstance().getResolvedProperty("browseJdbcTable.permission.rules");
	private Logger log = LogUtil.getLogger(this);

	@POST
	@RolesAllowed({"ObserverAccess", "IbisTester"})
	@Path("/jdbc/browse")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response execute(LinkedHashMap<String, Object> json) throws ApiException, ApiWarning {
		initBase(servletConfig);

		String realm = null, tableName = null, where = "", order = null;
		Boolean rowNumbersOnly = false;
		int minRow = 1, maxRow = 100;

		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			if(key.equalsIgnoreCase("realm")) {
				realm = entry.getValue().toString();
			}
			if(key.equalsIgnoreCase("table")) {
				tableName = entry.getValue().toString().toLowerCase();
			}
			if(key.equalsIgnoreCase("where")) {
				where = entry.getValue().toString().toLowerCase();
			}
			if(key.equalsIgnoreCase("order")) {
				order = entry.getValue().toString().toLowerCase();
			}
			if(key.equalsIgnoreCase("rowNumbersOnly")) {
				rowNumbersOnly = Boolean.parseBoolean(entry.getValue().toString());
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
					maxRow = Math.min(Math.max(maxRow, 1), 100);
				}
			}
		}

		if(realm == null || tableName == null) {
			throw new ApiWarning("realm and/or tableName not defined.");
		}

		if(maxRow < minRow)
			throw new ApiWarning("Rownum max must be greater than or equal to Rownum min");
		if (maxRow - minRow >= 100) {
			throw new ApiWarning("Difference between Rownum max and Rownum min must be less than hundred");
		}
		if (!readAllowed(permissionRules, tableName))
			throw new ApiWarning("Access to table ("+tableName+") not allowed");

		//We have all info we need, lets execute the query!
		Map<String, Object> fieldDef = new HashMap<String, Object>();
		DirectQuerySender qs;
		String result = "";
		String query = null;
		try {
			qs = (DirectQuerySender)ibisManager.getIbisContext().createBeanAutowireByName(DirectQuerySender.class);
			try {
				qs.setName("QuerySender");
				qs.setJmsRealm(realm);

				//if (form_numberOfRowsOnly || qs.getDatabaseType() == DbmsSupportFactory.DBMS_ORACLE) {
					qs.setQueryType("select");
					qs.setBlobSmartGet(true);
					qs.setIncludeFieldDefinition(true);
					qs.configure(true);
					qs.open();

					ResultSet rs = qs.getConnection().getMetaData().getColumns(null, null, tableName, null);
					if (!rs.isBeforeFirst()) {
						rs = qs.getConnection().getMetaData().getColumns(null, null, tableName.toUpperCase(), null);
					}
					
					String fielddefinition = "<fielddefinition>";
					while(rs.next()) {
						String field = "<field name=\""
								+ rs.getString(4)
								+ "\" type=\""
								+ DB2XMLWriter.getFieldType(rs.getInt(5))
								+ "\" size=\""
								+ rs.getInt(7)
								+ "\"/>";
						fielddefinition = fielddefinition + field;
						fieldDef.put(rs.getString(4), DB2XMLWriter.getFieldType(rs.getInt(5)) + "("+rs.getInt(7)+")");
					}
					fielddefinition = fielddefinition + "</fielddefinition>";

					String browseJdbcTableExecuteREQ =
						"<browseJdbcTableExecuteREQ>"
							+ "<dbmsName>"
							+ qs.getDbmsSupport().getDbmsName()
							+ "</dbmsName>"
							+ "<tableName>"
							+ tableName
							+ "</tableName>"
							+ "<where>"
							+ XmlUtils.encodeChars(where)
							+ "</where>"
							+ "<numberOfRowsOnly>"
							+ rowNumbersOnly
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
					URL url = ClassUtils.getResourceURL(this, DB2XML_XSLT);
					if (url != null) {
						Transformer t = XmlUtils.createTransformer(url);
						query = XmlUtils.transformXml(t, browseJdbcTableExecuteREQ);
					}
					result = qs.sendMessage("dummy", query);
				//} else {
					//error("errors.generic","This function only supports oracle databases",null);
				//}
			} catch (Throwable t) {
				throw new ApiWarning("An error occured on executing jdbc query: "+t.toString());
			} finally {
				qs.close();
			}
		} catch (Exception e) {
			throw new ApiWarning("An error occured on creating or closing the connection: "+e.toString());
		}

		List<Map<String, String>> resultMap = null;
		if(XmlUtils.isWellFormed(result)) {
			resultMap = XmlQueryResult2Map(result);
		}
		if(resultMap == null)
			throw new ApiWarning("Invalid query result.");

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
