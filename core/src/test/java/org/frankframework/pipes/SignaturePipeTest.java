package org.frankframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URL;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.Arrays;

import javax.net.ssl.KeyManager;
import javax.net.ssl.X509KeyManager;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeRunResult;
import org.frankframework.encryption.EncryptionException;
import org.frankframework.encryption.KeystoreType;
import org.frankframework.encryption.PkiUtil;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.parameters.Parameter;
import org.frankframework.pipes.SignaturePipe.Action;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassLoaderUtils;

public class SignaturePipeTest extends PipeTestBase<SignaturePipe> {

	private final String testMessage = "xyz";

	private final String testSignature = "JBKjNltZoFlQTsBgstpnIB4itBxzAohRXGpIWuIQh51F64P4WdT+R/55v+cHrPsQ2B49GhROeFUyy7kafOKTfMTjm7DQ5yT/srImFTlZZZbHbvQns2NWBE8DoQKt6SOYowDNIJY5qDV+82k6xY2BcTcZoiAPB53F3rEkfzz/QkxcFiCKvtg2voG1WyVkyoue10404UXIkSXv0ySYnRBRugdPO1DKyUwL6FS5tP2p8toBVzeRT6rMkEwuU3A5riQpdnEOi0ckeFvSNU3Cdgdah4HWd+48gXzBE6Uwu/BMOrD/5mRUnS0wmPn7dajkjHNC2r9+C1jxlFy3NIim1rS2iA==";

	private final String multipasswordKSSignature = "0b5443UL24hO4WaMif2G9k0wcpxboEhdDQEldui22NZtNfLwHjiTMosCZcwi2E3JEk/ZRU5JNfjwKoRw4qjI+dWo+7b3Bliny0KTjVXpAJrpDIJJXn25TMW7q2U+mpWl05ygLs3p9jjLSukE+5pi0WofGsQ7hco4KjR+uzfp+TfEmkEyG3dsxhDaaxXa8OCbrchmPrzkoMT4sJHT7k7aTpKX4Wg/mGfUpuMBy0G90CWAW72pKbBhxsKrFa3mqrHCUtYQ9Zb1X0PJtFLaaj/iQD6BBmE4Vl4YGD5ZRSsMCyZ9p49oiD2skHkwEbozfQ8vyXa3Udf4Eo26eMaiMw5goA==";

	@Override
	public SignaturePipe createPipe() {
		return new SignaturePipe();
	}

