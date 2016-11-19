/*
Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.webcontrol.api;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.transform.Transformer;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
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
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StringResolver;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

/**
* Shows the configuration (with resolved variables).
* 
* @author	Niels Meijer
*/

@Path("/")
public final class ShowSecurityItems extends Base {
	public static final String AUTHALIAS_XSLT = "xml/xsl/authAlias.xsl";
	public static final String GETCONNPOOLPROP_XSLT = "xml/xsl/getConnectionPoolProperties.xsl";

	@GET
	@RolesAllowed({"ObserverAccess", "IbisTester"})
	@Path("/securityitems")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSecurityItems(@Context ServletConfig servletConfig) throws ApiException {
		initBase(servletConfig);
		
		if (ibisManager == null) {
			throw new ApiException("Config not found!");
		}
		
		Map<String, Object> securityItems = new HashMap<String, Object>();
		
		addRegisteredAdapters(securityItems);
		addApplicationDeploymentDescriptor(securityItems);
		addSecurityRoleBindings(securityItems);
		addJmsRealms(securityItems);
		addSapSystems(securityItems);
		addAuthEntries(securityItems);
		addServerProps(securityItems);
		
		return Response.status(Response.Status.CREATED).entity(securityItems).build();
	}

	private void addRegisteredAdapters(Map<String, Object> securityItems) {
		for (IAdapter iAdapter : ibisManager.getRegisteredAdapters()) {
			Adapter adapter = (Adapter)iAdapter;

			Map<String, Object> adapterMap = new HashMap<String, Object>();

			adapterMap.put("name", adapter.getName());

			Iterator recIt = adapter.getReceiverIterator();
			if (recIt.hasNext()) {
				ArrayList<Object> receivers = new ArrayList<Object>();
				while (recIt.hasNext()) {
					IReceiver receiver = (IReceiver) recIt.next();
					Map<String, Object> receiverMap = new HashMap<String, Object>(2);

					receiverMap.put("name", receiver.getName());

					if (receiver instanceof HasSender) {
						ISender sender = ((HasSender) receiver).getSender();
						if (sender != null) {
							receiverMap.put("senderName", sender.getName());
						}
					}
					receivers.add(receiverMap);
				}
				securityItems.put("receivers", receivers);
			}

			// make list of pipes to be displayed in configuration status
			ArrayList<Object> pipes = new ArrayList<Object>();
			PipeLine pipeline = adapter.getPipeLine();
			for (int i = 0; i < pipeline.getPipes().size(); i++) {
				IPipe pipe = pipeline.getPipe(i);
				String pipename = pipe.getName();
				if (pipe instanceof MessageSendingPipe) {
					MessageSendingPipe msp = (MessageSendingPipe) pipe;
					Map<String, Object> pipeMap = new HashMap<String, Object>(2);
					pipeMap.put("name", pipename);
					ISender sender = msp.getSender();
					pipeMap.put("sender", ClassUtils.nameOf(sender));
					if (sender instanceof WebServiceSender) {
						WebServiceSender s = (WebServiceSender) sender;
						String certificate = s.getCertificate();
						if (StringUtils.isNotEmpty(certificate)) {
							Map<String, Object> certElem = new HashMap<String, Object>(4);
							certElem.put("name", certificate);
							String certificateAuthAlias = s.getCertificateAuthAlias();
							certElem.put("authAlias", certificateAuthAlias);
							URL certificateUrl = ClassUtils.getResourceURL(this, certificate);
							if (certificateUrl == null) {
								certElem.put("url", "");
								certElem.put("info", "*** ERROR ***");
							} else {
								certElem.put("url", certificateUrl.toString());
								String certificatePassword = s.getCertificatePassword();
								CredentialFactory certificateCf = new CredentialFactory(certificateAuthAlias, null, certificatePassword);
								String keystoreType = s.getKeystoreType();
								certElem.put("info", addCertificateInfo(certificateUrl, certificateCf.getPassword(), keystoreType, "Certificate chain"));
							}
							pipeMap.put("WebServiceSender", certElem);
						}
					} else {
						if (sender instanceof HttpSender) {
							HttpSender s = (HttpSender) sender;
							String certificate = s.getCertificate();
							if (StringUtils.isNotEmpty(certificate)) {
								Map<String, Object> certElem = new HashMap<String, Object>(4);
								certElem.put("name", certificate);
								String certificateAuthAlias = s.getCertificateAuthAlias();
								certElem.put("authAlias", certificateAuthAlias);
								URL certificateUrl = ClassUtils.getResourceURL(this, certificate);
								if (certificateUrl == null) {
									certElem.put("url", "");
									certElem.put("info", "*** ERROR ***");
								} else {
									certElem.put("url", certificateUrl.toString());
									String certificatePassword = s.getCertificatePassword();
									CredentialFactory certificateCf = new CredentialFactory(certificateAuthAlias, null, certificatePassword);
									String keystoreType = s.getKeystoreType();
									certElem.put("info", addCertificateInfo(certificateUrl, certificateCf.getPassword(), keystoreType, "Certificate chain"));
								}
								pipeMap.put("HttpSender", certElem);
							}
						} else {
							if (sender instanceof FtpSender) {
								FtpSender s = (FtpSender) sender;
								String certificate = s.getCertificate();
								if (StringUtils.isNotEmpty(certificate)) {
									Map<String, Object> certElem = new HashMap<String, Object>(4);
									certElem.put("name", certificate);
									String certificateAuthAlias = s.getCertificateAuthAlias();
									certElem.put("authAlias", certificateAuthAlias);
									URL certificateUrl = ClassUtils.getResourceURL(this, certificate);
									if (certificateUrl == null) {
										certElem.put("url", "");
										certElem.put("info", "*** ERROR ***");
									} else {
										certElem.put("url", certificateUrl.toString());
										String certificatePassword = s.getCertificatePassword();
										CredentialFactory certificateCf = new CredentialFactory(certificateAuthAlias, null, certificatePassword);
										String keystoreType = s.getCertificateType();
										certElem.put("info", addCertificateInfo(certificateUrl, certificateCf.getPassword(), keystoreType, "Certificate chain"));
									}
									pipeMap.put("FtpSender", certElem);
								}
							}
						}
					}
					pipes.add(pipeMap);
				}
			}
			securityItems.put("pipes", pipes);
		}
	}

