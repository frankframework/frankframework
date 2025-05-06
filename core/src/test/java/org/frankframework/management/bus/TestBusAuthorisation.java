package org.frankframework.management.bus;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.frankframework.testutil.SpringRootInitializer;

@SpringJUnitConfig(initializers = {SpringRootInitializer.class})
public class TestBusAuthorisation extends BusTestBase {

	@Test
	@WithMockUser(authorities = { "ROLE_IbisTester" })
	public void callTestEndpointAuthorisedIsAdmin() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.DEBUG, BusAction.MANAGE);
		boolean isAdmin = Boolean.parseBoolean((String)callSyncGateway(request).getPayload());
		assertTrue(isAdmin);
	}

	@Test
	@WithMockUser(authorities = { "ROLE_IbisAdmin" })
	public void callTestEndpointAuthorisedNotAdmin() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.DEBUG, BusAction.MANAGE);
		boolean isAdmin = Boolean.parseBoolean((String)callSyncGateway(request).getPayload());
		assertFalse(isAdmin);
	}

	@Test
	@WithMockUser(authorities = { "ROLE_IbisObserver" })
	public void callTestEndpointUnAuthorised() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.DEBUG, BusAction.MANAGE);
		Exception e = assertThrows(Exception.class, () -> callSyncGateway(request));
		assertInstanceOf(AuthorizationDeniedException.class, e.getCause());
	}

	@Test
	public void callTestEndpointNoUser() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.DEBUG, BusAction.MANAGE);
		Exception e = assertThrows(Exception.class, () -> callSyncGateway(request));
		assertInstanceOf(AuthenticationCredentialsNotFoundException.class, e.getCause());
	}
}
