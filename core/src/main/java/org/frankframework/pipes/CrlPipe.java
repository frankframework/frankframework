/*
   Copyright 2016, 2020 Nationale-Nederlanden, 2023 WeAreFrank!

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
package org.frankframework.pipes;


import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;

import lombok.Getter;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.stream.Message;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.XmlBuilder;

/**
 * Pipe that reads a CRL from an input stream and transforms it to an XML.
 * The stream is closed after reading.
 *
 * Example configuration:
 * <pre>{@code
 * 	<pipe name="Read input CSV file"
 *                 className="org.frankframework.pipes.FixedResultPipe">
 * 		<param name="filename" sessionKey="filePathName"/>
 * 		<forward name="success" path="Process each Line" />
 * 	</pipe>
 * 	<pipe
 * 		name="Read issuer"
 * 		className="org.frankframework.pipes.FixedResultPipe"
 * 		filename="dir/issuer.cer"
 * 		storeResultInSessionKey="issuer">
 * 		<forward name="success" path="Read CRL" />
 * 	</pipe>
 * 	<pipe
 * 		name="Read CRL"
 * 		className="org.frankframework.pipes.FixedResultPipe"
 * 		fileName="dir/CRL.crl">
 * 		<forward name="success" path="Transform CRL" />
 * 	</pipe>
 * 	<pipe
 * 		name="Transform CRL"
 * 		className="org.frankframework.pipes.CrlPipe"
 * 		issuerSessionKey="issuer">
 * 		<forward name="success" path="EXIT" />
 * 	</pipe>
 * }</pre>
 *
 * @author Miel Hoppenbrouwers
 * @author Jaco de Groot
 * @author Tom van der Heijden
 */
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.TRANSLATOR)
public class CrlPipe extends FixedForwardPipe {
	private @Getter String issuerSessionKey;

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		X509CRL crl;
		try (InputStream inputStream = message.asInputStream()) {
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			crl = (X509CRL)cf.generateCRL(inputStream);
		} catch (CertificateException | IOException | CRLException e) {
			throw new PipeRunException(this, "Could not read CRL", e);
		}
		Message result = null;
		if (isCRLOK(crl, session.getMessage(getIssuerSessionKey()))) {
			XmlBuilder root = new XmlBuilder("SerialNumbers");
			for (X509CRLEntry e : crl.getRevokedCertificates()) {
				XmlBuilder serialNumber = new XmlBuilder("SerialNumber");
				serialNumber.setValue(e.getSerialNumber().toString(16));
				root.addSubElement(serialNumber);
			}
			result = root.asMessage();
		}
		return new PipeRunResult(getSuccessForward(), result);
	}

	private boolean isCRLOK(X509CRL x509crl, Message issuer) throws PipeRunException {
		try {
			CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
			X509Certificate issuerCertificate = (X509Certificate)certificateFactory.generateCertificate(issuer.asInputStream());
			if (x509crl.getIssuerX500Principal().equals(issuerCertificate.getSubjectX500Principal())) {
				return true;
			}
		} catch (CertificateException | IOException e) {
			throw new PipeRunException(this, "Could not read issuer certificate", e);
		} finally {
			CloseUtils.closeSilently(issuer);
		}
		return false;
	}

	/** Name of the sessionKey that holds the certificate of the issuer who signed the CRL. */
	public void setIssuerSessionKey(String issuerSessionKey) {
		this.issuerSessionKey = issuerSessionKey;
	}
}
