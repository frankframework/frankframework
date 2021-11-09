package nl.nn.adapterframework.extensions.cmis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConnectionException;
import org.hamcrest.Matchers;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.extensions.cmis.server.CmisEvent;
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

	@Test
	public void parseCmisEvents() throws Exception {
		assertEquals(CmisEvent.GET_OBJECT, sender.parseEvent(CmisEvent.GET_OBJECT.getLabel()));
		assertEquals(CmisEvent.GET_OBJECT, sender.parseEvent(CmisEvent.GET_OBJECT.name()));

		IllegalArgumentException exception1 = assertThrows(
				IllegalArgumentException.class, () -> {
				sender.parseEvent(null);
			}
		);
		assertThat(exception1.getMessage(), Matchers.endsWith("CmisEvent may not be empty"));

		IllegalArgumentException exception2 = assertThrows(
				IllegalArgumentException.class, () -> {
				sender.parseEvent("ken ik niet");
			}
		);
		assertThat(exception2.getMessage(), Matchers.startsWith("cannot set field [CmisEvent] to unparsable value "));
	}
}