/*
 * $Log: ShowSecurityItems.java,v $
 * Revision 1.4  2010-02-03 11:25:01  m168309
 * removed a debug logging
 *
 * Revision 1.3  2010/02/03 11:15:25  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added information about JmsRealms
 *
 * Revision 1.2  2008/12/15 12:23:35  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * improved Security Role Bindings
 *
 * Revision 1.1  2008/11/25 10:14:45  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * ShowUsedCertificates renamed to ShowSecurityItems
 *
 * Revision 1.2  2008/10/31 10:56:34  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Error handling  when certificateUrl is null
 *
 * Revision 1.1  2007/12/28 12:17:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.jms.ConnectionFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.ftp.FtpSender;
import nl.nn.adapterframework.http.HttpSender;
import nl.nn.adapterframework.http.WebServiceSender;
import nl.nn.adapterframework.jdbc.DirectQuerySender;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jms.JmsException;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Shows the used certificate.
 * 
 * @author  Peter Leeuwenburgh
 * @since	4.8
 * @version Id 
 */

public final class ShowSecurityItems extends ActionBase {
	public static final String version = "$RCSfile: ShowSecurityItems.java,v $ $Revision: 1.4 $ $Date: 2010-02-03 11:25:01 $";

	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		// Initialize action
		initAction(request);

		if (null == config) {
			return (mapping.findForward("noconfig"));
		}

		XmlBuilder securityItems = new XmlBuilder("securityItems");
		addRegisteredAdapters(securityItems);
		//[PL]String appName = Misc.getDeployedApplicationName();
		String appName = "Ibis4TestIAF-035_20100202-1549";
		addApplicationDeploymentDescriptor(securityItems, appName);
		addSecurityRoleBindings(securityItems, appName);
		addJmsRealms(securityItems);

		request.setAttribute("secItems", securityItems.toXML());

