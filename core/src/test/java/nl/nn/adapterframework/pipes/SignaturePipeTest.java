package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URL;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.Arrays;

import javax.net.ssl.KeyManager;
import javax.net.ssl.X509KeyManager;

import org.junit.Test;

import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.encryption.KeystoreType;
import nl.nn.adapterframework.encryption.PkiUtil;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.pipes.SignaturePipe.Action;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;

public class SignaturePipeTest extends PipeTestBase<SignaturePipe> {

	private String testMessage = "xyz";
	private String testSignature = "JBKjNltZoFlQTsBgstpnIB4itBxzAohRXGpIWuIQh51F64P4WdT+R/55v+cHrPsQ2B49GhROeFUyy7kafOKTfMTjm7DQ5yT/srImFTlZZZbHbvQns2NWBE8DoQKt6SOYowDNIJY5qDV+82k6xY2BcTcZoiAPB53F3rEkfzz/QkxcFiCKvtg2voG1WyVkyoue10404UXIkSXv0ySYnRBRugdPO1DKyUwL6FS5tP2p8toBVzeRT6rMkEwuU3A5riQpdnEOi0ckeFvSNU3Cdgdah4HWd+48gXzBE6Uwu/BMOrD/5mRUnS0wmPn7dajkjHNC2r9+C1jxlFy3NIim1rS2iA==";
	private String multipasswordKSSignature = "0b5443UL24hO4WaMif2G9k0wcpxboEhdDQEldui22NZtNfLwHjiTMosCZcwi2E3JEk/ZRU5JNfjwKoRw4qjI+dWo+7b3Bliny0KTjVXpAJrpDIJJXn25TMW7q2U+mpWl05ygLs3p9jjLSukE+5pi0WofGsQ7hco4KjR+uzfp+TfEmkEyG3dsxhDaaxXa8OCbrchmPrzkoMT4sJHT7k7aTpKX4Wg/mGfUpuMBy0G90CWAW72pKbBhxsKrFa3mqrHCUtYQ9Zb1X0PJtFLaaj/iQD6BBmE4Vl4YGD5ZRSsMCyZ9p49oiD2skHkwEbozfQ8vyXa3Udf4Eo26eMaiMw5goA==";


	@Override
	public SignaturePipe createPipe() {
		return new SignaturePipe();
	}

