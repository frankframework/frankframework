package org.frankframework.management.web.spring;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.PermitAll;
import java.util.HashMap;
import java.util.Map;

@RestController
public class ServerStatistics extends FrankApiBase {

	@PermitAll
	@GetMapping(value = "/server/info", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getServerInformation() {
		return callSyncGateway(RequestMessageBuilder.create(this, BusTopic.APPLICATION, BusAction.GET));
	}

	@PermitAll
	@GetMapping(value = "/server/configurations", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getAllConfigurations() {
		return callSyncGateway(RequestMessageBuilder.create(this, BusTopic.CONFIGURATION, BusAction.FIND));
	}

	@PermitAll
	@GetMapping(value = "/server/warnings", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getServerConfiguration() {
		return callSyncGateway(RequestMessageBuilder.create(this, BusTopic.APPLICATION, BusAction.WARNINGS));
	}

	@PermitAll
	@GetMapping(value = "/server/health", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getIbisHealth() {
		try {
			return callSyncGateway(RequestMessageBuilder.create(this, BusTopic.HEALTH));
		} catch(ApiException e) {
			Map<String, Object> response = new HashMap<>();
			response.put("status", HttpStatus.INTERNAL_SERVER_ERROR);
			response.put("error", "unable to connect to backend system: "+e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		} catch (MessageHandlingException e) {
			throw e; //Spring gateway exchange exceptions are handled by the SpringBusExceptionHandler
		} catch (Exception e) {
			throw new ApiException(e);
		}
	}

}
