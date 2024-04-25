package org.frankframework.management.web.spring;

import org.apache.commons.lang3.StringUtils;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.web.Description;
import org.frankframework.management.web.Relation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.security.RolesAllowed;

public class AdapterStatistics extends FrankApiBase {

	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("statistics")
	@GetMapping(value = "/adapters/{adapter}/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
	@Deprecated
	public ResponseEntity<?> getAdapterStatisticsOld(@PathVariable("adapter") String adapter, @RequestParam(value = "configuration", required = false) String configuration) {
		final String config = StringUtils.isNotEmpty(configuration) ? configuration : null;
		return getAdapterStatistics(config, adapter);
	}

	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("statistics")
	@Description("view adapter processing statistics")
	@GetMapping(value = "/configurations/{configuration}/adapters/{adapter}/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getAdapterStatistics(@PathVariable("configuration") String configuration, @PathVariable("adapter") String adapter) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.ADAPTER, BusAction.STATUS);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configuration);
		builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapter);
		return callSyncGateway(builder);
	}

}