	@Test
	public void testSign() throws Exception {
		String pfxCertificate = "/Signature/certificate.pfx";
		String pfxPassword = "geheim";

		URL pfxURL = ClassUtils.getResourceURL(pfxCertificate);
		assertNotNull("PFX file not found", pfxURL);
		KeyStore keystore = PkiUtil.createKeyStore(pfxURL, pfxPassword, KeystoreType.PKCS12, "junittest");
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
				System.out.println("unable to retreive alias from PFX file");
			}
		}
		assertNotNull((aliases != null) ? ("found aliases "+Arrays.asList(aliases)+" in PFX file") : "no aliases found in PFX file", privateKey);

		pipe.setKeystore("/Signature/certificate.pfx");
		pipe.setKeystorePassword(pfxPassword);
		pipe.setKeystoreAlias(alias); //GitHub Actions uses a different X509KeyManager, the first alias is 0 instead of 1;
		configureAndStartPipe();

		PipeRunResult prr = doPipe(new Message(testMessage));

		assertFalse("base64 signature should not be binary", prr.getResult().isBinary()); // Base64 is meant to be able to handle data as String. Having it as bytes causes wrong handling, e.g. as parameters to XSLT
		assertEquals(testSignature, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}
	
	@Test
	public void testWithKeystoreAndKeyPairHavingDifferentPasswords() throws Exception {
		pipe.setKeystore("/Signature/ks_multipassword.jks");
		pipe.setKeystorePassword("geheim");
		pipe.setKeystoreType(KeystoreType.JKS);
		pipe.setKeystoreAlias("1");
		pipe.setKeystoreAliasPassword("test");
		configureAndStartPipe();

		PipeRunResult prr = doPipe(new Message(testMessage));

		assertFalse("base64 signature should not be binary", prr.getResult().isBinary()); // Base64 is meant to be able to handle data as String. Having it as bytes causes wrong handling, e.g. as parameters to XSLT
		assertEquals(multipasswordKSSignature, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}

	@Test
	public void tryUsingSamePasswordForKeystoreAndKeyPairHavingDifferentPasswords() throws Exception {
		pipe.setKeystore("/Signature/ks_multipassword.jks");
		pipe.setKeystorePassword("geheim");
		pipe.setKeystoreType(KeystoreType.JKS);
		pipe.setKeystoreAlias("1");

		exception.expect(PipeStartException.class);
		exception.expectMessage("Cannot obtain Private Key in alias [1]");
		configureAndStartPipe();

	}

	@Test
	public void testWithKeystoreHavingMultipleEntriesWithSamePassword() throws Exception {
		pipe.setKeystore("/Signature/ks_multientry_samepassword.jks");
		pipe.setKeystorePassword("geheim");
		pipe.setKeystoreType(KeystoreType.JKS);
		pipe.setKeystoreAlias("1");
		pipe.setKeystoreAliasPassword("test");
		configureAndStartPipe();

		PipeRunResult prr = doPipe(new Message(testMessage));

		assertFalse("base64 signature should not be binary", prr.getResult().isBinary()); // Base64 is meant to be able to handle data as String. Having it as bytes causes wrong handling, e.g. as parameters to XSLT
		assertEquals(multipasswordKSSignature, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());

	}

	@Test
	public void testTargetingSpecificKeyPairInMultiEntryKeystore() throws Exception {
		pipe.setKeystore("/Signature/ks_multientry_differentpassword.jks");
		pipe.setKeystorePassword("geheim");
		pipe.setKeystoreType(KeystoreType.JKS);
		pipe.setKeystoreAlias("2nd");
		pipe.setKeystoreAliasPassword("test2");
		configureAndStartPipe();

		PipeRunResult prr = doPipe(new Message(testMessage));

		assertFalse("base64 signature should not be binary", prr.getResult().isBinary()); // Base64 is meant to be able to handle data as String. Having it as bytes causes wrong handling, e.g. as parameters to XSLT
		assertEquals(multipasswordKSSignature, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());

	}

	@Test
	public void testSignPem() throws Exception {
		pipe.setKeystore("/Signature/privateKey.key");
		pipe.setKeystoreType(KeystoreType.PEM);
		configureAndStartPipe();
		
		PipeRunResult prr = doPipe(new Message(testMessage));
		
		assertFalse("base64 signature should not be binary", prr.getResult().isBinary()); // Base64 is meant to be able to handle data as String. Having it as bytes causes wrong handling, e.g. as parameters to XSLT
		assertEquals(testSignature, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}

	@Test
	public void testVerifyOK() throws Exception {
		pipe.setAction(Action.VERIFY);
		pipe.setKeystore("/Signature/certificate.pfx");
		pipe.setKeystorePassword("geheim");
		pipe.setKeystoreAlias("1");
		
		pipe.addParameter(new Parameter("signature", testSignature));
		
		PipeForward failure = new PipeForward();
		failure.setName("failure");
		pipe.registerForward(failure);
		configureAndStartPipe();
		
		PipeRunResult prr = doPipe(new Message(testMessage));

		assertEquals(testMessage, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}

	@Test
	public void testVerifyNotOK() throws Exception {
		pipe.setAction(Action.VERIFY);
		pipe.setKeystore("/Signature/certificate.pfx");
		pipe.setKeystorePassword("geheim");
		pipe.setKeystoreAlias("1");
		
		pipe.addParameter(new Parameter("signature", testSignature));
		
		PipeForward failure = new PipeForward();
		failure.setName("failure");
		pipe.registerForward(failure);
		configureAndStartPipe();
		
		PipeRunResult prr = doPipe(new Message("otherMessage"));

		assertEquals("failure", prr.getPipeForward().getName());
	}

	@Test
	public void testVerifyOKPEM() throws Exception {
		pipe.setAction(Action.VERIFY);
		pipe.setKeystore("/Signature/certificate.crt");
		pipe.setKeystoreType(KeystoreType.PEM);
		
		pipe.addParameter(new Parameter("signature", testSignature));
		
		PipeForward failure = new PipeForward();
		failure.setName("failure");
		pipe.registerForward(failure);
		configureAndStartPipe();
		
		PipeRunResult prr = doPipe(new Message(testMessage));

		assertEquals(testMessage, prr.getResult().asString());
		assertEquals("success", prr.getPipeForward().getName());
	}

}
