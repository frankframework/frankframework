package org.frankframework.soap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.frankframework.encryption.KeystoreConfiguration;
import org.frankframework.pipes.PipeTestBase;
import org.frankframework.soap.CryptoSoapValidator.Operation;

class CryptoSoapValidatorTest extends PipeTestBase<CryptoSoapValidator> {

	@Override
	public CryptoSoapValidator createPipe() {
		return new CryptoSoapValidator();
	}

	@Test
	void defaultsAreExpected() throws Exception {
		assertTrue(pipe.isIncludeCertificateInMessage());
		assertEquals(300, pipe.getTtl());
		assertTrue(pipe.isRemoveSecurityHeader());
		assertEquals(SoapUtils.KeyEncryptionAlgorithm.RSA_OAEP, pipe.getKeyEncryptionAlgorithm());
		assertEquals(SoapUtils.DataEncryptionAlgorithm.AES_256, pipe.getDataEncryptionAlgorithm());
		assertEquals(SoapUtils.KeyIdentifierType.X509_KEY_IDENTIFIER, pipe.getKeyIdentifier());
		assertEquals(SoapUtils.DigestAlgorithm.SHA256, pipe.getDigestAlgorithm());
		assertEquals(SoapUtils.SignatureAlgorithm.RSA_SHA256, pipe.getSignatureAlgorithm());
		assertNull(pipe.getKeystoreConfiguration());
		assertEquals(List.of(), getOperations());
	}

	@Test
	void settersUpdateProperties() throws Exception {
		KeystoreConfiguration keystoreConfiguration = new KeystoreConfiguration();

		pipe.setIncludeCertificateInMessage(false);
		pipe.setTtl(42);
		pipe.setRemoveSecurityHeader(false);
		pipe.setKeyEncryptionAlgorithm(SoapUtils.KeyEncryptionAlgorithm.RSA_15);
		pipe.setDataEncryptionAlgorithm(SoapUtils.DataEncryptionAlgorithm.AES_128);
		pipe.setKeyIdentifier(SoapUtils.KeyIdentifierType.THUMBPRINT_SHA1);
		pipe.setDigestAlgorithm(SoapUtils.DigestAlgorithm.SHA1);
		pipe.setSignatureAlgorithm(SoapUtils.SignatureAlgorithm.RSA_SHA1);
		pipe.setKeystoreConfiguration(keystoreConfiguration);
		pipe.setOperations(Operation.SIGN, Operation.ENCRYPT);

		assertFalse(pipe.isIncludeCertificateInMessage());
		assertEquals(42, pipe.getTtl());
		assertFalse(pipe.isRemoveSecurityHeader());
		assertEquals(SoapUtils.KeyEncryptionAlgorithm.RSA_15, pipe.getKeyEncryptionAlgorithm());
		assertEquals(SoapUtils.DataEncryptionAlgorithm.AES_128, pipe.getDataEncryptionAlgorithm());
		assertEquals(SoapUtils.KeyIdentifierType.THUMBPRINT_SHA1, pipe.getKeyIdentifier());
		assertEquals(SoapUtils.DigestAlgorithm.SHA1, pipe.getDigestAlgorithm());
		assertEquals(SoapUtils.SignatureAlgorithm.RSA_SHA1, pipe.getSignatureAlgorithm());
		assertSame(keystoreConfiguration, pipe.getKeystoreConfiguration());
		assertEquals(List.of(Operation.SIGN, Operation.ENCRYPT), getOperations());
	}

	@SuppressWarnings("unchecked")
	private List<Operation> getOperations() throws Exception {
		Field operationsField = CryptoSoapValidator.class.getDeclaredField("operations");
		operationsField.setAccessible(true);
		return (List<Operation>) operationsField.get(pipe);
	}
}
