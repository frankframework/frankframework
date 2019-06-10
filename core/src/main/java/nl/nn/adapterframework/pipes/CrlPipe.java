/*
   Copyright 2016 Nationale-Nederlanden

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

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.doc.IbisDescription; 
import nl.nn.adapterframework.util.XmlBuilder;


/** 
 * @author Miel Hoppenbrouwers
 * @author Jaco de Groot
 * @author Tom van der Heijden
 */
@IbisDescription(
	"Pipe that reads a CRL from an input stream and transforms it to an XML. \n" + 
	"The steam is closed after reading. \n" + 
	"Example configuration: \n" + 
	"<code><pre> \n" + 
	"pipe \n" + 
	"name=\"Read issuer\" \n" + 
	"className=\"nl.nn.adapterframework.pipes.FilePipe\" \n" + 
	"actions=\"read\" \n" + 
	"fileName=\"dir/issuer.cer\" \n" + 
	"preserveInput=\"true\" \n" + 
	"outputType=\"stream\" \n" + 
	"storeResultInSessionKey=\"issuer\" \n" + 
	"> \n" + 
	"<forward name=\"success\" path=\"Read CRL\" /> \n" + 
	"/pipe> \n" + 
	"pipe \n" + 
	"name=\"Read CRL\" \n" + 
	"className=\"nl.nn.adapterframework.pipes.FilePipe\" \n" + 
	"actions=\"read\" \n" + 
	"fileName=\"dir/CRL.crl\" \n" + 
	"outputType=\"stream\" \n" + 
	"> \n" + 
	"<forward name=\"success\" path=\"Transform CRL\" /> \n" + 
	"/pipe> \n" + 
	"pipe \n" + 
	"name=\"Transform CRL\" \n" + 
	"className=\"nl.nn.adapterframework.pipes.CrlPipe\" \n" + 
	"issuerSessionKey=\"issuer\" \n" + 
	"> \n" + 
	"<forward name=\"success\" path=\"EXIT\" /> \n" + 
	"/pipe> \n" + 
	"</pre></code> \n" 
)
public class CrlPipe extends FixedForwardPipe {
	private String issuerSessionKey;

	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		X509CRL crl;
		InputStream inputStream = (InputStream)input;
		try {
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			crl = (X509CRL)cf.generateCRL(inputStream);
		} catch (CertificateException e) {
			throw new PipeRunException(this, "Could not read CRL", e);
		} catch (CRLException e) {
			throw new PipeRunException(this, "Could not read CRL", e);
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					log.warn("Could not close CRL input stream", e);
				}
			}
		}
		String result = null;
		if (isCRLOK(crl, (InputStream)session.get(getIssuerSessionKey()))) {
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
		return new PipeRunResult(getForward(), result);
	}

	private boolean isCRLOK(X509CRL x509crl, InputStream issuer) throws PipeRunException {
		try {
			CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
			X509Certificate issuerCertificate = (X509Certificate)certificateFactory.generateCertificate(issuer);
			if (x509crl.getIssuerX500Principal().equals(issuerCertificate.getSubjectX500Principal())) {
				return true;
			}
		} catch (CertificateException e) {
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

	@IbisDoc({"name of the sessionkey that holds a stream to the certificate of the issuer who signed the crl. the steam is closed after reading", ""})
	public void setIssuerSessionKey(String issuerSessionKey) {
		this.issuerSessionKey = issuerSessionKey;
	}
}
