/*
   Copyright 2022-2023 WeAreFrank!

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.security.RolesAllowed;

import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.util.MimeType;

import org.frankframework.core.PipeLineSession;
import org.frankframework.jdbc.AbstractJdbcQuerySender.QueryType;
import org.frankframework.jdbc.DirectQuerySender;
import org.frankframework.jdbc.IDataSourceFactory;
import org.frankframework.jdbc.transformer.QueryOutputToCSV;
import org.frankframework.jdbc.transformer.QueryOutputToJson;
import org.frankframework.management.bus.ActionSelector;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusAware;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.TopicSelector;
import org.frankframework.management.bus.message.JsonMessage;
import org.frankframework.management.bus.message.StringMessage;
import org.frankframework.util.LogUtil;

@BusAware("frank-management-bus")
@TopicSelector(BusTopic.JDBC)
public class ExecuteJdbcQuery extends BusEndpointBase {
	private Logger secLog = LogUtil.getLogger("SEC");

	public enum ResultType {
		CSV, XML, JSON;
	}

	@ActionSelector(BusAction.GET)
	@RolesAllowed({ "IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	public Message<String> getJdbcInfo(Message<?> message) {
		Map<String, Object> result = new HashMap<>();

		IDataSourceFactory dataSourceFactory = getBean("dataSourceFactory", IDataSourceFactory.class);
		List<String> dataSourceNames = dataSourceFactory.getDataSourceNames();
		result.put("datasources", dataSourceNames);

		List<String> resultTypes = new ArrayList<>();
		for(ResultType type : ResultType.values()) {
			resultTypes.add(type.name().toLowerCase());
		}
		result.put("resultTypes", resultTypes);

		List<String> queryTypes = new ArrayList<>();
		queryTypes.add("AUTO");
		queryTypes.add(QueryType.SELECT.toString());
		queryTypes.add(QueryType.OTHER.toString());
		result.put("queryTypes", queryTypes);

		return new JsonMessage(result);
	}

	@ActionSelector(BusAction.MANAGE)
	@RolesAllowed({"IbisTester"})
	public StringMessage executeJdbcQuery(Message<?> message) {
		String datasource = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME);
		QueryType queryType = BusMessageUtils.getEnumHeader(message, "queryType", QueryType.class, QueryType.SELECT);
		String query = BusMessageUtils.getHeader(message, "query");
		boolean trimSpaces = BusMessageUtils.getBooleanHeader(message, "trimSpaces", false);
		boolean avoidLocking = BusMessageUtils.getBooleanHeader(message, "avoidLocking", false);
		ResultType resultType = BusMessageUtils.getEnumHeader(message, "resultType", ResultType.class, ResultType.XML);

		return doExecute(datasource, queryType, query, trimSpaces, avoidLocking, resultType);
	}

	private StringMessage doExecute(String datasource, QueryType queryType, String query, boolean trimSpaces, boolean avoidLocking, ResultType resultType) {
		secLog.info("executing query [%s] on datasource [%s] queryType [%s] avoidLocking [%s]".formatted(query, datasource, queryType, avoidLocking));

		DirectQuerySender qs = createBean(DirectQuerySender.class);
		String result;
		MimeType mimetype;

		try(PipeLineSession session = new PipeLineSession()) {
			qs.setName("ExecuteJdbc QuerySender");
			qs.setDatasourceName(datasource);
			qs.setQueryType(queryType);
			qs.setTrimSpaces(trimSpaces);
			qs.setAvoidLocking(avoidLocking);
			qs.setBlobSmartGet(true);
			qs.setPrettyPrint(true);
			qs.configure(true);
			qs.start();

			org.frankframework.stream.Message message = qs.sendMessageOrThrow(new org.frankframework.stream.Message(query), session);

			switch (resultType) {
			case CSV:
				result = new QueryOutputToCSV().parse(message);
				mimetype = MediaType.TEXT_PLAIN;
				break;
			case JSON:
				result = new QueryOutputToJson().parse(message);
				mimetype = MediaType.APPLICATION_JSON;
				break;
			case XML:
			default:
				result = message.asString();
				mimetype = MediaType.APPLICATION_XML;
				break;
			}
			message.close();
		} catch (Exception e) {
			log.debug("error executing query", e);
			throw new BusException("error executing query: "+e.getMessage(), 400);
		} finally {
			qs.stop();
		}

		return new StringMessage(result, mimetype);
	}
}