	private ArrayList<Object> addCertificateInfo(final URL url, final String password, String keyStoreType, String prefix) {
		ArrayList<Object> certificateList = new ArrayList<Object>();
		try {
			KeyStore keystore = KeyStore.getInstance(keyStoreType);
			keystore.load(url.openStream(), password != null ? password.toCharArray() : null);
			if (log.isInfoEnabled()) {
				Enumeration aliases = keystore.aliases();
				while (aliases.hasMoreElements()) {
					String alias = (String) aliases.nextElement();
					ArrayList<Object> infoElem = new ArrayList<Object>();
					infoElem.add(prefix + " '" + alias + "':");
					Certificate trustedcert = keystore.getCertificate(alias);
					if (trustedcert != null && trustedcert instanceof X509Certificate) {
						X509Certificate cert = (X509Certificate) trustedcert;
						infoElem.add("Subject DN: " + cert.getSubjectDN());
						infoElem.add("Signature Algorithm: " + cert.getSigAlgName());
						infoElem.add("Valid from: " + cert.getNotBefore());
						infoElem.add("Valid until: " + cert.getNotAfter());
						infoElem.add("Issuer: " + cert.getIssuerDN());
					}
					certificateList.add(infoElem);
				}
			}
		} catch (Exception e) {
			certificateList.add("*** ERROR ***");
		}
		return certificateList;
	}

	private void addApplicationDeploymentDescriptor(Map<String, Object> securityItems) {
		String appDDString = null;
		try {
			appDDString = Misc.getApplicationDeploymentDescriptor();
			appDDString = XmlUtils.skipXmlDeclaration(appDDString);
			appDDString = XmlUtils.skipDocTypeDeclaration(appDDString);
			appDDString = XmlUtils.removeNamespaces(appDDString);
		} catch (IOException e) {
			appDDString = "*** ERROR ***";
		}
		securityItems.put("applicationDeploymentDescriptor", appDDString);
	}

	private void addSecurityRoleBindings(Map<String, Object> securityItems) {
		String appBndString = null;
		try {
			appBndString = Misc.getDeployedApplicationBindings();
			appBndString = XmlUtils.removeNamespaces(appBndString);
		} catch (IOException e) {
			appBndString = "*** ERROR ***";
		}
		securityItems.put("securityRoleBindings", appBndString);
	}

