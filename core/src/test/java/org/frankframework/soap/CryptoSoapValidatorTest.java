package org.frankframework.soap;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URL;
import java.security.KeyStore;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

import org.frankframework.pipes.PipeTestBase;
import org.frankframework.stream.Message;
import org.frankframework.stream.UrlMessage;
import org.frankframework.testutil.TestFileUtils;

class CryptoSoapValidatorTest extends PipeTestBase<CryptoSoapValidator> {

	@Override
	public CryptoSoapValidator createPipe() {
		return new CryptoSoapValidator();
	}

	@Test
	void testRemoveSecurityHeader() {
		URL file = TestFileUtils.getTestFileURL("/Soap/Encryption/SZeebraSoap.xml");
		assertNotNull(file); // ensure we can find the file

		String certificateName = "myCustomCertificateName";
		String certificatePass = "Super$3cure";
		KeyStore keystore = createDummyKeyStoreWithNullKeyPassword(certificateName, certificatePass);

		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(256);
		SecretKey secretKey = keyGen.generateKey();

		Message encrypted = SoapUtils.encryptMessage(new UrlMessage(file), keystore, certificateName, secretKey, true,
				SoapUtils.KeyIdentifierType.THUMBPRINT_SHA1, SoapUtils.DigestAlgorithm.SHA1, SoapUtils.KeyEncryptionAlgorithm.RSA_OAEP, SoapUtils.DataEncryptionAlgorithm.AES_256, 300);

		pipe.setOperations(CryptoSoapValidator.Operation.VERIFY);
	}
}
