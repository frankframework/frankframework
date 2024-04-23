package org.frankframework.management.web.spring;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.web.ApiException;
import org.frankframework.management.web.Description;
import org.frankframework.management.web.Relation;
import org.frankframework.util.RequestUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.RolesAllowed;

import java.util.Map;

@RestController
public class ExecuteJdbcQuery extends FrankApiBase{

	@RolesAllowed({ "IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@GetMapping(value = "/jdbc", produces = MediaType.APPLICATION_JSON_VALUE)
	@Relation("jdbc")
	@Description("view a list of all JDBC DataSources")
	public ResponseEntity<?> getJdbcDataSources() throws ApiException {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.JDBC, BusAction.GET);
		return callSyncGateway(builder);
	}

	@RolesAllowed({"IbisTester"})
	@PostMapping(value = "/jdbc/query", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@Relation("jdbc")
	@Description("execute a JDBC query on a datasource")
	public ResponseEntity<?> executeJdbcQuery(Map<String, Object> json) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.JDBC, BusAction.MANAGE);
		String datasource = RequestUtils.getValue(json, "datasource");
		String query = RequestUtils.getValue(json, "query");
		String resultType = RequestUtils.getValue(json, "resultType");

		if(resultType == null || query == null) {
			throw new ApiException("Missing data, datasource, resultType and query are expected.", 400);
		}
		builder.addHeader("query", query);
		builder.addHeader("resultType", resultType);

		builder.addHeader("avoidLocking", RequestUtils.getBooleanValue(json, "avoidLocking"));
		builder.addHeader("trimSpaces", RequestUtils.getBooleanValue(json, "trimSpaces"));

		String queryType = RequestUtils.getValue(json, "queryType");
		if("AUTO".equals(queryType)) {
			queryType = "other"; // defaults to other

			String[] commands = new String[] {"select", "show"}; //if it matches, set it to select
			for (String command : commands) {
				if(query.toLowerCase().startsWith(command)) {
					queryType = "select";
					break;
				}
			}
		}

		builder.addHeader(BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, datasource);
		builder.addHeader("queryType", queryType);
		return callSyncGateway(builder);
	}

}
