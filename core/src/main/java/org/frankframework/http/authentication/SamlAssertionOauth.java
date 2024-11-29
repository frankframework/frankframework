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

import org.apache.http.NameValuePair;
import org.apache.http.auth.Credentials;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

import org.apache.http.message.BasicNameValuePair;

import org.apache.xml.security.algorithms.MessageDigestAlgorithm;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.http.AbstractHttpSession;

import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

import java.util.ArrayList;
import java.util.List;

public class SamlAssertionOauth extends AbstractOauthAuthenticator {

	private static final String SAML2_BEARER_GRANT_TYPE = "urn:ietf:params:oauth:grant-type:saml2-bearer";

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

		if (session.getPrivateKey() == null) {
			throw new ConfigurationException("privateKey is required");
		}
	}

	private String createAssertion() throws Exception {
		// Clean up the PEM content
		String privateKeyPem = session.getPrivateKey().replace("-----BEGIN PRIVATE KEY-----", "")
				.replace("-----END PRIVATE KEY-----", "")
				.replaceAll("\\s+", ""); // Remove headers, footers, and whitespace

		// Decode the Base64 content
		byte[] privateKeyBytes = java.util.Base64.getDecoder().decode(privateKeyPem);

		// Convert to PrivateKey object
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA"); // Ensure this matches the key algorithm
		PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		InputStream targetStream = new ByteArrayInputStream(session.getCertificate().getBytes());
		X509Certificate certificate = (X509Certificate) cf.generateCertificate(targetStream);

		// Generate SAML Assertion
		Document samlAssertion = generateSAMLAssertion();
		String docAsXml = documentToString(samlAssertion);

		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		docFactory.setNamespaceAware(true);
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		Document document = docBuilder.parse(new ByteArrayInputStream(docAsXml.getBytes()));

		Document signedAssertion = signAssertion(document, privateKey, certificate);

		String signedAssertionXml = documentToString(signedAssertion);

		return Base64.getEncoder().encodeToString(signedAssertionXml.getBytes());
	}

	private static final String namespaceURI = "urn:oasis:names:tc:SAML:2.0:assertion";

	private Document generateSAMLAssertion() throws Exception {
		String NotBefore = java.time.Instant.now().minusSeconds(60).toString();
		String NotOnOrAfter = java.time.Instant.now().plusSeconds(session.getAssertionExpiry()).toString();
		String now = java.time.Instant.now().toString();

		// Create a new XML Document
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		docFactory.setNamespaceAware(true);
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.newDocument();

		// Create the Assertion element
		Element assertion = doc.createElementNS(namespaceURI, "saml2:Assertion");
		assertion.setAttribute("ID", "_" + UUID.randomUUID());
		assertion.setAttribute("Version", "2.0");
		assertion.setAttribute("IssueInstant", java.time.Instant.now().toString());
		assertion.setAttribute("xmlns:xs", "http://www.w3.org/2001/XMLSchema");
		assertion.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");

		doc.appendChild(assertion);

		// Add Issuer
		Element issuerElement = doc.createElementNS(namespaceURI, "saml2:Issuer");
		issuerElement.setTextContent(session.getIssuer());
		assertion.appendChild(issuerElement);

		// Add Subject
		Element subject = doc.createElementNS(namespaceURI, "saml2:Subject");
		Element nameID = doc.createElementNS(namespaceURI, "saml2:NameID");
		nameID.setTextContent(session.getNameId());
		nameID.setAttribute("Format", "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified");
		subject.appendChild(nameID);
		assertion.appendChild(subject);

		Element subjectConfirmation = doc.createElementNS(namespaceURI, "saml2:SubjectConfirmation");
		subjectConfirmation.setAttribute("Method", "urn:oasis:names:tc:SAML:2.0:cm:bearer");

		Element subjectConfirmationData = doc.createElementNS(namespaceURI, "saml2:SubjectConfirmationData");
		subjectConfirmationData.setAttribute("NotOnOrAfter", NotOnOrAfter);
		subjectConfirmationData.setAttribute("Recipient", session.getTokenEndpoint());

		subjectConfirmation.appendChild(subjectConfirmationData);
		subject.appendChild(subjectConfirmation);

		// Add Conditions
		Element conditions = doc.createElementNS(namespaceURI, "saml2:Conditions");
		conditions.setAttribute("NotBefore", NotBefore);
		conditions.setAttribute("NotOnOrAfter", NotOnOrAfter);

		Element audienceRestriction = doc.createElementNS(namespaceURI, "saml2:AudienceRestriction");

		Element audienceElement = doc.createElementNS(namespaceURI, "saml2:Audience");
		audienceElement.setTextContent(session.getAudience());

		audienceRestriction.appendChild(audienceElement);
		conditions.appendChild(audienceRestriction);
		assertion.appendChild(conditions);

		// Add AuthnStatement
		Element authnStatement = doc.createElementNS(namespaceURI, "saml2:AuthnStatement");
		authnStatement.setAttribute("AuthnInstant", now);
		authnStatement.setAttribute("SessionIndex", UUID.randomUUID().toString());

		Element authnContext = doc.createElementNS(namespaceURI, "saml2:AuthnContext");

		Element authnContextClassRef = doc.createElementNS(namespaceURI, "saml2:AuthnContextClassRef");
		authnContextClassRef.setTextContent("urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport");

		authnContext.appendChild(authnContextClassRef);
		authnStatement.appendChild(authnContext);
		assertion.appendChild(authnStatement);

		// Add attributeStatement
		Element attributeStatement = doc.createElementNS(namespaceURI, "saml2:AttributeStatement");

		Element attribute = doc.createElementNS(namespaceURI, "saml2:Attribute");
		attribute.setAttribute("Name", "api_key");

		Element attributeValue = doc.createElementNS(namespaceURI, "saml2:AttributeValue");
		attributeValue.setAttribute("xsi:type", "xs:string");
		attributeValue.setTextContent(session.getClientId());

		attribute.appendChild(attributeValue);
		attributeStatement.appendChild(attribute);
		assertion.appendChild(attributeStatement);

		return doc;
	}

	private Document signAssertion(Document doc, PrivateKey privateKey, X509Certificate certificate) throws Exception {
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

	public String documentToString(Document document) throws Exception {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "no");
		transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "no");

		StringWriter writer = new StringWriter();
		transformer.transform(new DOMSource(document), new StreamResult(writer));
		return writer.toString().replace("\n", "").replace("\r", "");
	}

	@Override
	protected HttpEntityEnclosingRequestBase createRequest(Credentials credentials) throws HttpAuthenticationException {
		List<NameValuePair> parameters = new ArrayList<>();
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
