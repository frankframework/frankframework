package org.frankframework.management.web.spring;

import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.web.Description;
import org.frankframework.management.web.Relation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.RolesAllowed;

import java.util.Map;

@RestController
public class IbisstoreSummary extends FrankApiBase {

	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("jdbc")
	@Description("view database dump of the IbisStore table")
	@PostMapping(value = "/jdbc/summary", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getIbisstoreSummary(Map<String, Object> json) {

		String query = null;
		String datasource = null;

		for (Map.Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			if("datasource".equalsIgnoreCase(key)) {
				datasource = entry.getValue().toString();
			}
			if("query".equalsIgnoreCase(key)) {
				query = entry.getValue().toString();
			}
		}

		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.IBISSTORE_SUMMARY);
		builder.addHeader(BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, datasource);
		builder.addHeader("query", query);
		return callSyncGateway(builder);
	}

}
