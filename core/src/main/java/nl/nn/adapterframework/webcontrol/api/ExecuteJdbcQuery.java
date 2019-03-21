/*
Copyright 2016-2017, 2019 Integration Partners B.V.

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.transform.Transformer;

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.jdbc.DirectQuerySender;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.receivers.JavaListener;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Executes a query.
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public final class ExecuteJdbcQuery extends Base {
	@Context ServletConfig servletConfig;

	public static final String XML2CSV_XSLT="xml/xsl/xmlresult2csv.xsl";

	@GET
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/jdbc")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getJdbcInfo() throws ApiException {
		initBase(servletConfig);

		Map<String, Object> result = new HashMap<String, Object>();

		List<String> realmNames = JmsRealmFactory.getInstance().getRegisteredRealmNamesAsList();
		List<String> datasourceNames = new ArrayList<String>();
		for(String s : realmNames) {
			datasourceNames.add("["+s+"] "  + JmsRealmFactory.getInstance().getJmsRealm(s).getDatasourceName());
		}
		if (datasourceNames.size() == 0)
			datasourceNames.add("no data sources defined");
		result.put("datasourceNames", datasourceNames);

		List<String> expectResultOptions = new ArrayList<String>();
		expectResultOptions.add("auto");
		expectResultOptions.add("yes");
		expectResultOptions.add("no");
		result.put("expectResultOptions", expectResultOptions);
		
		List<String> resultTypes = new ArrayList<String>();
		resultTypes.add("csv");
		resultTypes.add("xml");
		result.put("resultTypes", resultTypes);

		return Response.status(Response.Status.CREATED).entity(result).build();
	}

	@POST
	@RolesAllowed({"IbisTester"})
	@Path("/jdbc/query")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response execute(LinkedHashMap<String, Object> json) throws ApiException {
		initBase(servletConfig);
		
		String datasourceName = null;
		String resultType = null;
		String query = null;
		String expectResultSet = null;
		String result = "";
		String returnType = MediaType.APPLICATION_XML;
		Object returnEntity = null;
		
		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			if(key.equalsIgnoreCase("datasourceName")) {
				datasourceName = entry.getValue().toString();
			}
			if(key.equalsIgnoreCase("resultType")) {
				resultType = entry.getValue().toString().toLowerCase();
				if(resultType.equalsIgnoreCase("csv")) {
					returnType = MediaType.TEXT_PLAIN;
				}
				else if(resultType.equalsIgnoreCase("json")) {
					returnType = MediaType.APPLICATION_JSON;
				}
				else if(resultType.equalsIgnoreCase("xml")) {
					returnType = MediaType.APPLICATION_XML;
				}
			}
			if(key.equalsIgnoreCase("query")) {
				query = entry.getValue().toString();
			}
			if(key.equalsIgnoreCase("expectResultSet")) {
				expectResultSet = entry.getValue().toString();
			}
		}

		if(datasourceName == null || resultType == null || query == null) {
			throw new ApiException("Missing data; datasourceName, resultType and query are expected.", 400);
		}

		//We have all info we need, lets execute the query!
		
		XmlBuilder xbRoot = new XmlBuilder("manageDatabaseREQ");
		
		XmlBuilder xSql = new XmlBuilder("sql");
		xSql.addAttribute("datasourceName", datasourceName);
		xSql.addAttribute("expectResultSet", expectResultSet);
		xbRoot.addSubElement(xSql);

		XmlBuilder xQType = new XmlBuilder("type");
		xQType.setValue(query.split(" ")[0]);
		xSql.addSubElement(xQType);
		
		XmlBuilder xQuery = new XmlBuilder("query");
		xQuery.setValue(query);
		xSql.addSubElement(xQuery);
		
		// Send XML to ManageDatabase adapter
		JavaListener listener = JavaListener.getListener("ManageDatabase");
		try {			
			HashMap context = new HashMap();
			String mId = listener.getIdFromRawMessage(xbRoot, context);
			result = listener.processRequest(mId, xbRoot.toXML(), context);
			
			if (resultType.equalsIgnoreCase("csv")) {
				URL url= ClassUtils.getResourceURL(this, XML2CSV_XSLT);
				if (url!=null) {
					Transformer t = XmlUtils.createTransformer(url);
					result = XmlUtils.transformXml(t,result);
				}
			}
		} catch (ListenerException e1) {
			log.error("Error occured on creating or closing connection:\n" + e1);
		} catch (Throwable e) {
			log.error("Error occured on executing jdbc query:\n" + e);
		}
		
		if(resultType.equalsIgnoreCase("json")) {
			if(XmlUtils.isWellFormed(result)) {
				returnEntity = XmlQueryResult2Map(result);
			}
			if(returnEntity == null)
				throw new ApiException("Invalid query result", 400);
		}
		else
			returnEntity = result;

		return Response.status(Response.Status.CREATED).type(returnType).entity(returnEntity).build();
	}
}