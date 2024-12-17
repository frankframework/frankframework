/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.http.authentication;

import java.io.StringWriter;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.http.NameValuePair;
import org.apache.http.auth.Credentials;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.message.BasicNameValuePair;
import org.apache.xml.security.algorithms.MessageDigestAlgorithm;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;

import org.frankframework.util.DomBuilderException;
import org.frankframework.util.XmlUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.encryption.EncryptionException;
import org.frankframework.encryption.PkiUtil;
import org.frankframework.http.AbstractHttpSession;

public class SamlAssertionOauth extends AbstractOauthAuthenticator {

	private static final String SAML2_BEARER_GRANT_TYPE = "urn:ietf:params:oauth:grant-type:saml2-bearer";
	private static final String SAML2_NAMESPACE_URI = "urn:oasis:names:tc:SAML:2.0:assertion";

	private PrivateKey privateKey;
	private X509Certificate certificate;

	static {
		org.apache.xml.security.Init.init();
	}

	public SamlAssertionOauth(AbstractHttpSession session) throws HttpAuthenticationException {
		super(session);
	}

	@Override
	public void configure() throws ConfigurationException {
		if (session.getClientId() == null) {
			throw new ConfigurationException("clientId is required");
		}

		if (session.getClientSecret() == null) {
			throw new ConfigurationException("clientSecret is required");
		}

		try {
			privateKey = PkiUtil.getPrivateKey(session, "Creation of SAML assertion");

			Certificate loadedCertificate = PkiUtil.getCertificate(session, "Creation of SAML assertion");

			if (loadedCertificate instanceof X509Certificate x509certificate) {
				this.certificate = x509certificate;
			} else {
				throw new ConfigurationException("Certificate must be a X.509 certificate");
			}
		} catch (EncryptionException e) {
			throw new ConfigurationException(e);
		}
	}

	private String createAssertion() throws ParserConfigurationException, TransformerException, DomBuilderException, XMLSecurityException {
		// Generate SAML Assertion
		Document samlAssertion = generateSAMLAssertion();
		String docAsXml = documentToString(samlAssertion);

		Document document = XmlUtils.buildDomDocument(docAsXml);
		Document signedAssertion = signAssertion(document, privateKey, certificate);

		String signedAssertionXml = documentToString(signedAssertion);

		return Base64.getEncoder().encodeToString(signedAssertionXml.getBytes());
	}