		// Forward control to the specified success URI
		log.debug("forward to success");
		return (mapping.findForward("success"));
	}

	private void addRegisteredAdapters(XmlBuilder securityItems) {
		XmlBuilder registeredAdapters = new XmlBuilder("registeredAdapters");
		securityItems.addSubElement(registeredAdapters);
		for (int j = 0; j < config.getRegisteredAdapters().size(); j++) {
			Adapter adapter = (Adapter) config.getRegisteredAdapter(j);

			XmlBuilder adapterXML = new XmlBuilder("adapter");
			registeredAdapters.addSubElement(adapterXML);

			RunStateEnum adapterRunState = adapter.getRunState();

			adapterXML.addAttribute("name", adapter.getName());

			Iterator recIt = adapter.getReceiverIterator();
			if (recIt.hasNext()) {
				XmlBuilder receiversXML = new XmlBuilder("receivers");
				while (recIt.hasNext()) {
					IReceiver receiver = (IReceiver) recIt.next();
					XmlBuilder receiverXML = new XmlBuilder("receiver");
					receiversXML.addSubElement(receiverXML);

					RunStateEnum receiverRunState = receiver.getRunState();

					receiverXML.addAttribute("name", receiver.getName());

					if (receiver instanceof HasSender) {
						ISender sender = ((HasSender) receiver).getSender();
						if (sender != null) {
							receiverXML.addAttribute("senderName", sender.getName());
						}
					}
				}
				adapterXML.addSubElement(receiversXML);
			}

			// make list of pipes to be displayed in configuration status
			XmlBuilder pipesElem = new XmlBuilder("pipes");
			adapterXML.addSubElement(pipesElem);
			PipeLine pipeline = adapter.getPipeLine();
			for (int i = 0; i < pipeline.getPipes().size(); i++) {
				IPipe pipe = pipeline.getPipe(i);
				String pipename = pipe.getName();
				if (pipe instanceof MessageSendingPipe) {
					MessageSendingPipe msp = (MessageSendingPipe) pipe;
					XmlBuilder pipeElem = new XmlBuilder("pipe");
					pipeElem.addAttribute("name", pipename);
					ISender sender = msp.getSender();
					pipeElem.addAttribute("sender", ClassUtils.nameOf(sender));
					pipesElem.addSubElement(pipeElem);
					if (sender instanceof WebServiceSender) {
						WebServiceSender s = (WebServiceSender) sender;
						String certificate = s.getCertificate();
						if (StringUtils.isNotEmpty(certificate)) {
							XmlBuilder certElem = new XmlBuilder("certificate");
							certElem.addAttribute("name", certificate);
							String certificateAuthAlias = s.getCertificateAuthAlias();
							certElem.addAttribute("authAlias", certificateAuthAlias);
							URL certificateUrl = ClassUtils.getResourceURL(this, certificate);
							if (certificateUrl == null) {
								certElem.addAttribute("url", "");
								pipeElem.addSubElement(certElem);
								XmlBuilder infoElem = new XmlBuilder("info");
								infoElem.setCdataValue("*** ERROR ***");
								certElem.addSubElement(infoElem);
							} else {
								certElem.addAttribute("url", certificateUrl.toString());
								pipeElem.addSubElement(certElem);
								String certificatePassword = s.getCertificatePassword();
								CredentialFactory certificateCf = new CredentialFactory(certificateAuthAlias, null, certificatePassword);
								String keystoreType = s.getKeystoreType();
								addCertificateInfo(certElem, certificateUrl, certificateCf.getPassword(), keystoreType, "Certificate chain");
							}
						}
					} else {
						if (sender instanceof HttpSender) {
							HttpSender s = (HttpSender) sender;
							String certificate = s.getCertificate();
							if (StringUtils.isNotEmpty(certificate)) {
								XmlBuilder certElem = new XmlBuilder("certificate");
								certElem.addAttribute("name", certificate);
								String certificateAuthAlias = s.getCertificateAuthAlias();
								certElem.addAttribute("authAlias", certificateAuthAlias);
								URL certificateUrl = ClassUtils.getResourceURL(this, certificate);
								if (certificateUrl == null) {
									certElem.addAttribute("url", "");
									pipeElem.addSubElement(certElem);
									XmlBuilder infoElem = new XmlBuilder("info");
									infoElem.setCdataValue("*** ERROR ***");
									certElem.addSubElement(infoElem);
								} else {
									certElem.addAttribute("url", certificateUrl.toString());
									pipeElem.addSubElement(certElem);
									String certificatePassword = s.getCertificatePassword();
									CredentialFactory certificateCf = new CredentialFactory(certificateAuthAlias, null, certificatePassword);
									String keystoreType = s.getKeystoreType();
									addCertificateInfo(certElem, certificateUrl, certificateCf.getPassword(), keystoreType, "Certificate chain");
								}
							}
						} else {
							if (sender instanceof FtpSender) {
								FtpSender s = (FtpSender) sender;
								String certificate = s.getCertificate();
								if (StringUtils.isNotEmpty(certificate)) {
									XmlBuilder certElem = new XmlBuilder("certificate");
									certElem.addAttribute("name", certificate);
									String certificateAuthAlias = s.getCertificateAuthAlias();
									certElem.addAttribute("authAlias", certificateAuthAlias);
									URL certificateUrl = ClassUtils.getResourceURL(this, certificate);
									if (certificateUrl == null) {
										certElem.addAttribute("url", "");
										pipeElem.addSubElement(certElem);
										XmlBuilder infoElem = new XmlBuilder("info");
										infoElem.setCdataValue("*** ERROR ***");
										certElem.addSubElement(infoElem);
									} else {
										certElem.addAttribute("url", certificateUrl.toString());
										pipeElem.addSubElement(certElem);
										String certificatePassword = s.getCertificatePassword();
										CredentialFactory certificateCf = new CredentialFactory(certificateAuthAlias, null, certificatePassword);
										String keystoreType = s.getCertificateType();
										addCertificateInfo(certElem, certificateUrl, certificateCf.getPassword(), keystoreType, "Certificate chain");
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private void addCertificateInfo(XmlBuilder certElem, final URL url, final String password, String keyStoreType, String prefix) {
		try {
			KeyStore keystore = KeyStore.getInstance(keyStoreType);
			keystore.load(url.openStream(), password != null ? password.toCharArray() : null);
			if (log.isInfoEnabled()) {
				Enumeration aliases = keystore.aliases();
				while (aliases.hasMoreElements()) {
					String alias = (String) aliases.nextElement();
					XmlBuilder infoElem = new XmlBuilder("info");
					infoElem.setCdataValue(prefix + " '" + alias + "':");
					certElem.addSubElement(infoElem);
					Certificate trustedcert = keystore.getCertificate(alias);
					if (trustedcert != null && trustedcert instanceof X509Certificate) {
						X509Certificate cert = (X509Certificate) trustedcert;
						infoElem = new XmlBuilder("info");
						infoElem.setCdataValue("  Subject DN: " + cert.getSubjectDN());
						certElem.addSubElement(infoElem);
						infoElem = new XmlBuilder("info");
						infoElem.setCdataValue("  Signature Algorithm: " + cert.getSigAlgName());
						certElem.addSubElement(infoElem);
						infoElem = new XmlBuilder("info");
						infoElem.setCdataValue("  Valid from: " + cert.getNotBefore());
						certElem.addSubElement(infoElem);
						infoElem = new XmlBuilder("info");
						infoElem.setCdataValue("  Valid until: " + cert.getNotAfter());
						certElem.addSubElement(infoElem);
						infoElem = new XmlBuilder("info");
						infoElem.setCdataValue("  Issuer: " + cert.getIssuerDN());
						certElem.addSubElement(infoElem);
					}
				}
			}
		} catch (Exception e) {
			XmlBuilder infoElem = new XmlBuilder("info");
			infoElem.setCdataValue("*** ERROR ***");
			certElem.addSubElement(infoElem);
		}
	}

	private void addApplicationDeploymentDescriptor(XmlBuilder securityItems, String appName) {
		if (appName != null) {
			XmlBuilder appDD = new XmlBuilder("applicationDeploymentDescriptor");
			appDD.addAttribute("appName", appName);
			String appDDString = null;
			try {
				appDDString = Misc.getApplicationDeploymentDescriptor(appName);
				appDDString = XmlUtils.skipXmlDeclaration(appDDString);
				appDDString = XmlUtils.skipDocTypeDeclaration(appDDString);
			} catch (IOException e) {
				appDDString = "*** ERROR ***";
			}
			appDD.setValue(appDDString, false);
			securityItems.addSubElement(appDD);
		}
	}

	private void addSecurityRoleBindings(XmlBuilder securityItems, String appName) {
		if (appName != null) {
			XmlBuilder appBnd = new XmlBuilder("securityRoleBindings");
			appBnd.addAttribute("appName", appName);
			String appBndString = null;
			try {
				appBndString = Misc.getDeployedApplicationBindings(appName);
				appBndString = XmlUtils.removeNamespaces(appBndString);
			} catch (IOException e) {
				appBndString = "*** ERROR ***";
			}
			appBnd.setValue(appBndString, false);
			securityItems.addSubElement(appBnd);
		}
	}

	private void addJmsRealms(XmlBuilder securityItems) {
		List jmsRealms = JmsRealmFactory.getInstance().getRegisteredRealmNamesAsList();
		XmlBuilder jrs = new XmlBuilder("jmsRealms");
		securityItems.addSubElement(jrs);
		for (int j = 0; j < jmsRealms.size(); j++) {
			String jmsRealm = (String) jmsRealms.get(j);

			String dsName = null;
			String qcfName = null;
			String tcfName = null;
			String dsInfo = null;
			String qcfInfo = null;

			DirectQuerySender qs = new DirectQuerySender();
			qs.setJmsRealm(jmsRealm);
			try {
				dsName = qs.getDataSourceNameToUse();
				dsInfo = qs.getDatasourceInfo();
			} catch (JdbcException jdbce) {
				// no datasource
			}
			if (StringUtils.isNotEmpty(dsName)) {
				XmlBuilder jr = new XmlBuilder("jmsRealm");
				jrs.addSubElement(jr);
				jr.addAttribute("name", jmsRealm);
				jr.addAttribute("datasourceName", dsName);
				jr.addAttribute("info", dsInfo);
			}

			JmsSender js = new JmsSender();
			js.setJmsRealm(jmsRealm);
			try {
				qcfName = js.getConnectionFactoryName();
				qcfInfo = js.getConnectionFactoryInfo();
			} catch (JmsException jmse) {
				// no connectionFactory
			}
			if (StringUtils.isNotEmpty(qcfName)) {
				XmlBuilder jr = new XmlBuilder("jmsRealm");
				jrs.addSubElement(jr);
				jr.addAttribute("name", jmsRealm);
				jr.addAttribute("queueConnectionFactoryName", qcfName);
				jr.addAttribute("info", qcfInfo);
			}
			tcfName = js.getTopicConnectionFactoryName();
			if (StringUtils.isNotEmpty(tcfName)) {
				XmlBuilder jr = new XmlBuilder("jmsRealm");
				jrs.addSubElement(jr);
				jr.addAttribute("name", jmsRealm);
				jr.addAttribute("topicConnectionFactoryName", tcfName);
			}
		}
	}
}
