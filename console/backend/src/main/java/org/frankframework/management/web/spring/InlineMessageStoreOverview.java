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
public class InlineMessageStoreOverview extends FrankApiBase {

	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("messagebrowser")
	@Description("view available messagebrowsers")
	@GetMapping(value = "inlinestores/overview", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getMessageBrowsers() {
		return callSyncGateway(RequestMessageBuilder.create(this, BusTopic.INLINESTORAGE_SUMMARY));
	}

}