	private Document generateSAMLAssertion() throws ParserConfigurationException {
		Instant nowInstant = Instant.now();

		String notBefore = nowInstant.minusSeconds(60).toString();
		String notOnOrAfter = nowInstant.plusSeconds(session.getSamlAssertionExpiry()).toString();
		String now = nowInstant.toString();

		// Create a new XML Document
		DocumentBuilderFactory docFactory = XmlUtils.getDocumentBuilderFactory(true);
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.newDocument();

		// Create the Assertion element
		Element assertion = doc.createElementNS(SAML2_NAMESPACE_URI, "saml2:Assertion");
		assertion.setAttribute("ID", "_" + UUID.randomUUID());
		assertion.setAttribute("Version", "2.0");
		assertion.setAttribute("IssueInstant", Instant.now().toString());
		assertion.setAttribute("xmlns:xs", "http://www.w3.org/2001/XMLSchema");
		assertion.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");

		doc.appendChild(assertion);

		// Add Issuer
		Element issuerElement = doc.createElementNS(SAML2_NAMESPACE_URI, "saml2:Issuer");
		issuerElement.setTextContent(session.getSamlIssuer());
		assertion.appendChild(issuerElement);

		// Add Subject
		Element subject = doc.createElementNS(SAML2_NAMESPACE_URI, "saml2:Subject");
		Element nameID = doc.createElementNS(SAML2_NAMESPACE_URI, "saml2:NameID");
		nameID.setTextContent(session.getSamlNameId());
		nameID.setAttribute("Format", "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified");
		subject.appendChild(nameID);
		assertion.appendChild(subject);

		Element subjectConfirmation = doc.createElementNS(SAML2_NAMESPACE_URI, "saml2:SubjectConfirmation");
		subjectConfirmation.setAttribute("Method", "urn:oasis:names:tc:SAML:2.0:cm:bearer");

		Element subjectConfirmationData = doc.createElementNS(SAML2_NAMESPACE_URI, "saml2:SubjectConfirmationData");
		subjectConfirmationData.setAttribute("NotOnOrAfter", notOnOrAfter);
		subjectConfirmationData.setAttribute("Recipient", session.getTokenEndpoint());

		subjectConfirmation.appendChild(subjectConfirmationData);
		subject.appendChild(subjectConfirmation);

		// Add Conditions
		Element conditions = doc.createElementNS(SAML2_NAMESPACE_URI, "saml2:Conditions");
		conditions.setAttribute("NotBefore", notBefore);
		conditions.setAttribute("NotOnOrAfter", notOnOrAfter);

		Element audienceRestriction = doc.createElementNS(SAML2_NAMESPACE_URI, "saml2:AudienceRestriction");

		Element audienceElement = doc.createElementNS(SAML2_NAMESPACE_URI, "saml2:Audience");
		audienceElement.setTextContent(session.getSamlAudience());

		audienceRestriction.appendChild(audienceElement);
		conditions.appendChild(audienceRestriction);
		assertion.appendChild(conditions);

		// Add AuthnStatement
		Element authnStatement = doc.createElementNS(SAML2_NAMESPACE_URI, "saml2:AuthnStatement");
		authnStatement.setAttribute("AuthnInstant", now);
		authnStatement.setAttribute("SessionIndex", UUID.randomUUID().toString());

		Element authnContext = doc.createElementNS(SAML2_NAMESPACE_URI, "saml2:AuthnContext");

		Element authnContextClassRef = doc.createElementNS(SAML2_NAMESPACE_URI, "saml2:AuthnContextClassRef");
		authnContextClassRef.setTextContent("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport");

		authnContext.appendChild(authnContextClassRef);
		authnStatement.appendChild(authnContext);
		assertion.appendChild(authnStatement);

		// Add attributeStatement
		Element attributeStatement = doc.createElementNS(SAML2_NAMESPACE_URI, "saml2:AttributeStatement");

		Element attribute = doc.createElementNS(SAML2_NAMESPACE_URI, "saml2:Attribute");
		attribute.setAttribute("Name", "api_key");

		Element attributeValue = doc.createElementNS(SAML2_NAMESPACE_URI, "saml2:AttributeValue");
		attributeValue.setAttribute("xsi:type", "xs:string");
		attributeValue.setTextContent(session.getClientId());

		attribute.appendChild(attributeValue);
		attributeStatement.appendChild(attribute);
		assertion.appendChild(attributeStatement);

		return doc;
	}

	private Document signAssertion(Document doc, PrivateKey privateKey, X509Certificate certificate) throws XMLSecurityException {
		// Register the "ID" attribute as type ID in the XML document
		Element rootElement = doc.getDocumentElement();
		rootElement.setIdAttribute("ID", true);

		// Create the XMLSignature object
		XMLSignature signature = new XMLSignature(doc, "", XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256);

		// Add transforms (Canonicalization)
		Transforms transforms = new Transforms(doc);
		transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE); // For signature element
		transforms.addTransform(Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS); // Canonicalization
		signature.addDocument("#" + rootElement.getAttribute("ID"), transforms, MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA256);

		// Add the KeyInfo to the Signature
		signature.addKeyInfo(certificate);
		signature.addKeyInfo(certificate.getPublicKey());

		// Append the signature to the root element
		rootElement.appendChild(signature.getElement());

		// Sign the document
		signature.sign(privateKey);

		return doc;
	}

	public String documentToString(Document document) throws TransformerException {
		TransformerFactory transformerFactory = XmlUtils.getTransformerFactory();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "no");
		transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "no");

		StringWriter writer = new StringWriter();
		transformer.transform(new DOMSource(document), new StreamResult(writer));
		return writer.toString().replace("\n", "").replace("\r", "");
	}

	@Override
	protected HttpEntityEnclosingRequestBase createRequest(Credentials credentials, List<NameValuePair> parameters) throws HttpAuthenticationException {
		parameters.add(new BasicNameValuePair("grant_type", SAML2_BEARER_GRANT_TYPE));
		parameters.add(new BasicNameValuePair("client_id", session.getClientId()));
		parameters.add(new BasicNameValuePair("client_secret", session.getClientSecret()));

		if (session.getScope() != null) {
			parameters.add(getScopeHeader());
		}

		try {
			parameters.add(new BasicNameValuePair("assertion", createAssertion()));
		} catch (Exception e) {
			throw new HttpAuthenticationException(e);
		}

		return createPostRequestWithForm(authorizationEndpoint, parameters);
	}

}
