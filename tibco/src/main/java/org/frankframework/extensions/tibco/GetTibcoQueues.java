/*
   Copyright 2013-2016, 2020 Nationale-Nederlanden, 2021, 2024-2025 WeAreFrank!

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
package org.frankframework.extensions.tibco;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import jakarta.jms.QueueBrowser;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

import org.apache.commons.lang3.StringUtils;

import com.tibco.tibjms.admin.ACLEntry;
import com.tibco.tibjms.admin.BridgeTarget;
import com.tibco.tibjms.admin.ConnectionInfo;
import com.tibco.tibjms.admin.ConsumerInfo;
import com.tibco.tibjms.admin.QueueInfo;
import com.tibco.tibjms.admin.ServerInfo;
import com.tibco.tibjms.admin.TibjmsAdmin;
import com.tibco.tibjms.admin.TibjmsAdminException;
import com.tibco.tibjms.admin.UserInfo;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.ldap.LdapSender;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.pipes.TimeoutGuardPipe;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.Misc;
import org.frankframework.util.TimeProvider;
import org.frankframework.util.XmlBuilder;
import org.frankframework.util.XmlUtils;

/**
 * Returns information about Tibco queues in a XML string.
 * <p>
 * If the parameter <code>queueName</code> is empty then
 * <ul><li>all Tibco queues including information about these queues are returned</li></ul>
 * else
 * <ul><li>one message on a specific Tibco queue including information about this message is returned (without removing it)</li></ul>
 * </p>
 *
 * @ff.parameter url When a parameter with name url is present, it is used instead of the url specified by the attribute
 * @ff.parameter authAlias When a parameter with name authAlias is present, it is used instead of the authAlias specified by the attribute
 * @ff.parameter username When a parameter with name userName is present, it is used instead of the userName specified by the attribute
 * @ff.parameter password When a parameter with name password is present, it is used instead of the password specified by the attribute
 * @ff.parameter queueName The name of the queue which is used for browsing one queue
 * @ff.parameter queueItem The number of the queue message which is used for browsing one queue (default is 1)
 * @ff.parameter showAge When set to <code>true</code> and <code>pendingMsgCount&gt;0</code> and <code>receiverCount=0</code>, the age of the current first message in the queue is shown in the queues overview (default is false)
 * @ff.parameter countOnly When set to <code>true</code> and <code>queueName</code> is filled, only the number of pending messages is returned (default is false)
 * @ff.parameter ldapUrl When present, principal descriptions are retrieved from this LDAP server
 *
 * @author Peter Leeuwenburgh
 */

public class GetTibcoQueues extends TimeoutGuardPipe {
	private String url;
	private String authAlias;
	private String username;
	private String password;
	private boolean skipTemporaryQueues = false;
	private boolean hideMessage = false;
	private String queueRegex;
	private String emsPropertiesFile;
	private Map<String, Object> emsProperties;

	@Override
	public void configure() throws ConfigurationException {
		if (getParameterList().hasParameter("userName")) {
			ConfigurationWarnings.add(this, log, "parameter [userName] has been replaced with [username]");
		}

		if(StringUtils.isNotEmpty(emsPropertiesFile)) {
			try {
				emsProperties = new TibcoEmsProperties(this, emsPropertiesFile);
			} catch (IOException e) {
				throw new ConfigurationException("unable to find/load the EMS properties file", e);
			}
		} else {
			emsProperties = Collections.emptyMap();
		}

		super.configure();
	}

