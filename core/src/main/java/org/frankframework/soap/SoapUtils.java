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
import java.util.Objects;

import javax.crypto.SecretKey;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;

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
import org.apache.xml.security.algorithms.JCEMapper;
import org.jspecify.annotations.NonNull;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import org.frankframework.stream.Message;

public class SoapUtils {

	static {
		JCEMapper.registerDefaultAlgorithms();
		WSSConfig.init();
	}

	@NonNull
	public static Document toSoapDocument(Message soapMessage) throws SOAPException, IOException {
		if (soapMessage.isRequestOfType(Document.class) && soapMessage.asObject() instanceof Document d) {
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

	public static Message encryptMessage(Message soapMessage, KeyStore keystore, String certificateName, SecretKey symmetricKey, boolean includeCertificateInMessage) throws SOAPException, IOException, WSSecurityException {
		Document doc = toSoapDocument(soapMessage);

		// create security header and insert it into unsigned envelope
		WSSecHeader secHeader = new WSSecHeader(doc);
		secHeader.insertSecurityHeader();
		secHeader.setMustUnderstand(true);

		Crypto crypto = new KeyStoreCrypto(keystore);

		WSSecEncrypt encrypt = new WSSecEncrypt(secHeader);
		encrypt.setUserInfo(certificateName);
/*
	// Encrypt a specific element in the header by namespace + localname
	List<WSEncryptionPart> parts = new ArrayList<>();

String soapNamespace = WSSecurityUtil.getSOAPNamespace(doc.getDocumentElement());
if (elementToEncrypt.getParentNode().getNamespaceURI().equals(soapNamespace)
                    && WSConstants.ELEM_HEADER.equals(elementToEncrypt.getParentNode().getLocalName())) {
                }
	//doc.getElementsByTagName("Body").item(0);
	// Encrypt the entire Body (default, optional to keep)
	parts.add(new WSEncryptionPart("Body", "http://schemas.xmlsoap.org/soap/envelope/", "Content"));

	// Encrypt a custom header element
	parts.add(new WSEncryptionPart("myHeader", "http://www.test.nl/v0101", "Element"));

	encrypt.getParts().addAll(parts);
*/
		encrypt.setKeyEncAlgo(WSS4JConstants.KEYTRANSPORT_RSAOAEP); // Key EncryptionMethod (rsa-oaep-mgf1p)
		encrypt.setEncryptSymmKey(true);

		encrypt.setKeyIdentifierType(WSConstants.THUMBPRINT_IDENTIFIER); // embeds KeyInfo with BinarySecurityToken ref
		encrypt.setSymmetricEncAlgorithm(WSS4JConstants.AES_256); // Data EncryptionMethod (aes256-cbc)
		encrypt.setDigestAlgorithm(WSS4JConstants.SHA1); // DigestMethod
		encrypt.setIncludeEncryptionToken(includeCertificateInMessage);

		// Add a Timestamp
		WSSecTimestamp timestampBuilder = new WSSecTimestamp(secHeader);
		timestampBuilder.setTimeToLive(300);
//		timestampBuilder.setIdAllocator(idAllocator);
		timestampBuilder.build();

		Document encryptedDocument = encrypt.build(crypto, symmetricKey);

		return new Message(encryptedDocument);
	}

	public static Message decryptMessage(Message soapMessage, KeyStore keystore, String certificateName, String certificatePassword, boolean removeSecurityHeader) throws SOAPException, IOException, WSSecurityException {
		RequestData requestData = getRequestData(keystore, certificateName, certificatePassword);
		return decryptMessage(soapMessage, requestData, removeSecurityHeader);
	}

	private static Message decryptMessage(Message soapMessage, RequestData requestData, boolean removeSecurityHeader) throws SOAPException, IOException, WSSecurityException {
		final Document doc = toSoapDocument(soapMessage);
		final WSHandlerResult result;
		try {
			WSSecurityEngine engine = new WSSecurityEngine();
			result = engine.processSecurityHeader(doc, requestData);
		} catch (IllegalArgumentException | IllegalStateException | WSSecurityException e) {
			// Catch WSSecurityException to prevent the locale based exception trace and throw a generic instead.
			throw new CustomWSSecurityException("unable to process security header", e);
		}

		if (result == null || result.getResults().isEmpty()) {
			// Return message as-is. It appears to not be encrypted/signed.
			return new Message(doc);
		}

		boolean encryptionProcessed = result.getResults().stream()
				.map(e -> e.get(WSSecurityEngineResult.TAG_ACTION))
				.filter(Objects::nonNull)
				.map(Integer.class::cast)
				.anyMatch(action -> (action & WSConstants.ENCR) == WSConstants.ENCR);

		if (!encryptionProcessed) {
			throw new CustomWSSecurityException("some encryption references were not processed");
		}

		if (removeSecurityHeader) {
			WSSecHeader secHeader = new WSSecHeader(doc);
			secHeader.removeSecurityHeader();
		}

		return new Message(doc);
	}

	public static Message signMessage(Message soapMessage, KeyStore keystore, String certificateName, String certificatePassword, boolean includeCertificateInMessage) throws SOAPException, IOException, WSSecurityException {
		Document doc = SoapUtils.toSoapDocument(soapMessage);

		// create security header and insert it into unsigned envelope
		WSSecHeader secHeader = new WSSecHeader(doc);
		secHeader.insertSecurityHeader();
		secHeader.setMustUnderstand(true);

		Crypto crypto = new KeyStoreCrypto(keystore);

		WSSecSignature sign = new WSSecSignature(secHeader);
		sign.setUserInfo(certificateName, certificatePassword);
//		sign.setKeyIdentifierType(WSConstants.BST_DIRECT_REFERENCE);
//		sign.setSignatureAlgorithm(WSConstants.RSA_SHA256);
//		sign.setDigestAlgo(WSConstants.SHA256);
		sign.setIncludeSignatureToken(includeCertificateInMessage);

		// Add a Timestamp
		WSSecTimestamp timestampBuilder = new WSSecTimestamp(secHeader);
		timestampBuilder.setTimeToLive(300);
		timestampBuilder.build();

		Document encryptedDocument = sign.build(crypto);

		return new Message(encryptedDocument);
	}

	public static Message verifyMessage(Message soapMessage, KeyStore keystore, String certificateName, String certificatePassword, boolean removeSecurityHeader) throws SOAPException, IOException, WSSecurityException {
		RequestData requestData = getRequestData(keystore, certificateName, certificatePassword);
		return verifyMessage(soapMessage, requestData, removeSecurityHeader);
	}

	private static Message verifyMessage(Message soapMessage, RequestData requestData, boolean removeSecurityHeader) throws SOAPException, IOException, WSSecurityException {
		final Document doc = toSoapDocument(soapMessage);
		final WSHandlerResult result;
		try {
			WSSecurityEngine engine = new WSSecurityEngine();
			result = engine.processSecurityHeader(doc, requestData);
		} catch (IllegalArgumentException | IllegalStateException | WSSecurityException e) {
			// Catch WSSecurityException to prevent the locale based exception trace and throw a generic instead.
			throw new CustomWSSecurityException("unable to process security header", e);
		}

		if (result == null || result.getResults().isEmpty()) {
			// Return message as-is. It appears to not be encrypted/signed.
			return new Message(doc);
		}

		boolean encryptionProcessed = result.getResults().stream()
				.map(e -> e.get(WSSecurityEngineResult.TAG_ACTION))
				.filter(Objects::nonNull)
				.map(Integer.class::cast)
				.anyMatch(action -> (action & WSConstants.SIGN) == WSConstants.SIGN);

		if (!encryptionProcessed) {
			throw new CustomWSSecurityException("some signature references were not processed");
		}

		if (removeSecurityHeader) {
			WSSecHeader secHeader = new WSSecHeader(doc);
			secHeader.removeSecurityHeader();
		}

		return new Message(doc);
	}

	private static @NonNull RequestData getRequestData(KeyStore keystore, String certificateName, String certificatePassword) {
		Crypto crypto = new KeyStoreCrypto(keystore);
		RequestData requestData = new RequestData();
		requestData.setSigVerCrypto(crypto);
		requestData.setDecCrypto(crypto);
		requestData.setCallbackHandler(callbacks -> {
			for (Callback callback : callbacks) {
				if (callback instanceof WSPasswordCallback pc && WSPasswordCallback.DECRYPT == pc.getUsage()) {
					if (certificateName.equals(pc.getIdentifier())) {
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
}
