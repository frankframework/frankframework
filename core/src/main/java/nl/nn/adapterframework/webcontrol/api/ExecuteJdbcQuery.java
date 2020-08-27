/*
Copyright 2016-2017, 2019, 2020 WeAreFrank!

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
import nl.nn.adapterframework.jdbc.transformer.AbstractQueryOutputTransformer;
import nl.nn.adapterframework.jdbc.transformer.QueryOutputToCSV;
import nl.nn.adapterframework.jdbc.transformer.QueryOutputToJson;
import nl.nn.adapterframework.jms.JmsRealm;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.stream.Message;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Executes a query.
 *
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public final class ExecuteJdbcQuery extends Base {

	public static final String DBXML2CSV_XSLT="xml/xsl/dbxml2csv.xslt";

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/jdbc")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getJdbcInfo() throws ApiException {

		Map<String, Object> result = new HashMap<String, Object>();
		JmsRealmFactory realmFactory = JmsRealmFactory.getInstance();

		List<String> jmsRealms = realmFactory.getRegisteredDatasourceRealmNamesAsList();
		List<String> datasources = new ArrayList<>();
		if (jmsRealms.size() == 0) {
			datasources.add("no datasources found in jmsRealms");
		} else {
			for (String jmsRealm : jmsRealms) {
				JmsRealm realm = realmFactory.getJmsRealm(jmsRealm);
				String datasourceName = realm.getDatasourceName();
				if(!datasources.contains(datasourceName)) { //It's possible multiple realms use the same datasourceName
					datasources.add(datasourceName);
				}
			}
		}

		result.put("datasources", datasources);

		List<String> resultTypes = new ArrayList<String>();
		resultTypes.add("csv");
		resultTypes.add("xml");
		resultTypes.add("json");
		result.put("resultTypes", resultTypes);

		return Response.status(Response.Status.CREATED).entity(result).build();
	}

	@POST
	@RolesAllowed({"IbisTester"})
	@Path("/jdbc/query")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response execute(LinkedHashMap<String, Object> json) throws ApiException {

		String datasource = null, resultType = null, query = null, queryType = null, result = "", returnType = MediaType.APPLICATION_XML;
		boolean avoidLocking = false;
		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			if(key.equalsIgnoreCase("datasource")) {
				datasource = entry.getValue().toString();
			}
			if(key.equalsIgnoreCase("resultType")) {
				resultType = entry.getValue().toString().toLowerCase();
				if(resultType.equalsIgnoreCase("csv")) {
					returnType = MediaType.TEXT_PLAIN;
				}
				if(resultType.equalsIgnoreCase("json")) {
					returnType = MediaType.APPLICATION_JSON;
				}
			}
			if(key.equalsIgnoreCase("avoidLocking")) {
				avoidLocking = Boolean.parseBoolean(entry.getValue().toString());
			}
			if(key.equalsIgnoreCase("query")) {
				query = entry.getValue().toString();
			}
			if(key.equalsIgnoreCase("type")) {
				queryType = entry.getValue().toString();
			}
		}

		if(StringUtils.isEmpty(queryType)) {
			queryType = "other"; // defaults to other

			String[] commands = new String[] {"select", "show"}; //if it matches, set it to select
			for (String command : commands) {
				if(query.toLowerCase().startsWith(command)) {
					queryType = "select";
					break;
				}
			}
		}

		if(datasource == null || resultType == null || query == null) {
			throw new ApiException("Missing data, datasource, resultType and query are expected.", 400);
		}

		//We have all info we need, lets execute the query!
		DirectQuerySender qs;
		try {
			qs = (DirectQuerySender) getIbisContext().createBeanAutowireByName(DirectQuerySender.class);
		} catch (Exception e) {
			throw new ApiException("An error occured on creating or closing the connection", e);
		}

		try {
			qs.setName("QuerySender");
			qs.setDatasourceName(datasource);
			qs.setQueryType(queryType);
			qs.setAvoidLocking(avoidLocking);
			qs.setBlobSmartGet(true);
			qs.configure(true);
			qs.open();
			Message message = qs.sendMessage(new Message(query), null);

			if (resultType.equalsIgnoreCase("csv")) {
				AbstractQueryOutputTransformer filter = new QueryOutputToCSV();
				result = filter.parse(message);
			} else if (resultType.equalsIgnoreCase("json")) {
				AbstractQueryOutputTransformer filter = new QueryOutputToJson();
				result = filter.parse(message);
			} else {
				result = message.asString();
			}

		} catch (Throwable t) {
			throw new ApiException("Error executing query", t);
		} finally {
			qs.close();
		}

		return Response.status(Response.Status.CREATED).type(returnType).entity(result).build();
	}
}