package org.frankframework.management.web.spring;

import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.web.Description;
import org.frankframework.management.web.Relation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.RolesAllowed;

@RestController
public class EnvironmentVariables extends FrankApiBase {

	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("debug")
	@Description("view all system/environment/application properties")
	@GetMapping(value = "/environmentvariables", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getEnvironmentVariables() {
		return callSyncGateway(RequestMessageBuilder.create(this, BusTopic.ENVIRONMENT));
	}

}