	@Override
	public PipeRunResult doPipeWithTimeoutGuarded(Message input, PipeLineSession session) throws PipeRunException {
		String result;
		String urlWork;
		String authAliasWork;
		String userNameWork;
		String passwordWork;
		String queueNameWork = null;

		ParameterValueList pvl;
		try {
			pvl = getParameterList().getValues(input, session);
		} catch (ParameterException e) {
			throw new PipeRunException(this, "exception on extracting parameters", e);
		}

		urlWork = getParameterValue(pvl, "url");
		if (urlWork == null) {
			urlWork = getUrl();
		}
		authAliasWork = getParameterValue(pvl, "authAlias");
		if (authAliasWork == null) {
			authAliasWork = getAuthAlias();
		}
		userNameWork = pvl.contains("username") ? getParameterValue(pvl, "username") : getParameterValue(pvl, "userName");
		if (userNameWork == null) {
			userNameWork = getUsername();
		}
		passwordWork = getParameterValue(pvl, "password");
		if (passwordWork == null) {
			passwordWork = getPassword();
		}

		CredentialFactory cf = new CredentialFactory(authAliasWork, userNameWork, passwordWork);

		TibjmsAdmin admin = null;
		try {
			admin = TibcoUtils.getActiveServerAdmin(urlWork, cf, emsProperties);
			if (admin == null) {
				throw new PipeRunException(this, "could not find an active server");
			}

			String ldapUrl = getParameterValue(pvl, "ldapUrl");
			LdapSender ldapSender = null;
			if (StringUtils.isNotEmpty(ldapUrl)) {
				ldapSender = retrieveLdapSender(ldapUrl, cf);
			}

			queueNameWork = getParameterValue(pvl, "queueName");
			if (StringUtils.isNotEmpty(queueNameWork)) {
				String countOnlyStr = getParameterValue(pvl, "countOnly");
				boolean countOnly = "true".equalsIgnoreCase(countOnlyStr);
				if (countOnly) {
					return new PipeRunResult(getSuccessForward(), getQueueMessageCountOnly(admin, queueNameWork));
				}
			}

			ConnectionFactory factory = new com.tibco.tibjms.TibjmsConnectionFactory(urlWork, null, emsProperties);
			try (Connection connection = factory.createConnection(cf.getUsername(), cf.getPassword());
				 Session jSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {

				if (StringUtils.isNotEmpty(queueNameWork)) {
					String queueItemStr = getParameterValue(pvl, "queueItem");
					int qi;
					if (StringUtils.isNumeric(queueItemStr)) {
						qi = Integer.parseInt(queueItemStr);
					} else {
						qi = 1;
					}
					result = getQueueMessage(jSession, admin, queueNameWork, qi, ldapSender);
				} else {
					String showAgeStr = getParameterValue(pvl, "showAge");
					boolean showAge = "true".equalsIgnoreCase(showAgeStr);
					result = getQueuesInfo(jSession, admin, showAge, ldapSender);
				}
			}
		} catch (Exception e) {
			String msg = "exception on showing Tibco queues, url [" + urlWork + "]" + (StringUtils.isNotEmpty(queueNameWork) ? " queue [" + queueNameWork + "]" : "");
			throw new PipeRunException(this, msg, e);
		} finally {
			TibcoUtils.closeAdminClient(admin);
		}
		return new PipeRunResult(getSuccessForward(), result);
	}

	private LdapSender retrieveLdapSender(String ldapUrl, CredentialFactory cf) {
		try {
			LdapSender ldapSender = new LdapSender();
			ldapSender.setProviderURL(ldapUrl);
			ldapSender.setAttributesToReturn("cn,description");
			if (StringUtils.isNotEmpty(cf.getAlias())) {
				ldapSender.setJndiAuthAlias(cf.getAlias());
			}
			if (StringUtils.isNotEmpty(cf.getUsername())) {
				ldapSender.setPrincipal(cf.getUsername());
			}
			if (StringUtils.isNotEmpty(cf.getPassword())) {
				ldapSender.setCredentials(cf.getPassword());
			}
			Parameter p = new Parameter();
			p.setName("entryName");
			p.setXpathExpression("concat('cn=',*)");
			p.configure();
			ldapSender.addParameter(p);
			ldapSender.configure();
			return ldapSender;
		} catch (ConfigurationException e) {
			log.warn("exception on retrieving ldapSender", e);
		}
		return null;
	}

	private String getQueueMessage(Session jSession, TibjmsAdmin admin, String queueName, int queueItem, LdapSender ldapSender) throws TibjmsAdminException, JMSException {
		QueueInfo qInfo = admin.getQueue(queueName);
		if (qInfo == null) {
			throw new JMSException(" queue [" + queueName + "] does not exist");
		}

		XmlBuilder qMessageXml = new XmlBuilder("qMessage");
		ServerInfo serverInfo = admin.getInfo();
		String url = serverInfo.getURL();
		qMessageXml.addAttribute("url", url);
		String resolvedUrl = getResolvedUrl(url);
		if (resolvedUrl != null) {
			qMessageXml.addAttribute("resolvedUrl", resolvedUrl);
		}
		qMessageXml.addAttribute("timestamp", DateFormatUtils.getTimeStamp());
		qMessageXml.addAttribute("startTime", DateFormatUtils.format(serverInfo.getStartTime()));
		XmlBuilder qNameXml = new XmlBuilder("qName");
		qNameXml.setCdataValue(queueName);

		Queue queue = jSession.createQueue(queueName);
		QueueBrowser queueBrowser = null;
		try {
			queueBrowser = jSession.createBrowser(queue);
			Enumeration<?> enm = queueBrowser.getEnumeration();
			int count = 0;
			boolean found = false;
			String chompCharSizeString = AppConstants.getInstance().getString("browseQueue.chompCharSize", null);
			int chompCharSize = (int) Misc.toFileSize(chompCharSizeString, -1);

			while (enm.hasMoreElements() && !found) {
				count++;
				if (count == queueItem) {
					qNameXml.addAttribute("item", count);
					Object o = enm.nextElement();
					if (o instanceof jakarta.jms.Message msg) {
						XmlBuilder qMessageId = new XmlBuilder("qMessageId");
						qMessageId.setCdataValue(msg.getJMSMessageID());
						qMessageXml.addSubElement(qMessageId);
						XmlBuilder qTimestamp = new XmlBuilder("qTimestamp");
						qTimestamp.setCdataValue(DateFormatUtils.format(msg.getJMSTimestamp()));
						qMessageXml.addSubElement(qTimestamp);

						StringBuilder sb = new StringBuilder();
						Enumeration<?> propertyNames = msg.getPropertyNames();
						while (propertyNames.hasMoreElements()) {
							String propertyName = (String) propertyNames .nextElement();
							Object object = msg.getObjectProperty(propertyName);
							if (!sb.isEmpty()) {
								sb.append("; ");
							}
							sb.append(propertyName);
							sb.append("=");
							sb.append(object);
						}
						XmlBuilder qPropsXml = new XmlBuilder("qProps");
						qPropsXml.setCdataValue(sb.toString());

						qMessageXml.addSubElement(qPropsXml);
						XmlBuilder qTextXml = new XmlBuilder("qText");
						String msgText;
						try {
							TextMessage textMessage = (TextMessage) msg;
							msgText = textMessage.getText();
						} catch (ClassCastException e) {
							msgText = msg.toString();
							qTextXml.addAttribute("text", false);
						}
						int msgSize = msgText.length();
						if (isHideMessage()) {
							qTextXml.setCdataValue("***HIDDEN***");
						} else {
							if (chompCharSize >= 0 && msgSize > chompCharSize) {
								qTextXml.setCdataValue(msgText.substring(0, chompCharSize) + "...");
								qTextXml.addAttribute("chomped", true);
							} else {
								qTextXml.setCdataValue(msgText);
							}
						}
						qMessageXml.addSubElement(qTextXml);
						XmlBuilder qTextSizeXml = new XmlBuilder("qTextSize");
						qTextSizeXml.setValue(Misc.toFileSize(msgSize));
						qMessageXml.addSubElement(qTextSizeXml);
					}
					found = true;
				} else {
					enm.nextElement();
				}
			}
		} finally {
			if (queueBrowser != null) {
				try {
					queueBrowser.close();
				} catch (JMSException e) {
					log.warn("exception on closing queue browser", e);
				}
			}
		}

		qMessageXml.addSubElement(qNameXml);

		Map<String, String> aclMap = getAclMap(admin, ldapSender);
		XmlBuilder aclXml = new XmlBuilder("acl");
		XmlBuilder qInfoXml = qInfoToXml(qInfo);
		aclXml.setValue(aclMap.get(qInfo.getName()));
		qInfoXml.addSubElement(aclXml);
		qMessageXml.addSubElement(qInfoXml);

		Map<String, List<String>> consumersMap = getConnectedConsumersMap(admin);
		XmlBuilder consumerXml = new XmlBuilder("connectedConsumers");
		if (consumersMap.containsKey(qInfo.getName())) {
			List<String> consumers = consumersMap.get(qInfo.getName());
			if (consumers != null) {
				String consumersString = String.join("; ", consumers);
				consumerXml.setCdataValue(consumersString);
			}
		}
		qInfoXml.addSubElement(consumerXml);

		return qMessageXml.asXmlString();
	}

	private String getQueueMessageCountOnly(TibjmsAdmin admin, String queueName) throws TibjmsAdminException {
		QueueInfo queueInfo = admin.getQueue(queueName);
		long pendingMessageCount = queueInfo.getPendingMessageCount();
		return "<qCount>" + pendingMessageCount + "</qCount>";
	}

	private String getQueuesInfo(Session jSession, TibjmsAdmin admin, boolean showAge, LdapSender ldapSender) throws TibjmsAdminException {
		XmlBuilder qInfosXml = new XmlBuilder("qInfos");
		ServerInfo serverInfo = admin.getInfo();
		String url = serverInfo.getURL();
		qInfosXml.addAttribute("url", url);
		String resolvedUrl = getResolvedUrl(url);
		if (resolvedUrl != null) {
			qInfosXml.addAttribute("resolvedUrl", resolvedUrl);
		}
		Instant currentTime = TimeProvider.now();
		qInfosXml.addAttribute("timestamp", DateFormatUtils.format(currentTime));
		long startTime = serverInfo.getStartTime();
		qInfosXml.addAttribute("startTime", DateFormatUtils.format(startTime));
		qInfosXml.addAttribute("age", Misc.getAge(startTime));

		Map<String, String> aclMap = getAclMap(admin, ldapSender);
		Map<String, List<String>> consumersMap = getConnectedConsumersMap(admin);
		QueueInfo[] qInfos = admin.getQueues();
		for (int i = 0; i < qInfos.length; i++) {
			QueueInfo qInfo = qInfos[i];
			if (skipTemporaryQueues && qInfo.isTemporary()) {
				// skip
			} else {
				XmlBuilder qInfoXml = qInfoToXml(qInfo);
				qInfosXml.addSubElement(qInfoXml);
				XmlBuilder aclXml = new XmlBuilder("acl");
				aclXml.setValue(aclMap.get(qInfo.getName()));
				qInfoXml.addSubElement(aclXml);
				XmlBuilder consumerXml = new XmlBuilder("connectedConsumers");
				if (consumersMap.containsKey(qInfo.getName())) {
					List<String> consumers = consumersMap.get(qInfo.getName());
					if (consumers != null) {
						String consumersString = String.join("; ", consumers);
						consumerXml.setCdataValue(consumersString);
					}
				}
				qInfoXml.addSubElement(consumerXml);
				if (showAge) {
					if (qInfo.getReceiverCount() == 0
							&& qInfo.getPendingMessageCount() > 0) {
						String qfmAge;
						if (getQueueRegex() == null || qInfo.getName().matches(getQueueRegex())) {
							qfmAge = TibcoUtils.getQueueFirstMessageAgeAsString(jSession, qInfo.getName(), currentTime.toEpochMilli());
						} else {
							qfmAge = "?";
						}
						if (qfmAge != null) {
							XmlBuilder firstMsgAgeXml = new XmlBuilder("firstMsgAge");
							firstMsgAgeXml.setCdataValue(qfmAge);
							qInfoXml.addSubElement(firstMsgAgeXml);
						}
					}
				}
			}
		}
		return qInfosXml.asXmlString();
	}

	private XmlBuilder qInfoToXml(QueueInfo qInfo) {
		XmlBuilder qInfoXml = new XmlBuilder("qInfo");
		XmlBuilder qNameXml = new XmlBuilder("qName");
		String qName = qInfo.getName();
		qNameXml.setCdataValue(qName);
		qInfoXml.addSubElement(qNameXml);
		String qNameEncoded = XmlUtils.encodeURL(qName);
		if (!qNameEncoded.equals(qName)) {
			XmlBuilder qNameEncodedXml = new XmlBuilder("qNameEncoded");
			qNameEncodedXml.setCdataValue(qNameEncoded);
			qInfoXml.addSubElement(qNameEncodedXml);
		}
		XmlBuilder pendingMsgCountXml = new XmlBuilder("pendingMsgCount");
		long pendingMsgCount = qInfo.getPendingMessageCount();
		pendingMsgCountXml.setValue(Long.toString(pendingMsgCount));
		qInfoXml.addSubElement(pendingMsgCountXml);
		XmlBuilder pendingMsgSizeXml = new XmlBuilder("pendingMsgSize");
		long pendingMsgSize = qInfo.getPendingMessageSize();
		pendingMsgSizeXml.setValue(Misc.toFileSize(pendingMsgSize));
		qInfoXml.addSubElement(pendingMsgSizeXml);
		XmlBuilder receiverCountXml = new XmlBuilder("receiverCount");
		int receiverCount = qInfo.getReceiverCount();
		receiverCountXml.setValue(Integer.toString(receiverCount));
		qInfoXml.addSubElement(receiverCountXml);
		XmlBuilder inTotalMsgsXml = new XmlBuilder("inTotalMsgs");
		long inTotalMsgs = qInfo.getInboundStatistics().getTotalMessages();
		inTotalMsgsXml.setValue(Long.toString(inTotalMsgs));
		qInfoXml.addSubElement(inTotalMsgsXml);
		XmlBuilder outTotalMsgsXml = new XmlBuilder("outTotalMsgs");
		long outTotalMsgs = qInfo.getOutboundStatistics().getTotalMessages();
		outTotalMsgsXml.setValue(Long.toString(outTotalMsgs));
		qInfoXml.addSubElement(outTotalMsgsXml);
		XmlBuilder isStaticXml = new XmlBuilder("isStatic");
		isStaticXml.setValue(qInfo.isStatic() ? "true" : "false");
		qInfoXml.addSubElement(isStaticXml);
		XmlBuilder prefetchXml = new XmlBuilder("prefetch");
		int prefetch = qInfo.getPrefetch();
		prefetchXml.setValue(Integer.toString(prefetch));
		qInfoXml.addSubElement(prefetchXml);
		XmlBuilder isBridgedXml = new XmlBuilder("isBridged");
		BridgeTarget[] bta = qInfo.getBridgeTargets();
		isBridgedXml.setValue(bta.length == 0 ? "false" : "true");
		qInfoXml.addSubElement(isBridgedXml);

		if (bta.length != 0) {
			XmlBuilder bridgeTargetsXml = new XmlBuilder("bridgeTargets");
			String btaString = null;
			for (int j = 0; j < bta.length; j++) {
				BridgeTarget bridgeTarget = bta[j];
				if (btaString == null) {
					btaString = bridgeTarget.toString();
				} else {
					btaString = btaString + "; " + bridgeTarget.toString();
				}
			}
			bridgeTargetsXml.setCdataValue(btaString);
			qInfoXml.addSubElement(bridgeTargetsXml);
		}
		return qInfoXml;
	}

	private Map<String, String> getAclMap(TibjmsAdmin admin, LdapSender ldapSender) throws TibjmsAdminException {
		Map<String, String> userMap = new HashMap<>();
		Map<String, String> aclMap = new HashMap<>();
		ACLEntry[] aclEntries = admin.getACLEntries();
		for (int j = 0; j < aclEntries.length; j++) {
			ACLEntry aclEntry = aclEntries[j];
			String destination = aclEntry.getDestination().getName();
			String principal = aclEntry.getPrincipal().getName();
			String permissions = aclEntry.getPermissions().toString();
			String principalDescription = null;
			if (StringUtils.isEmpty(principal)) {
				continue;
			}

			if (userMap.containsKey(principal)) {
				principalDescription = userMap.get(principal);
			} else {
				if (ldapSender != null) {
					principalDescription = getLdapPrincipalDescription(principal, ldapSender);
				}
				if (principalDescription == null) {
					UserInfo principalUserInfo = admin.getUser(principal);
					if (principalUserInfo != null) {
						principalDescription = principalUserInfo.getDescription();
					}
				}
				userMap.put(principal, principalDescription);
			}

			String pp;
			if (principalDescription != null) {
				pp = principal + " (" + principalDescription + ")=" + permissions;
			} else {
				pp = principal + "=" + permissions;

			}
			if (aclMap.containsKey(destination)) {
				String ppe = aclMap.get(destination);
				aclMap.remove(destination);
				aclMap.put(destination, ppe + "; " + pp);
			} else {
				aclMap.put(destination, pp);
			}
		}
		return aclMap;
	}

	private String getLdapPrincipalDescription(String principal, LdapSender ldapSender) {
		String principalDescription = null;
		Message ldapRequest = new Message("<req>" + principal + "</req>");
		try (PipeLineSession session = new PipeLineSession();
			Message message = ldapSender.sendMessageOrThrow(ldapRequest, session)) {
			String ldapResult = message.asString();
			if (ldapResult != null) {
				Collection<String> c = XmlUtils.evaluateXPathNodeSet(ldapResult,"attributes/attribute[@name='description']/@value");
				if (!c.isEmpty()) {
					principalDescription = c.iterator().next();
				}
			}
		} catch (Exception e) {
			log.debug("Caught exception retrieving description for principal [{}]", principal, e);
			return null;
		}
		return principalDescription;
	}

	private String getResolvedUrl(String url) {
		URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			log.debug("Caught URISyntaxException while resolving url [{}]", url, e);
			return null;
		}
		InetAddress inetAddress;
		try {
			inetAddress = InetAddress.getByName(uri.getHost());
		} catch (UnknownHostException e) {
			log.debug("Caught UnknownHostException while resolving url [{}]", url, e);
			return null;
		}
		return inetAddress.getCanonicalHostName();
	}

