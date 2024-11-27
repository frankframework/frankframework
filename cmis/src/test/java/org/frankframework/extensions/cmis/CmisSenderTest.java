package org.frankframework.extensions.cmis;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.senders.SenderTestBase;

public class CmisSenderTest extends SenderTestBase<CmisSender> {

	@Override
	public CmisSender createSender() {
		return new CmisSender();
	}

	@Test
	public void testEmptyUrlOverrideEntryPointWSDLNull() {
		assertThrows(IllegalArgumentException.class, ()->sender.setUrl(""));
	}

	@Test
	public void testEmptyRepository() {
		assertThrows(IllegalArgumentException.class, ()->sender.setRepository(""));
	}

	@Test
	public void testOverrideEntryPointWSDLWithoutWebservice() throws Exception {
		String dummyString = "dummyString";
		sender.setUrl(dummyString);
		sender.setOverrideEntryPointWSDL(dummyString);
		sender.setRepository(dummyString);
		sender.setBindingType(CmisSessionBuilder.BindingTypes.BROWSER);
		sender.setAction(CmisSender.CmisAction.DYNAMIC);
		sender.configure();

		assertThrows(LifecycleException.class, sender::start);
	}

	@Test
	public void testCreateActionWithNoSession() {
		String dummyString = "dummyString";
		sender.setUrl(dummyString);
		sender.setOverrideEntryPointWSDL(dummyString);
		sender.setRepository(dummyString);
		sender.setBindingType(CmisSessionBuilder.BindingTypes.WEBSERVICES);
		sender.setAction(CmisSender.CmisAction.CREATE);
		assertThrows(ConfigurationException.class, sender::configure);
	}

	@Test
	public void testGetActionWithNoSession() {
		String dummyString = "dummyString";
		sender.setUrl(dummyString);
		sender.setRepository(dummyString);
		sender.setBindingType(CmisSessionBuilder.BindingTypes.WEBSERVICES);
		sender.setAction(CmisSender.CmisAction.GET);
		sender.setGetProperties(true);
		assertThrows(ConfigurationException.class, sender::configure);
	}

	@Test
	public void testSuccessfulConfigure() throws Exception {
		String dummyString = "dummyString";
		sender.setUrl(dummyString);
		sender.setRepository(dummyString);
		sender.setBindingType(CmisSessionBuilder.BindingTypes.WEBSERVICES);
		sender.setAction(CmisSender.CmisAction.FIND);
		sender.configure();

		//Should configure and open just fine, but fail trying to connect to an endpoint.
		assertThrows(CmisConnectionException.class, sender::start);
	}
}
