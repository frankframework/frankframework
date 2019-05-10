package nl.nn.adapterframework.extensions.cmis;

import nl.nn.adapterframework.configuration.ConfigurationException;

import org.junit.Test;

import static org.junit.Assert.*;

public class CmisSenderTest extends SenderBase<CmisSender> {

	@Override
	public CmisSender createSender() {
		return new CmisSender();
	}

	@Test
	public void getterSetterOverrideEntryPointWSDL() {
		String dummyString = "dummySring";
		sender.setOverrideEntryPointWSDL(dummyString);

		assertEquals(dummyString, sender.getOverrideEntryPointWSDL());
	}

	@Test
	public void getterSetterAllowSelfSignedCertificates() {
		sender.setAllowSelfSignedCertificates(true);
		assertEquals(true, sender.isAllowSelfSignedCertificates());

		sender.setAllowSelfSignedCertificates(false);
		assertEquals(false, sender.isAllowSelfSignedCertificates());
	}

	@Test
	public void getterSetterVerifyHostname() {
		sender.setVerifyHostname(true);
		assertEquals(true, sender.isVerifyHostname());

		sender.setVerifyHostname(false);
		assertEquals(false, sender.isVerifyHostname());
	}

	@Test
	public void getterSetterIgnoreCertificateExpiredException() {
		sender.setIgnoreCertificateExpiredException(true);
		assertEquals(true, sender.isIgnoreCertificateExpiredException());

		sender.setIgnoreCertificateExpiredException(false);
		assertEquals(false, sender.isIgnoreCertificateExpiredException());
	}

	@Test
	public void getterSetterCertificateUrl() {
		String dummyString = "dummyString";
		sender.setCertificateUrl(dummyString);

		assertEquals(dummyString, sender.getCertificate());
	}

	@Test
	public void getterSetterCertificateAuthAlias() {
		String dummyString = "dummyString";
		sender.setCertificateAuthAlias(dummyString);

		assertEquals(dummyString, sender.getCertificateAuthAlias());
	}

	@Test
	public void getterSetterCertificatePassword() {
		String dummyString = "dummyString";
		sender.setCertificatePassword(dummyString);

		assertEquals(dummyString, sender.getCertificatePassword());
	}

	@Test
	public void getterSetterTruststore() {
		String dummyString = "dummyString";
		sender.setTruststore(dummyString);

		assertEquals(dummyString, sender.getTruststore());
	}

	@Test
	public void getterSetterTruststoreAuthAlias() {
		String dummyString = "dummyString";
		sender.setTruststoreAuthAlias(dummyString);

		assertEquals(dummyString, sender.getTruststoreAuthAlias());
	}

	@Test
	public void getterSetterTruststorePassword() {
		String dummyString = "dummyString";
		sender.setTruststorePassword(dummyString);

		assertEquals(dummyString, sender.getTruststorePassword());
	}

	@Test
	public void getterSetterKeystoreType() {
		String dummyString = "dummyString";
		sender.setKeystoreType(dummyString);

		assertEquals(dummyString, sender.getKeystoreType());
	}

	@Test
	public void getterSetterKeyManagerAlgorithm() {
		String dummyString = "dummyString";
		sender.setKeyManagerAlgorithm(dummyString);

		assertEquals(dummyString, sender.getKeyManagerAlgorithm());
	}

	@Test
	public void getterSetterTrustManagerAlgorithm() {
		String dummyString = "dummyString";
		sender.setTrustManagerAlgorithm(dummyString);

		assertEquals(dummyString, sender.getTrustManagerAlgorithm());
	}

	@Test
	public void getterSetterProxyHost() {
		String dummyString = "dummyString";
		sender.setProxyHost(dummyString);

		assertEquals(dummyString, sender.getProxyHost());
	}

	@Test
	public void getterSetterProxyPort() {
		int dummyInt = 1337;
		sender.setProxyPort(dummyInt);

		assertEquals(dummyInt, sender.getProxyPort());
	}

	@Test
	public void getterSetterProxyAuthAlias() {
		String dummyString = "dummyString";
		sender.setProxyAuthAlias(dummyString);

		assertEquals(dummyString, sender.getProxyAuthAlias());
	}

	@Test
	public void getterSetterProxyUserName() {
		String dummyString = "dummyString";
		sender.setProxyUserName(dummyString);

		assertEquals(dummyString, sender.getProxyUserName());
	}

	@Test
	public void getterSetterProxyPassword() {
		String dummyString = "dummyString";
		sender.setProxyPassword(dummyString);

		assertEquals(dummyString, sender.getProxyPassword());
	}

	@Test
	public void getterSetterAction() {
		String dummyString = "dummyString";
		sender.setAction(dummyString);

		assertEquals(dummyString.toLowerCase(), sender.getAction());
	}

	@Test
	public void getterSetterUrle() {
		String dummyString = "dummyString";
		sender.setUrl(dummyString);

		assertEquals(dummyString, sender.getUrl());
	}

	@Test
	public void getterSetterRepository() {
		String dummyString = "dummyString";
		sender.setRepository(dummyString);

		assertEquals(dummyString, sender.getRepository());
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

	@Test
	public void getterSetterBridgeSender1() {
		sender.setBridgeSender(true);
		assertEquals(true, sender.isBridgeSender());
	}

	@Test
	public void getterSetterBridgeSender2() {
		sender.setBridgeSender(false);
		assertEquals(false, sender.isBridgeSender());
	}

	@Test
	public void getterSetterProxyRealm() {
		String dummyString = "dummyString";
		sender.setProxyRealm(dummyString);

		assertEquals(dummyString, sender.getProxyRealm());

		sender.setProxyRealm("");
		assertNull(sender.getProxyRealm());
	}

	@Test
	public void getterSetterBindingType() {
		String dummyString = "dummyString";
		sender.setBindingType(dummyString);

		assertEquals(dummyString.toLowerCase(), sender.getBindingType());

		sender.setBindingType(null);
		assertNull(sender.getBindingType());
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

	@Test(expected = ConfigurationException.class)
	public void testOverrideEntryPointWSDLWithoutWebservice() throws ConfigurationException {
		String dummyString = "dummyString";
		sender.setUrl(dummyString);
		sender.setOverrideEntryPointWSDL(dummyString);
		sender.setRepository(dummyString);
		sender.setBindingType("browser");
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
	public void testBridgeSender() throws ConfigurationException {
		String dummyString = "dummyString";
		sender.setUrl(dummyString);
		sender.setRepository(dummyString);
		sender.setBindingType("webservices");
		sender.setAction("find");
		sender.setBridgeSender(true);
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