	private Map<String, List<String>> getConnectedConsumersMap(TibjmsAdmin admin) throws TibjmsAdminException {
		Map<Long, String> connectionMap = new HashMap<>();
		ConnectionInfo[] connectionInfos = admin.getConnections();
		for (ConnectionInfo connectionInfo : connectionInfos) {
			long id = connectionInfo.getID();
			String clientId = connectionInfo.getClientID();
			if (StringUtils.isNotEmpty(clientId)) {
				connectionMap.put(id, clientId);
			}
		}

		Map<String, List<String>> consumerMap = new HashMap<>();
		ConsumerInfo[] consumerInfos = admin.getConsumers();
		for (ConsumerInfo consumerInfo : consumerInfos) {
			String destinationName = consumerInfo.getDestinationName();
			long connectionId = consumerInfo.getConnectionID();
			if (connectionMap.containsKey(connectionId)) {
				String ci = connectionMap.get(connectionId);
				List<String> consumers = consumerMap.computeIfAbsent(destinationName, k -> new ArrayList<>());
				consumers.add(ci);
			}
		}
		return consumerMap;
	}

	public String getUrl() {
		return url;
	}

	/** URL or base of URL to be used. When multiple URLs are defined (comma separated list), the first URL is used of which the server has an active state */
	public void setUrl(String string) {
		url = string;
	}

