package nl.nn.adapterframework.extensions.cmis;

import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.junit.Test;

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

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyUrlOverrideEntryPointWSDLNull() {
		sender.setUrl("");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyRepository() {
		sender.setRepository("");
	}

	@Test(expected = SenderException.class)
	public void testOverrideEntryPointWSDLWithoutWebservice() throws Exception {
		String dummyString = "dummyString";
		sender.setUrl(dummyString);
		sender.setOverrideEntryPointWSDL(dummyString);
		sender.setRepository(dummyString);
		sender.setBindingType(BindingTypes.BROWSER);
		sender.setAction(CmisAction.DYNAMIC);
		sender.configure();
		sender.open();
	}

	@Test(expected = ConfigurationException.class)
	public void testCreateActionWithNoSession() throws ConfigurationException {
		String dummyString = "dummyString";
		sender.setUrl(dummyString);
		sender.setOverrideEntryPointWSDL(dummyString);
		sender.setRepository(dummyString);
		sender.setBindingType(BindingTypes.WEBSERVICES);
		sender.setAction(CmisAction.CREATE);
		sender.configure();
	}

	@Test(expected = ConfigurationException.class)
	public void testGetActionWithNoSession() throws ConfigurationException {
		String dummyString = "dummyString";
		sender.setUrl(dummyString);
		sender.setRepository(dummyString);
		sender.setBindingType(BindingTypes.WEBSERVICES);
		sender.setAction(CmisAction.GET);
		sender.setGetProperties(true);
		sender.configure();
	}

	@Test(expected = CmisConnectionException.class)
	public void testSuccessfulConfigure() throws Exception {
		String dummyString = "dummyString";
		sender.setUrl(dummyString);
		sender.setRepository(dummyString);
		sender.setBindingType(BindingTypes.WEBSERVICES);
		sender.setAction(CmisAction.FIND);
		sender.configure();
		sender.open();//Should configure and open just fine, but fail trying to connect to an endpoint.
	}
}