package nl.nn.adapterframework.extensions.cmis;

import nl.nn.adapterframework.configuration.ConfigurationException;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.junit.Test;

import static org.junit.Assert.*;

public class CmisSenderTest extends SenderBase<CmisSender> {

	@Override
	public CmisSender createSender() {
		return new CmisSender();
	}

	@Test
	public void getterSetterOverrideEntryPointWSDL() {
		String dummyString = "dummyString";
		sender.setOverrideEntryPointWSDL(dummyString);
	}

	@Test
	public void getterSetterAllowSelfSignedCertificates() {
		sender.setAllowSelfSignedCertificates(true);

		sender.setAllowSelfSignedCertificates(false);
	}

	@Test
	public void getterSetterVerifyHostname() {
		sender.setVerifyHostname(true);

		sender.setVerifyHostname(false);
	}

	@Test
	public void getterSetterIgnoreCertificateExpiredException() {
		sender.setIgnoreCertificateExpiredException(true);

		sender.setIgnoreCertificateExpiredException(false);
	}

	@Test
	public void getterSetterCertificateUrl() {
		String dummyString = "dummyString";
		sender.setCertificateUrl(dummyString);
	}

	@Test
	public void getterSetterCertificateAuthAlias() {
		String dummyString = "dummyString";
		sender.setCertificateAuthAlias(dummyString);
	}

	@Test
	public void getterSetterCertificatePassword() {
		String dummyString = "dummyString";
		sender.setCertificatePassword(dummyString);
	}

	@Test
	public void getterSetterTruststore() {
		String dummyString = "dummyString";
		sender.setTruststore(dummyString);
	}

	@Test
	public void getterSetterTruststoreAuthAlias() {
		String dummyString = "dummyString";
		sender.setTruststoreAuthAlias(dummyString);
	}

	@Test
	public void getterSetterTruststorePassword() {
		String dummyString = "dummyString";
		sender.setTruststorePassword(dummyString);
	}

	@Test
	public void getterSetterKeystoreType() {
		String dummyString = "dummyString";
		sender.setKeystoreType(dummyString);
	}

	@Test
	public void getterSetterKeyManagerAlgorithm() {
		String dummyString = "dummyString";
		sender.setKeyManagerAlgorithm(dummyString);
	}

	@Test
	public void getterSetterTrustManagerAlgorithm() {
		String dummyString = "dummyString";
		sender.setTrustManagerAlgorithm(dummyString);
	}

	@Test
	public void getterSetterProxyHost() {
		String dummyString = "dummyString";
		sender.setProxyHost(dummyString);
	}

	@Test
	public void getterSetterProxyPort() {
		int dummyInt = 1337;
		sender.setProxyPort(dummyInt);
	}

	@Test
	public void getterSetterProxyAuthAlias() {
		String dummyString = "dummyString";
		sender.setProxyAuthAlias(dummyString);
	}

	@Test
	public void getterSetterProxyUserName() {
		String dummyString = "dummyString";
		sender.setProxyUsername(dummyString);
	}

	@Test
	public void getterSetterProxyPassword() {
		String dummyString = "dummyString";
		sender.setProxyPassword(dummyString);
	}

	@Test
	public void getterSetterAction() {
		String dummyString = "dummyString";
		sender.setAction(dummyString);

		assertEquals(dummyString.toLowerCase(), sender.getAction());
	}

	@Test
	public void getterSetterUrl() throws ConfigurationException {
		String dummyString = "dummyString";
		sender.setUrl(dummyString);
	}

	@Test
	public void getterSetterRepository() throws ConfigurationException {
		String dummyString = "dummyString";
		sender.setRepository(dummyString);
	}

	@Test
	public void getterSetterAuthAlias() {
		String dummyString = "dummyString";
		sender.setAuthAlias(dummyString);

		assertEquals(dummyString, sender.getAuthAlias());
	}

	@Test
	public void getterSetterUserName() {
		String dummyString = "dummyString";
		sender.setUserName(dummyString);

		assertEquals(dummyString, sender.getUserName());
	}

	@Test
	public void getterSetterPassword() {
		String dummyString = "dummyString";
		sender.setPassword(dummyString);

		assertEquals(dummyString, sender.getPassword());
	}