	public String getAuthAlias() {
		return authAlias;
	}

	/** alias used to obtain credentials for authentication to host */
	public void setAuthAlias(String string) {
		authAlias = string;
	}

	public String getUsername() {
		return username;
	}

	/** username used in authentication to host */
	public void setUsername(String string) {
		username = string;
	}

	public String getPassword() {
		return password;
	}

	/** password used in authentication to host */
	public void setPassword(String string) {
		password = string;
	}

	public boolean isSkipTemporaryQueues() {
		return skipTemporaryQueues;
	}

	/** when set to <code>true</code>, temporary queues are skipped
	 * @ff.default false
	 */
	public void setSkipTemporaryQueues(boolean b) {
		skipTemporaryQueues = b;
	}

	public boolean isHideMessage() {
		return hideMessage;
	}

	/** when set to <code>true</code>, the length of the queue message is returned instead of the queue message self (when parameter <code>queueName</code> is not empty)
	 * @ff.default false
	 */
	public void setHideMessage(boolean b) {
		hideMessage = b;
	}

	public String getQueueRegex() {
		return queueRegex;
	}

	public void setQueueRegex(String string) {
		queueRegex = string;
	}

	/** Location to a <code>jndi.properties</code> file for additional EMS (SSL) properties */
	public void setEmsPropertiesFile(String propertyFile) {
		emsPropertiesFile = propertyFile;
	}
}
