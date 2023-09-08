/*
   Copyright 2016, 2020 Nationale-Nederlanden

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
package nl.nn.adapterframework.pipes;


import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.util.Iterator;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.ElementType;
import nl.nn.adapterframework.doc.ElementType.ElementTypes;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Pipe that reads a CRL from an input stream and transforms it to an XML.
 * The steam is closed after reading.
 *
 * Example configuration:
 * <pre><code>
	&lt;pipe
		name="Read issuer"
		className="nl.nn.adapterframework.pipes.FilePipe"
		actions="read"
		fileName="dir/issuer.cer"
		preserveInput="true"
		outputType="stream"
		storeResultInSessionKey="issuer"
		&gt;
		&lt;forward name="success" path="Read CRL" /&gt;
	&lt;/pipe&gt;
	&lt;pipe
		name="Read CRL"
		className="nl.nn.adapterframework.pipes.FilePipe"
		actions="read"
		fileName="dir/CRL.crl"
		outputType="stream"
		&gt;
		&lt;forward name="success" path="Transform CRL" /&gt;
	&lt;/pipe&gt;
	&lt;pipe
		name="Transform CRL"
		className="nl.nn.adapterframework.pipes.CrlPipe"
		issuerSessionKey="issuer"
		&gt;
		&lt;forward name="success" path="EXIT" /&gt;
	&lt;/pipe&gt;
 * </code></pre>
 *
 *
 * @author Miel Hoppenbrouwers
 * @author Jaco de Groot
 * @author Tom van der Heijden
 */
@ElementType(ElementTypes.TRANSLATOR)
public class CrlPipe extends FixedForwardPipe {
	private String issuerSessionKey;

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		X509CRL crl;
		try (InputStream inputStream = message.asInputStream()) {
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			crl = (X509CRL)cf.generateCRL(inputStream);
		} catch (CertificateException | IOException | CRLException e) {
			throw new PipeRunException(this, "Could not read CRL", e);
		}
		String result = null;
		if (isCRLOK(crl, Message.asMessage(session.get(getIssuerSessionKey())))) {
			XmlBuilder root = new XmlBuilder("SerialNumbers");
			Iterator <? extends X509CRLEntry> it = crl.getRevokedCertificates().iterator();
			while (it.hasNext()) {
				X509CRLEntry e = (X509CRLEntry) it.next();
				XmlBuilder serialNumber = new XmlBuilder("SerialNumber");
				serialNumber.setValue(e.getSerialNumber().toString(16));
				root.addSubElement(serialNumber);
			}
			result = root.toXML();
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
			if (issuer != null) {
				try {
					issuer.close();
				} catch (IOException e) {
					log.warn("Could not close issuer input stream", e);
				}
			}
		}
		return false;
	}

	public String getIssuerSessionKey() {
		return issuerSessionKey;
	}

	/** name of the sessionkey that holds a stream to the certificate of the issuer who signed the crl. the steam is closed after reading */
	public void setIssuerSessionKey(String issuerSessionKey) {
		this.issuerSessionKey = issuerSessionKey;
	}
}