	@Test
	void testSign() throws Exception {
		String pfxCertificate = "/Signature/certificate.pfx";
		String pfxPassword = "geheim";

		URL pfxURL = ClassLoaderUtils.getResourceURL(pfxCertificate);
		assertNotNull(pfxURL, "PFX file not found");
		KeyStore keystore = PkiUtil.createKeyStore(pfxURL, pfxPassword, KeystoreType.PKCS12);
		KeyManager[] keymanagers = PkiUtil.createKeyManagers(keystore, pfxPassword, null);
		if (keymanagers==null || keymanagers.length==0) {
			fail("No keymanager found in PFX file ["+pfxCertificate+"]");
		}
		X509KeyManager keyManager = (X509KeyManager)keymanagers[0];
		PrivateKey privateKey = keyManager.getPrivateKey("1");

		String alias = "1";
		String[] aliases = null;
		if(privateKey == null) {
			try {
				aliases = keyManager.getServerAliases("RSA", null);
				if(aliases != null) { // Try the first alias
					privateKey = keyManager.getPrivateKey(aliases[0]);
					assertNotNull(privateKey);
					alias = aliases[0];
				}
			} catch (Exception e) {
				fail("unable to retreive alias from PFX file");
			}
		}
		assertNotNull(privateKey, aliases != null ? ("found aliases "+Arrays.asList(aliases)+" in PFX file") : "no aliases found in PFX file");

		pipe.setKeystore("/Signature/certificate.pfx");
		pipe.setKeystorePassword(pfxPassword);
		pipe.setKeystoreAlias(alias); // GitHub Actions uses a different X509KeyManager, the first alias is 0 instead of 1;
		configureAndStartPipe();

		PipeRunResult prr = doPipe(new Message(testMessage));

		assertFalse(prr.getResult().isBinary(), "base64 signature should not be binary"); // Base64 is meant to be able to handle data as String. Having it as bytes causes wrong handling, e.g. as parameters to XSLT
		assertEquals(testSignature, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}

	@Test
	void testWithKeystoreAndKeyPairHavingDifferentPasswords() throws Exception {
		pipe.setKeystore("/Signature/ks_multipassword.jks");
		pipe.setKeystorePassword("geheim");
		pipe.setKeystoreType(KeystoreType.JKS);
		pipe.setKeystoreAlias("1");
		pipe.setKeystoreAliasPassword("test");
		configureAndStartPipe();

		PipeRunResult prr = doPipe(new Message(testMessage));

		assertFalse(prr.getResult().isBinary(), "base64 signature should not be binary"); // Base64 is meant to be able to handle data as String. Having it as bytes causes wrong handling, e.g. as parameters to XSLT
		assertEquals(multipasswordKSSignature, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}

	@Test
	void tryUsingSamePasswordForKeystoreAndKeyPairHavingDifferentPasswords() {
		pipe.setKeystore("/Signature/ks_multipassword.jks");
		pipe.setKeystorePassword("geheim");
		pipe.setKeystoreType(KeystoreType.JKS);
		pipe.setKeystoreAlias("1");

		LifecycleException e = assertThrows(LifecycleException.class, this::configureAndStartPipe);
		EncryptionException ee = assertInstanceOf(EncryptionException.class, e.getCause());
		assertThat(ee.getMessage(), Matchers.containsString("cannot obtain Private Key in alias [1] of keystore [/Signature/ks_multipassword.jks]"));
	}

	@Test
	void testWithKeystoreHavingMultipleEntriesWithSamePassword() throws Exception {
		pipe.setKeystore("/Signature/ks_multientry_samepassword.jks");
		pipe.setKeystorePassword("geheim");
		pipe.setKeystoreType(KeystoreType.JKS);
		pipe.setKeystoreAlias("1");
		pipe.setKeystoreAliasPassword("test");
		configureAndStartPipe();

		PipeRunResult prr = doPipe(new Message(testMessage));

		assertFalse(prr.getResult().isBinary(), "base64 signature should not be binary"); // Base64 is meant to be able to handle data as String. Having it as bytes causes wrong handling, e.g. as parameters to XSLT
		assertEquals(multipasswordKSSignature, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());

	}

	@Test
	void testTargetingSpecificKeyPairInMultiEntryKeystore() throws Exception {
		pipe.setKeystore("/Signature/ks_multientry_differentpassword.jks");
		pipe.setKeystorePassword("geheim");
		pipe.setKeystoreType(KeystoreType.JKS);
		pipe.setKeystoreAlias("2nd");
		pipe.setKeystoreAliasPassword("test2");
		configureAndStartPipe();

		PipeRunResult prr = doPipe(new Message(testMessage));

		assertFalse(prr.getResult().isBinary(), "base64 signature should not be binary"); // Base64 is meant to be able to handle data as String. Having it as bytes causes wrong handling, e.g. as parameters to XSLT
		assertEquals(multipasswordKSSignature, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());

	}

	@Test
	void testSignPem() throws Exception {
		pipe.setKeystore("/Signature/privateKey.key");
		pipe.setKeystoreType(KeystoreType.PEM);
		configureAndStartPipe();

		PipeRunResult prr = doPipe(new Message(testMessage));

		assertFalse(prr.getResult().isBinary(), "base64 signature should not be binary"); // Base64 is meant to be able to handle data as String. Having it as bytes causes wrong handling, e.g. as parameters to XSLT
		assertEquals(testSignature, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}

	@Test
	void testVerifyOK() throws Exception {
		pipe.setAction(Action.VERIFY);
		pipe.setKeystore("/Signature/certificate.pfx");
		pipe.setKeystorePassword("geheim");
		pipe.setKeystoreAlias("1");

		pipe.addParameter(new Parameter("signature", testSignature));

		PipeForward failure = new PipeForward();
		failure.setName("failure");
		pipe.addForward(failure);
		configureAndStartPipe();

		PipeRunResult prr = doPipe(new Message(testMessage));

		assertEquals(testMessage, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}

	@Test
	void testVerifyNotOK() throws Exception {
		pipe.setAction(Action.VERIFY);
		pipe.setKeystore("/Signature/certificate.pfx");
		pipe.setKeystorePassword("geheim");
		pipe.setKeystoreAlias("1");

		pipe.addParameter(new Parameter("signature", testSignature));

		PipeForward failure = new PipeForward();
		failure.setName("failure");
		pipe.addForward(failure);
		configureAndStartPipe();

		PipeRunResult prr = doPipe(new Message("otherMessage"));

		assertEquals("failure", prr.getPipeForward().getName());
	}

	@Test
	void testVerifyOKPEM() throws Exception {
		pipe.setAction(Action.VERIFY);
		pipe.setKeystore("/Signature/certificate.crt");
		pipe.setKeystoreType(KeystoreType.PEM);

		pipe.addParameter(new Parameter("signature", testSignature));

		PipeForward failure = new PipeForward();
		failure.setName("failure");
		pipe.addForward(failure);
		configureAndStartPipe();

		PipeRunResult prr = doPipe(new Message(testMessage));

		assertEquals(testMessage, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}

}
