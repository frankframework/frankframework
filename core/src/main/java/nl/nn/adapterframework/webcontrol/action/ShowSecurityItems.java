/*
   Copyright 2013, 2016, 2018 Nationale-Nederlanden

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
package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.IAdapter;
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
import nl.nn.adapterframework.jms.JmsRealm;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Shows the used certificate.
 * 
 * @author  Peter Leeuwenburgh
 * @since	4.8
 */

public final class ShowSecurityItems extends ActionBase {
	public static final String AUTHALIAS_XSLT = "xml/xsl/authAlias.xsl";

	private class JmsDestination {
		String connectionFactoryJndiName;
		String jndiName;

		JmsDestination(String connectionFactoryJndiName, String jndiName) {
			this.connectionFactoryJndiName = connectionFactoryJndiName;
			this.jndiName = jndiName;
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof JmsDestination) {
				JmsDestination other = (JmsDestination) o;
				if (connectionFactoryJndiName
						.equals(other.connectionFactoryJndiName)
						&& jndiName.equals(other.jndiName)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public int hashCode() {
			return (connectionFactoryJndiName + "|" + jndiName).hashCode();
		}
	}
	
	public ActionForward executeSub(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		// Initialize action
		initAction(request);
		if(ibisManager==null)return (mapping.findForward("noIbisContext"));

		XmlBuilder securityItems = new XmlBuilder("securityItems");
		addRegisteredAdapters(securityItems);
		addApplicationDeploymentDescriptor(securityItems);
		addSecurityRoleBindings(securityItems);

		String confResString;
		try {
			confResString = Misc.getConfigurationResources();
			if (confResString!=null) {
				confResString = XmlUtils.removeNamespaces(confResString);
			}
		} catch (IOException e) {
			log.warn("error getting configuration resources ["+e+"]");
			confResString = null;
		}

		addJmsRealms(securityItems, confResString);
		addProvidedJmsDestinations(securityItems, confResString);
		addSapSystems(securityItems);
		addAuthEntries(securityItems);
		addServerProps(securityItems);

		request.setAttribute("secItems", securityItems.toXML());

		// Forward control to the specified success URI
		log.debug("forward to success");
		return (mapping.findForward("success"));
	}

	private void addRegisteredAdapters(XmlBuilder securityItems) {
		XmlBuilder registeredAdapters = new XmlBuilder("registeredAdapters");
		securityItems.addSubElement(registeredAdapters);
		for (IAdapter iAdapter : ibisManager.getRegisteredAdapters()) {
			Adapter adapter = (Adapter)iAdapter;

			XmlBuilder adapterXML = new XmlBuilder("adapter");
			registeredAdapters.addSubElement(adapterXML);

			adapterXML.addAttribute("name", adapter.getName());

			Iterator recIt = adapter.getReceiverIterator();
			if (recIt.hasNext()) {
				XmlBuilder receiversXML = new XmlBuilder("receivers");
				while (recIt.hasNext()) {
					IReceiver receiver = (IReceiver) recIt.next();
					XmlBuilder receiverXML = new XmlBuilder("receiver");
					receiversXML.addSubElement(receiverXML);

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

	private void addApplicationDeploymentDescriptor(XmlBuilder securityItems) {
		XmlBuilder appDD = new XmlBuilder("applicationDeploymentDescriptor");
		String appDDString = null;
		try {
			appDDString = Misc.getApplicationDeploymentDescriptor();
			appDDString = XmlUtils.skipXmlDeclaration(appDDString);
			appDDString = XmlUtils.skipDocTypeDeclaration(appDDString);
			appDDString = XmlUtils.removeNamespaces(appDDString);
		} catch (IOException e) {
			appDDString = "*** ERROR ***";
		}
		appDD.setValue(appDDString, false);
		securityItems.addSubElement(appDD);
	}

	private void addSecurityRoleBindings(XmlBuilder securityItems) {
		XmlBuilder appBnd = new XmlBuilder("securityRoleBindings");
		String appBndString = null;
		try {
			appBndString = Misc.getDeployedApplicationBindings();
			appBndString = XmlUtils.removeNamespaces(appBndString);
		} catch (IOException e) {
			appBndString = "*** ERROR ***";
		}
		appBnd.setValue(appBndString, false);
		securityItems.addSubElement(appBnd);
	}

	private void addJmsRealms(XmlBuilder securityItems, String confResString) {
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

			DirectQuerySender qs = (DirectQuerySender)ibisManager.getIbisContext().createBeanAutowireByName(DirectQuerySender.class);
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
				XmlBuilder infoElem = new XmlBuilder("info");
				infoElem.setValue(dsInfo);
				jr.addSubElement(infoElem);
				if (confResString!=null) {
					String connectionPoolProperties;
					try {
						connectionPoolProperties = Misc.getConnectionPoolProperties(confResString, "JDBC", dsName);
					} catch (Exception e) {
						connectionPoolProperties = "*** ERROR ***";
					}
					if (StringUtils.isNotEmpty(connectionPoolProperties)) {
						infoElem = new XmlBuilder("info");
						infoElem.setValue(connectionPoolProperties);
						jr.addSubElement(infoElem);
					}
				}
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
				XmlBuilder infoElem = new XmlBuilder("info");
				infoElem.setValue(qcfInfo);
				jr.addSubElement(infoElem);
				if (confResString!=null) {
					String connectionPoolProperties;
					try {
						connectionPoolProperties = Misc.getConnectionPoolProperties(confResString, "JMS", qcfName);
					} catch (Exception e) {
						connectionPoolProperties = "*** ERROR ***";
					}
					if (StringUtils.isNotEmpty(connectionPoolProperties)) {
						infoElem = new XmlBuilder("info");
						infoElem.setValue(connectionPoolProperties);
						jr.addSubElement(infoElem);
					}
				}
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

	private void addProvidedJmsDestinations(XmlBuilder securityItems,
			String confResString) {
		XmlBuilder providedJmsDestinationsXml;
		if (confResString == null) {
			providedJmsDestinationsXml = new XmlBuilder(
					"providedJmsDestinations");
			providedJmsDestinationsXml.addAttribute("warn", "true");
			providedJmsDestinationsXml
					.setCdataValue("Resources file not found or empty");
		} else {
			List<JmsDestination> usedJmsDestinations;
			try {
				usedJmsDestinations = retrieveUsedJmsDestinations();
				providedJmsDestinationsXml = buildProvidedJmsDestinationsXml(
						confResString, usedJmsDestinations);
			} catch (Exception e) {
				providedJmsDestinationsXml = new XmlBuilder(
						"providedJmsDestinations");
				providedJmsDestinationsXml.addAttribute("error", "true");
				providedJmsDestinationsXml.setCdataValue(e.getMessage());
			}
		}
		securityItems.addSubElement(providedJmsDestinationsXml);
	}

	private XmlBuilder buildProvidedJmsDestinationsXml(String confResString,
			List<JmsDestination> usedJmsDestinations)
			throws DomBuilderException, IOException, TransformerException {
		XmlBuilder providedJmsDestinationsXml = new XmlBuilder(
				"providedJmsDestinations");

		String providedJmsDestinations = Misc.getJmsDestinations(confResString);
		Element providedJmsDestinationsElement = XmlUtils
				.buildElement(providedJmsDestinations);
		Collection providedConnectionFactories = XmlUtils.getChildTags(
				providedJmsDestinationsElement, "connectionFactory");
		Iterator providedConnectionFactoriesIterator = providedConnectionFactories
				.iterator();
		while (providedConnectionFactoriesIterator.hasNext()) {
			Element providedConnectionFactoryElement = (Element) providedConnectionFactoriesIterator
					.next();
			String providedConnectionFactoryJndiName = providedConnectionFactoryElement
					.getAttribute("jndiName");
			XmlBuilder providedConnectionFactoryXml = new XmlBuilder(
					"connectionFactory");
			providedConnectionFactoryXml.addAttribute("jndiName",
					providedConnectionFactoryJndiName);
			providedJmsDestinationsXml
					.addSubElement(providedConnectionFactoryXml);
			Collection providedDestinations = XmlUtils.getChildTags(
					providedConnectionFactoryElement, "destination");
			Iterator providedDestinationsIterator = providedDestinations
					.iterator();
			while (providedDestinationsIterator.hasNext()) {
				Element providedDestinationElement = (Element) providedDestinationsIterator
						.next();
				String providedValue = XmlUtils
						.getStringValue(providedDestinationElement);
				String providedJndiName = providedDestinationElement
						.getAttribute("jndiName");

				XmlBuilder providedDestinationXml = new XmlBuilder(
						"destination");
				providedConnectionFactoryXml
						.addSubElement(providedDestinationXml);
				providedDestinationXml.setValue(providedValue);
				providedDestinationXml.addAttribute("jndiName",
						providedJndiName);
				if (providedConnectionFactoryJndiName != null
						&& !usedJmsDestinations.isEmpty()
						&& usedJmsDestinations.contains(new JmsDestination(
								providedConnectionFactoryJndiName,
								providedJndiName))) {
					providedDestinationXml.addAttribute("used", "true");
				}
			}
		}
		return providedJmsDestinationsXml;
	}

	private List<JmsDestination> retrieveUsedJmsDestinations()
			throws DomBuilderException {
		List<JmsDestination> usedJmsDestinations = new ArrayList<JmsDestination>();
		StringBuilder config = new StringBuilder("<config>");
		for (Configuration configuration : ibisManager.getConfigurations()) {
			config.append(configuration.getLoadedConfiguration());
		}
		config.append("</config>");
		Document document = XmlUtils.buildDomDocument(config.toString());
		NodeList nodeList = document.getElementsByTagName("*");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				if (element.hasAttribute("destinationName")
						&& !"false".equalsIgnoreCase(
								element.getAttribute("lookupDestination"))) {
					JmsRealm jmsRealm = JmsRealmFactory.getInstance()
							.getJmsRealm(element.getAttribute("jmsRealm"));
					if (jmsRealm != null) {
						String connectionFactory = jmsRealm
								.retrieveConnectionFactoryName();
						usedJmsDestinations.add(new JmsDestination(
								connectionFactory,
								element.getAttribute("destinationName")));
					}
				}
			}
		}
		return usedJmsDestinations;
	}
	
	private void addSapSystems(XmlBuilder securityItems) {
		List sapSystems = null;
		Object sapSystemFactory = null;
		Method factoryGetSapSystemInfo = null;
		try {
			Class c = Class.forName("nl.nn.adapterframework.extensions.sap.SapSystemFactory");
			Method factoryGetInstance = c.getMethod("getInstance");
			sapSystemFactory = factoryGetInstance.invoke(null, null);
			Method factoryGetRegisteredSapSystemsNamesAsList = c.getMethod("getRegisteredSapSystemsNamesAsList");
			sapSystems = (List) factoryGetRegisteredSapSystemsNamesAsList.invoke(sapSystemFactory, null);
			factoryGetSapSystemInfo = c.getMethod("getSapSystemInfo", String.class);
		} catch (Throwable t) {
            log.debug("Caught NoClassDefFoundError, just no sapSystem available: " + t.getMessage());
		}
		
        if (sapSystems!=null) {
    		XmlBuilder sss = new XmlBuilder("sapSystems");
    		securityItems.addSubElement(sss);
    		Iterator iter = sapSystems.iterator();
    		while (iter.hasNext()) {
    			XmlBuilder ss = new XmlBuilder("sapSystem");
    			sss.addSubElement(ss);
    			String name = (String) iter.next();
   				ss.addAttribute("name", name);
   				XmlBuilder infoElem = new XmlBuilder("info");
   				try {
   					infoElem.setCdataValue((String) factoryGetSapSystemInfo.invoke(sapSystemFactory, name));
   				} catch (Exception e) {
   					infoElem.setValue("*** ERROR ***");
   				}
   				ss.addSubElement(infoElem);
    		}
        }
	}

	private void addAuthEntries(XmlBuilder securityItems) {
		XmlBuilder aes = new XmlBuilder("authEntries");
		securityItems.addSubElement(aes);
		List entries = new ArrayList();
		try {
			URL url = ClassUtils.getResourceURL(this, AUTHALIAS_XSLT);
			if (url != null) {
				for (Configuration configuration : ibisManager.getConfigurations()) {
					Transformer t = XmlUtils.createTransformer(url, true);
					String configString = configuration.getLoadedConfiguration();
					String authEntries = XmlUtils.transformXml(t, configString);
					log.debug("authentication aliases for configuration ["
							+ configuration.getName() + "] found ["
							+ authEntries.trim() + "]");
					Collection<String> c = XmlUtils.evaluateXPathNodeSet(authEntries, "authEntries/entry/@alias");
					if (c != null && c.size() > 0) {
						for (Iterator<String> cit = c.iterator(); cit.hasNext();) {
							String entry = cit.next();
							if (!entries.contains(entry)) {
								entries.add(entry);
							}
						}						
					}
				}
			}
		} catch (Exception e) {
			XmlBuilder ae = new XmlBuilder("entry");
			aes.addSubElement(ae);
			ae.addAttribute("alias", "*** ERROR ***");
		}

		if (entries != null) {
			Collections.sort(entries);
			Iterator iter = entries.iterator();
			while (iter.hasNext()) {
				String alias = (String) iter.next();
				CredentialFactory cf = new CredentialFactory(alias, null, null);
				XmlBuilder ae = new XmlBuilder("entry");
				aes.addSubElement(ae);
				ae.addAttribute("alias", alias);
				String userName;
				String passWord;
				try {
					userName = cf.getUsername();
					passWord = StringUtils.repeat("*", cf.getPassword().length());
	
				} catch (Exception e) {
					userName = "*** ERROR ***";
					passWord = "*** ERROR ***";
				}
				ae.addAttribute("userName", userName);
				ae.addAttribute("passWord", passWord);
			}
		}
	}

	private void addServerProps(XmlBuilder securityItems) {
		XmlBuilder serverProps = new XmlBuilder("serverProps");
			XmlBuilder transactionService = new XmlBuilder("transactionService");
			serverProps.addSubElement(transactionService);
			String totalTransactionLifetimeTimeout;
			try {
				totalTransactionLifetimeTimeout = Misc.getTotalTransactionLifetimeTimeout();
			} catch (Exception e) {
				totalTransactionLifetimeTimeout = "*** ERROR ***";
			}
			transactionService.addAttribute("totalTransactionLifetimeTimeout", totalTransactionLifetimeTimeout);
			String maximumTransactionTimeout;
			try {
				maximumTransactionTimeout = Misc.getMaximumTransactionTimeout();
			} catch (Exception e) {
				maximumTransactionTimeout = "*** ERROR ***";
			}
			transactionService.addAttribute("maximumTransactionTimeout", maximumTransactionTimeout);
		securityItems.addSubElement(serverProps);
	}
}