	private void addJmsRealms(Map<String, Object> securityItems) {
		List jmsRealms = JmsRealmFactory.getInstance().getRegisteredRealmNamesAsList();
		ArrayList<Object> jmsRealmList = new ArrayList<Object>();
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

		for (int j = 0; j < jmsRealms.size(); j++) {
			Map<String, Object> realm = new HashMap<String, Object>();
			String jmsRealm = (String) jmsRealms.get(j);

			String dsName = null;
			String qcfName = null;
			String tcfName = null;
			String dsInfo = null;
			String qcfInfo = null;

			DirectQuerySender qs = (DirectQuerySender) ibisManager.getIbisContext().createBeanAutowireByName(DirectQuerySender.class);
			qs.setJmsRealm(jmsRealm);
			try {
				dsName = qs.getDataSourceNameToUse();
				dsInfo = qs.getDatasourceInfo();
			} catch (JdbcException jdbce) {
				// no datasource
			}
			if (StringUtils.isNotEmpty(dsName)) {
				realm.put("name", jmsRealm);
				realm.put("datasourceName", dsName);
				realm.put("info", dsInfo);

				if (confResString!=null) {
					String connectionPoolProperties = getConnectionPoolProperties(confResString, "JDBC", dsName);
					if (StringUtils.isNotEmpty(connectionPoolProperties)) {
						realm.put("connectionPoolProperties", connectionPoolProperties);
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
				realm.put("name", jmsRealm);
				realm.put("queueConnectionFactoryName", qcfName);
				realm.put("info", qcfInfo);

				if (confResString!=null) {
					String connectionPoolProperties = getConnectionPoolProperties(confResString, "JMS", qcfName);
					if (StringUtils.isNotEmpty(connectionPoolProperties)) {
						realm.put("connectionPoolProperties", connectionPoolProperties);
					}
				}
			}
			tcfName = js.getTopicConnectionFactoryName();
			if (StringUtils.isNotEmpty(tcfName)) {
				realm.put("name", jmsRealm);
				realm.put("topicConnectionFactoryName", tcfName);
			}
			jmsRealmList.add(realm);
		}
		
		securityItems.put("jmsRealms", jmsRealmList);
	}

	private String getConnectionPoolProperties(String confResString, String providerType, String jndiName) {
		String connectionPoolProperties = null;
		try {
			URL url = ClassUtils.getResourceURL(this, GETCONNPOOLPROP_XSLT);
			if (url != null) {
				Transformer t = XmlUtils.createTransformer(url, true);
				Map<String, String> parameters = new Hashtable();
				parameters.put("providerType", providerType);
				parameters.put("jndiName", jndiName);
				XmlUtils.setTransformerParameters(t, parameters);
				connectionPoolProperties = XmlUtils.transformXml(t, confResString);
			}
		} catch (Exception e) {
			connectionPoolProperties = "*** ERROR ***";
		}
		return connectionPoolProperties;
	}

	private void addSapSystems(Map<String, Object> securityItems) {
		ArrayList<Object> sapSystemList = new ArrayList<Object>();
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
    		Iterator iter = sapSystems.iterator();
    		while (iter.hasNext()) {
    			Map<String, Object> ss = new HashMap<String, Object>();
    			String name = (String) iter.next();
   				ss.put("name", name);
   				try {
   	   				ss.put("info", (String) factoryGetSapSystemInfo.invoke(sapSystemFactory, name));
   				} catch (Exception e) {
   	   				ss.put("info", "*** ERROR ***");
   				}
   				sapSystemList.add(ss);
    		}
        }
		securityItems.put("sapSystems", sapSystemList);
	}

	private void addAuthEntries(Map<String, Object> securityItems) {
		ArrayList<Object> authEntries = new ArrayList<Object>();
		Collection entries = null;
		try {
			URL url = ClassUtils.getResourceURL(this, AUTHALIAS_XSLT);
			if (url != null) {
				for (Configuration configuration : ibisManager.getConfigurations()) {
					Transformer t = XmlUtils.createTransformer(url, true);
					String configString = configuration.getOriginalConfiguration();
					configString = StringResolver.substVars(configString, AppConstants.getInstance());
					configString = ConfigurationUtils.getActivatedConfiguration(configuration, configString);
					String ae = XmlUtils.transformXml(t, configString);
					Element authEntriesElement = XmlUtils.buildElement(ae);
					if (entries == null) {
						entries = XmlUtils.getChildTags(authEntriesElement, "entry");
					} else {
						entries.addAll(XmlUtils.getChildTags(authEntriesElement, "entry"));
					}
				}
			}
		} catch (Exception e) {
			authEntries.add("*** ERROR ***");
		}

		if (entries != null) {
			
			Iterator iter = entries.iterator();
			while (iter.hasNext()) {
				Map<String, Object> ae = new HashMap<String, Object>();
				Element itemElement = (Element) iter.next();
				String alias = itemElement.getAttribute("alias");
				ae.put("alias", alias);
				CredentialFactory cf = new CredentialFactory(alias, null, null);

				String userName;
				String passWord;
				try {
					userName = cf.getUsername();
					passWord = StringUtils.repeat("*", cf.getPassword().length());
	
				} catch (Exception e) {
					userName = "*** ERROR ***";
					passWord = "*** ERROR ***";
				}
				ae.put("userName", userName);
				ae.put("passWord", passWord);
				authEntries.add(ae);
			}
		}
		securityItems.put("authEntries", authEntries);
	}

	private void addServerProps(Map<String, Object> securityItems) {
		Map<String, Object> serverProps = new HashMap<String, Object>(2);
		
		String totalTransactionLifetimeTimeout;
		try {
			totalTransactionLifetimeTimeout = Misc.getTotalTransactionLifetimeTimeout();
		} catch (Exception e) {
			totalTransactionLifetimeTimeout = "*** ERROR ***";
		}
		serverProps.put("totalTransactionLifetimeTimeout", totalTransactionLifetimeTimeout);
		String maximumTransactionTimeout;
		try {
			maximumTransactionTimeout = Misc.getMaximumTransactionTimeout();
		} catch (Exception e) {
			maximumTransactionTimeout = "*** ERROR ***";
		}
		serverProps.put("maximumTransactionTimeout", maximumTransactionTimeout);
		securityItems.put("serverProps", serverProps);
	}
}
