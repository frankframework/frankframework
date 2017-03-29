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

import nl.nn.adapterframework.jdbc.DirectQuerySender;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.XmlUtils;

/**
* Executes a query.
* 
* @author	Niels Meijer
*/

@Path("/")
public final class ExecuteJdbcQuery extends Base {
	@Context ServletConfig servletConfig;

	public static final String DB2XML_XSLT="xml/xsl/dbxml2csv.xslt";

	@GET
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/jdbc")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getJdbcInfo() throws ApiException {
		initBase(servletConfig);

		Map<String, Object> result = new HashMap<String, Object>();

		List<String> jmsRealms = JmsRealmFactory.getInstance().getRegisteredRealmNamesAsList();
		if (jmsRealms.size() == 0)
			jmsRealms.add("no realms defined");
		result.put("jmsRealms", jmsRealms);

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

		String realm = null, resultType = null, query = null, queryType = "select", result = "", returnType = MediaType.APPLICATION_XML;
		Object returnEntity = null;
		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			if(key.equalsIgnoreCase("realm")) {
				realm = entry.getValue().toString();
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
			if(key.equalsIgnoreCase("query")) {
				query = entry.getValue().toString();
				if(query.toLowerCase().indexOf("select") == -1) queryType = "other";
			}
		}

		if(realm == null || resultType == null || query == null) {
			throw new ApiException("Missing data, realm, resultType and query are expected.", 400);
		}

		//We have all info we need, lets execute the query!
		try {
			DirectQuerySender qs = (DirectQuerySender) ibisManager.getIbisContext().createBeanAutowireByName(DirectQuerySender.class);
			try {
				qs.setName("QuerySender");
				qs.setJmsRealm(realm);
				qs.setQueryType(queryType);
				qs.setBlobSmartGet(true);
				qs.configure(true);
				qs.open();
				result = qs.sendMessage("dummy", query);
				if (resultType.equalsIgnoreCase("csv")) {
					URL url = ClassUtils.getResourceURL(this, DB2XML_XSLT);
					if (url!=null) {
						Transformer t = XmlUtils.createTransformer(url);
						result = XmlUtils.transformXml(t,result);
					}
				}

				qs.close();
			} catch (Throwable t) {
				throw new ApiException("An error occured on executing jdbc query: "+t.toString(), 400);
			}
		} catch (Exception e) {
			throw new ApiException("An error occured on creating or closing the connection: "+e.toString(), 400);
		}

		if(resultType.equalsIgnoreCase("json")) {
			if(XmlUtils.isWellFormed(result)) {
				returnEntity = XmlQueryResult2Map(result);
			}
			if(returnEntity == null)
				throw new ApiException("Invalid query result.", 400);
		}
		else
			returnEntity = result;

		return Response.status(Response.Status.CREATED).type(returnType).entity(returnEntity).build();
	}
}