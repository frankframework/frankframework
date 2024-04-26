package org.frankframework.management.web.spring;

import org.frankframework.management.bus.BusTopic;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.RolesAllowed;

@RestController
public class ConnectionOverview extends FrankApiBase{

	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@GetMapping(value = "/connections", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getConnections() {
		return callSyncGateway(RequestMessageBuilder.create(this, BusTopic.CONNECTION_OVERVIEW));
	}

}
