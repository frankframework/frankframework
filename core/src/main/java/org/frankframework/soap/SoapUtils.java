/*
   Copyright 2026 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.soap;

import java.io.IOException;
import java.security.KeyStore;

import javax.crypto.SecretKey;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.crypto.dsig.DigestMethod;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPConstants;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.soap.SOAPPart;

import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.engine.WSSecurityEngine;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.message.WSSecEncrypt;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.apache.wss4j.dom.message.WSSecTimestamp;
import org.apache.wss4j.dom.processor.Processor;
import org.apache.xml.security.algorithms.JCEMapper;
import org.jspecify.annotations.NonNull;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import lombok.Getter;

import org.frankframework.stream.Message;

public class SoapUtils {

	private SoapUtils() {
		/* This utility class should not be instantiated */
	}

	static {
		JCEMapper.registerDefaultAlgorithms();
		WSSConfig.init();
	}

	@NonNull
	public static Document toSoapDocument(Message soapMessage) throws SOAPException, IOException {
		if (soapMessage.asObject() instanceof Document d) {
			// If the source is already a Document (not NODE) it we can directly use it.
			// Yes we check things twice, but it keeps my compiler happy...
			return d;
		}

		// Support for different soap versions.
		String soapVersion = soapMessage.getContext().get(SoapContext.SOAP_VERSION_KEY);
		String soapProtocol = "1.2".equals(soapVersion) ? SOAPConstants.SOAP_1_2_PROTOCOL : SOAPConstants.SOAP_1_1_PROTOCOL;

		MessageFactory factory = MessageFactory.newInstance(soapProtocol);
		SOAPMessage msg = factory.createMessage();
		SOAPPart part = msg.getSOAPPart();

		try {
			part.setContent(soapMessage.asSource());

			// Create unsigned envelope
			SOAPEnvelope unsignedEnvelope = part.getEnvelope();
			return unsignedEnvelope.getOwnerDocument();
		} catch (SAXException e) {
			throw new IOException("unable to read document", e);
		}
	}

	@SuppressWarnings("java:S107")
	public static Message encryptMessage(Message soapMessage, KeyStore keystore, String certificateName, SecretKey symmetricKey, boolean includeCertificateInMessage, KeyIdentifierType kiType, DigestAlgorithm digestAlgorithm, KeyEncryptionAlgorithm keAlgorithm, DataEncryptionAlgorithm deAlgorithm, int ttl) throws SOAPException, IOException, WSSecurityException {
		Document doc = toSoapDocument(soapMessage);

		// create security header and insert it into unsigned envelope
		WSSecHeader secHeader = new WSSecHeader(doc);
		secHeader.insertSecurityHeader();
		secHeader.setMustUnderstand(true);

		Crypto crypto = new KeyStoreCrypto(keystore);

		WSSecEncrypt encrypt = new WSSecEncrypt(secHeader);
		encrypt.setUserInfo(certificateName);

		encrypt.setKeyEncAlgo(keAlgorithm.getAlgorithm()); // Key EncryptionMethod (rsa-oaep-mgf1p)
		encrypt.setEncryptSymmKey(true);

		encrypt.setKeyIdentifierType(kiType.getType()); // embeds KeyInfo with BinarySecurityToken ref
		encrypt.setSymmetricEncAlgorithm(deAlgorithm.getAlgorithm()); // Data EncryptionMethod (aes256-cbc)
		encrypt.setDigestAlgorithm(digestAlgorithm.getAlgorithm()); // DigestMethod
		encrypt.setIncludeEncryptionToken(includeCertificateInMessage);

		setTimestamp(secHeader, ttl);

		Document encryptedDocument = encrypt.build(crypto, symmetricKey);

		return new Message(encryptedDocument);
	}

	@SuppressWarnings("java:S107")
	public static Message signMessage(Message soapMessage, KeyStore keystore, String certificateName, String certificatePassword, boolean includeCertificateInMessage, KeyIdentifierType kiType, DigestAlgorithm digestAlgorithm, SignatureAlgorithm signatureAlgorithm, int ttl) throws SOAPException, IOException, WSSecurityException {
		Document doc = SoapUtils.toSoapDocument(soapMessage);

		// create security header and insert it into unsigned envelope
		WSSecHeader secHeader = new WSSecHeader(doc);
		secHeader.insertSecurityHeader();
		secHeader.setMustUnderstand(true);

		Crypto crypto = new KeyStoreCrypto(keystore);

		WSSecSignature sign = new WSSecSignature(secHeader);
		sign.setUserInfo(certificateName, certificatePassword);
		sign.setAddInclusivePrefixes(true);
		sign.setSignatureAlgorithm(signatureAlgorithm.getAlgorithm());
		sign.setIncludeSignatureToken(includeCertificateInMessage);
		sign.setDigestAlgo(digestAlgorithm.getAlgorithm());
		sign.setUseSingleCertificate(true);

		sign.setKeyIdentifierType(kiType.getType());

		setTimestamp(secHeader, ttl);

		Document encryptedDocument = sign.build(crypto);

		return new Message(encryptedDocument);
	}

	/**
	 * Add a WS-Sec Timestamp if not yet present.
	 */
	private static void setTimestamp(WSSecHeader secHeader, int ttl) {
		if (ttl  < 1) {
			return; // No need to set a TTL.
		}

		NodeList timestamp = secHeader.getSecurityHeaderElement().getElementsByTagNameNS(WSS4JConstants.WSU_NS, WSS4JConstants.TIMESTAMP_TOKEN_LN);
		if (timestamp.getLength() == 0) {
			WSSecTimestamp timestampBuilder = new WSSecTimestamp(secHeader);
			timestampBuilder.setTimeToLive(ttl);
			timestampBuilder.build();
		}
	}

	public static Message decryptMessage(Message soapMessage, KeyStore keystore, String certificateName, String certificatePassword, boolean removeSecurityHeader) throws SOAPException, IOException, WSSecurityException {
		RequestData requestData = getRequestData(keystore, certificateName, certificatePassword, WSConstants.ENCR);
		return decryptMessage(soapMessage, requestData, removeSecurityHeader);
	}

	private static Message decryptMessage(Message soapMessage, RequestData requestData, boolean removeSecurityHeader) throws SOAPException, IOException, WSSecurityException {
		return processMessage(soapMessage, requestData, removeSecurityHeader, WSConstants.ENCR);
	}

	public static Message verifyMessage(Message soapMessage, KeyStore keystore, String certificateName, String certificatePassword, boolean removeSecurityHeader) throws SOAPException, IOException, WSSecurityException {
		RequestData requestData = getRequestData(keystore, certificateName, certificatePassword, WSConstants.SIGN);
		return verifyMessage(soapMessage, requestData, removeSecurityHeader);
	}

	private static Message verifyMessage(Message soapMessage, RequestData requestData, boolean removeSecurityHeader) throws SOAPException, IOException, WSSecurityException {
		return processMessage(soapMessage, requestData, removeSecurityHeader, WSConstants.SIGN);
	}

	private static Message processMessage(Message soapMessage, RequestData requestData, boolean removeSecurityHeader, Integer actionType) throws SOAPException, IOException, WSSecurityException {
		final Document doc = toSoapDocument(soapMessage);
		final WSHandlerResult result;
		try {
			WSSecurityEngine engine = new WSSecurityEngine();
			engine.setWssConfig(requestData.getWssConfig());
			result = engine.processSecurityHeader(doc, requestData);
		} catch (IllegalArgumentException | IllegalStateException | WSSecurityException e) {
			// Catch WSSecurityException to prevent the locale based exception trace and throw a generic instead.
			throw new CustomWSSecurityException("unable to process security header", e);
		}

		if (result == null || result.getResults().isEmpty()) {
			// Return message as-is. It appears to not be encrypted/signed.
			throw new CustomWSSecurityException("message does not appear to be signed nor encrypted");
		}

		boolean processed = result.getResults().stream()
				.map(e -> e.get(WSSecurityEngineResult.TAG_ACTION))
				.filter(Integer.class::isInstance)
				.map(Integer.class::cast)
				.anyMatch(action -> (action & actionType) == actionType);
		// Since there may also be other actions present, decrypt vs verify. Only validate the given actionType.

		if (!processed) {
			throw new CustomWSSecurityException("some signature references were not processed");
		}

		if (removeSecurityHeader) {
			WSSecHeader secHeader = new WSSecHeader(doc);
			secHeader.removeSecurityHeader();
		}

		return new Message(doc);
	}

	private static @NonNull RequestData getRequestData(@NonNull KeyStore keystore, @NonNull String certificateName, @NonNull String certificatePassword, @NonNull int type) {
		Crypto crypto = new KeyStoreCrypto(keystore);
		RequestData requestData = new RequestData();

		// By default the following two Processors are used.
		// WSConstants.ENCRYPTED_KEY, org.apache.wss4j.dom.processor.EncryptedKeyProcessor.class
		// WSConstants.SIGNATURE, org.apache.wss4j.dom.processor.SignatureProcessor.class
		// If we disable one, it's skipped. This allows you to only verify a signature or decrypt a message.
		WSSConfig defaultWssConfig = WSSConfig.getNewInstance();
		if (type == WSConstants.ENCR) {
			requestData.setDecCrypto(crypto);
			defaultWssConfig.setProcessor(WSConstants.SIGNATURE, (Processor) null);
		} else {
			requestData.setSigVerCrypto(crypto);
			defaultWssConfig.setProcessor(WSConstants.ENCRYPTED_KEY, (Processor) null);
		}
		requestData.setWssConfig(defaultWssConfig);


		requestData.setCallbackHandler(callbacks -> {
			for (Callback callback : callbacks) {
				if (callback instanceof WSPasswordCallback pc && WSPasswordCallback.DECRYPT == pc.getUsage()) {
					if (certificateName.equalsIgnoreCase(pc.getIdentifier())) {
						pc.setPassword(certificatePassword);
					}
				} else {
					throw new UnsupportedCallbackException(callback, "Unknown Callback");
				}
			}
		});
		return requestData;
	}

	/**
	 * This implementation does not use a LOCALE and therefor does not rely on either
	 * {@code org/apache/xml/security/resource/xmlsecurity_en.properties} or
	 * a JVM default {@code com/sun/org/apache/xml/internal/security/resource/xmlsecurity_en.properties}.
	 */
	public static final class CustomWSSecurityException extends WSSecurityException {

		public CustomWSSecurityException(String message) {
			super(ErrorCode.FAILURE, "empty", new Object[] { message });
		}

		public CustomWSSecurityException(String message, Exception exception) {
			super(ErrorCode.FAILURE, exception, "empty", new Object[] { message });
		}
	}

	public enum KeyEncryptionAlgorithm {
		RSA_OAEP(WSS4JConstants.KEYTRANSPORT_RSAOAEP);

		@Getter
		private final String algorithm;

		KeyEncryptionAlgorithm(String algorithm) {
			this.algorithm = algorithm;
		}
	}

	public enum DataEncryptionAlgorithm {
		AES_256(WSS4JConstants.AES_256);

		@Getter
		private final String algorithm;

		DataEncryptionAlgorithm(String algorithm) {
			this.algorithm = algorithm;
		}
	}

	public enum KeyIdentifierType {
		ISSUER_SERIAL(WSConstants.ISSUER_SERIAL),
		ISSUER_SERIAL_QUOTE_FORMAT(WSConstants.ISSUER_SERIAL_QUOTE_FORMAT),
		BST_DIRECT_REFERENCE(WSConstants.BST_DIRECT_REFERENCE),
		X509_KEY_IDENTIFIER(WSConstants.X509_KEY_IDENTIFIER),
		THUMBPRINT_IDENTIFIER(WSConstants.THUMBPRINT_IDENTIFIER),
		SKI_KEY_IDENTIFIER(WSConstants.SKI_KEY_IDENTIFIER),
		KEY_VALUE(WSConstants.KEY_VALUE);

		@Getter
		private final int type;

		KeyIdentifierType(int type) {
			this.type = type;
		}
	}

	public enum DigestAlgorithm {
		SHA1(DigestMethod.SHA1),
		SHA224(DigestMethod.SHA224),
		SHA256(DigestMethod.SHA256),
		SHA384(DigestMethod.SHA384),
		SHA512(DigestMethod.SHA512),
		SHA3_224(DigestMethod.SHA3_224),
		SHA3_256(DigestMethod.SHA3_256),
		SHA3_384(DigestMethod.SHA3_384),
		SHA3_512(DigestMethod.SHA3_512);

		@Getter
		private final String algorithm;

		DigestAlgorithm(String algorithm) {
			this.algorithm = algorithm;
		}
	}

	public enum SignatureAlgorithm {
		RSA_SHA1(WSS4JConstants.RSA_SHA1),
		RSA_SHA256(WSS4JConstants.RSA_SHA256),
		RSA_SHA512(WSS4JConstants.RSA_SHA512);

		@Getter
		private final String algorithm;

		SignatureAlgorithm(String algorithm) {
			this.algorithm = algorithm;
		}
	}
}
