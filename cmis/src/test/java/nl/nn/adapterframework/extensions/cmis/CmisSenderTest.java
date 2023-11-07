package nl.nn.adapterframework.extensions.cmis;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.extensions.cmis.CmisSender.CmisAction;
import nl.nn.adapterframework.extensions.cmis.CmisSessionBuilder.BindingTypes;
import nl.nn.adapterframework.senders.SenderTestBase;

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
		sender.setBindingType(BindingTypes.BROWSER);
		sender.setAction(CmisAction.DYNAMIC);
		sender.configure();

		assertThrows(SenderException.class, sender::open);
	}

	@Test
	public void testCreateActionWithNoSession() throws ConfigurationException {
		String dummyString = "dummyString";
		sender.setUrl(dummyString);
		sender.setOverrideEntryPointWSDL(dummyString);
		sender.setRepository(dummyString);
		sender.setBindingType(BindingTypes.WEBSERVICES);
		sender.setAction(CmisAction.CREATE);
		assertThrows(ConfigurationException.class, sender::configure);
	}

	@Test
	public void testGetActionWithNoSession() throws ConfigurationException {
		String dummyString = "dummyString";
		sender.setUrl(dummyString);
		sender.setRepository(dummyString);
		sender.setBindingType(BindingTypes.WEBSERVICES);
		sender.setAction(CmisAction.GET);
		sender.setGetProperties(true);
		assertThrows(ConfigurationException.class, sender::configure);
	}

	@Test
	public void testSuccessfulConfigure() throws Exception {
		String dummyString = "dummyString";
		sender.setUrl(dummyString);
		sender.setRepository(dummyString);
		sender.setBindingType(BindingTypes.WEBSERVICES);
		sender.setAction(CmisAction.FIND);
		sender.configure();

		//Should configure and open just fine, but fail trying to connect to an endpoint.
		assertThrows(CmisConnectionException.class, sender::open);
	}
}