	@Test
	public void getterSetterFileNameSessionKey() {
		String dummyString = "dummyString";
		sender.setFileNameSessionKey(dummyString);

		assertEquals(dummyString, sender.getFileNameSessionKey());
	}

	@Test
	public void getterSetterFileInputStreamSessionKey() {
		String dummyString = "dummyString";
		sender.setFileInputStreamSessionKey(dummyString);

		assertEquals(dummyString, sender.getFileInputStreamSessionKey());
	}

	@Test
	public void getterSetterFileContentSessionKey() {
		String dummyString = "dummyString";
		sender.setFileContentSessionKey(dummyString);

		assertEquals(dummyString, sender.getFileContentSessionKey());
	}

	@Test
	public void getterSetterDefaultMediaType() {
		String dummyString = "dummyString";
		sender.setDefaultMediaType(dummyString);

		assertEquals(dummyString, sender.getDefaultMediaType());
	}

	@Test
	public void getterSetterStreamResultToServlet() {
		sender.setStreamResultToServlet(true);

		assertTrue(sender.isStreamResultToServlet());
	}

	@Test
	public void getterSetterResultOnNotFound() {
		String dummyString = "dummyString";
		sender.setResultOnNotFound(dummyString);

		assertEquals(dummyString, sender.getResultOnNotFound());
	}

	@Test
	public void getterSetterGetProperties() {
		sender.setGetProperties(true);
		assertEquals(true, sender.isGetProperties());

		sender.setGetProperties(false);
		assertEquals(false, sender.isGetProperties());
	}

	@Test
	public void getterSetterUseRootFolder() {
		sender.setUseRootFolder(true);
		assertEquals(true, sender.isUseRootFolder());

		sender.setUseRootFolder(false);
		assertEquals(false, sender.isUseRootFolder());
	}

	@Test
	public void getterSetterKeepSession() {
		sender.setKeepSession(true);
		assertEquals(true, sender.isKeepSession());

		sender.setKeepSession(false);
		assertEquals(false, sender.isKeepSession());
	}

	@Test(expected = Exception.class)
	public void getterSetterBindingTypeFailure() throws ConfigurationException {
		sender.setBindingType("dummyString");
	}

	@Test()
	public void getterSetterBindingTypeSuccess() throws ConfigurationException {
		sender.setBindingType(BindingType.BROWSER.value());
		sender.setBindingType(BindingType.ATOMPUB.value());
		sender.setBindingType(BindingType.WEBSERVICES.value());
	}

	@Test(expected = ConfigurationException.class)
	public void testEmptyUrlOverrideEntryPointWSDLNull() throws ConfigurationException {
		sender.setUrl("");
		sender.configure();
	}

	@Test(expected = ConfigurationException.class)
	public void testEmptyRepository() throws ConfigurationException {
		String dummyString = "dummyString";
		sender.setUrl(dummyString);
		sender.setRepository("");
		sender.configure();
	}

	@Test(expected = ConfigurationException.class)
	public void testWrongBindingType() throws ConfigurationException {
		String dummyString = "dummyString";
		sender.setUrl(dummyString);
		sender.setRepository(dummyString);
		sender.setBindingType(dummyString);
		sender.configure();
	}

	@Test()
	public void testOverrideEntryPointWSDLWithoutWebservice() throws ConfigurationException {
		String dummyString = "dummyString";
		sender.setUrl(dummyString);
		sender.setOverrideEntryPointWSDL(dummyString);
		sender.setRepository(dummyString);
		sender.setBindingType("browser");
		sender.setAction("dynamic");
		sender.configure();
	}

	@Test(expected = ConfigurationException.class)
	public void testWrongAction() throws ConfigurationException {
		String dummyString = "dummyString";
		sender.setUrl(dummyString);
		sender.setRepository(dummyString);
		sender.setBindingType("webservices");
		sender.setAction(dummyString);
		sender.configure();
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

	@Test
	public void testSuccessfulConfigure() throws ConfigurationException {
		String dummyString = "dummyString";
		sender.setUrl(dummyString);
		sender.setRepository(dummyString);
		sender.setBindingType("webservices");
		sender.setAction("find");
		sender.configure();
	}
}