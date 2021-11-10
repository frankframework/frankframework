package nl.nn.adapterframework.extensions.cmis;

import static org.junit.Assert.assertEquals;

import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.senders.SenderTestBase;

public class CmisSenderTest extends SenderTestBase<CmisSender> {

	@Override
	public CmisSender createSender() {
		return new CmisSender();
	}

	@Test
	public void testValidAction() {
		String dummyString = "CREATE";
		sender.setAction(dummyString);

		assertEquals(dummyString.toLowerCase(), sender.getAction());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidAction() {
		String dummyString = "CREATED";
		sender.setAction(dummyString);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNonExistingBindingType() {
		sender.setBindingType("WEBTOMBROWSER");
	}

	@Test
	public void testExistingBindingTypes() {
		sender.setBindingType(BindingType.BROWSER.value());
		sender.setBindingType(BindingType.ATOMPUB.value());
		sender.setBindingType(BindingType.WEBSERVICES.value());
		//All BindingTypes should be parsed (and thus not throw an exception)
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
		sender.setBindingType("browser");
		sender.setAction("dynamic");
		sender.configure();
		sender.open();
	}

	@Test(expected = ConfigurationException.class)
	public void testCreateActionWithNoSession() throws ConfigurationException {
		String dummyString = "dummyString";
		sender.setUrl(dummyString);
		sender.setOverrideEntryPointWSDL(dummyString);
		sender.setRepository(dummyString);
		sender.setBindingType("webservices");
		sender.setAction("create");
		sender.configure();
	}

	@Test(expected = ConfigurationException.class)
	public void testGetActionWithNoSession() throws ConfigurationException {
		String dummyString = "dummyString";
		sender.setUrl(dummyString);
		sender.setRepository(dummyString);
		sender.setBindingType("webservices");
		sender.setAction("get");
		sender.setGetProperties(true);
		sender.configure();
	}

	@Test(expected = CmisConnectionException.class)
	public void testSuccessfulConfigure() throws Exception {
		String dummyString = "dummyString";
		sender.setUrl(dummyString);
		sender.setRepository(dummyString);
		sender.setBindingType("webservices");
		sender.setAction("find");
		sender.configure();
		sender.open();//Should configure and open just fine, but fail trying to connect to an endpoint.
	}
}