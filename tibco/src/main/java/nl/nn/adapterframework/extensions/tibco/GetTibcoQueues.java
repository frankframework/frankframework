/*
   Copyright 2013-2016, 2020 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.tibco;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
//import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.commons.lang3.StringUtils;

import com.tibco.tibjms.admin.ACLEntry;
import com.tibco.tibjms.admin.BridgeTarget;
import com.tibco.tibjms.admin.ConnectionInfo;
import com.tibco.tibjms.admin.ConsumerInfo;
import com.tibco.tibjms.admin.QueueInfo;
import com.tibco.tibjms.admin.ServerInfo;
import com.tibco.tibjms.admin.TibjmsAdmin;
import com.tibco.tibjms.admin.TibjmsAdminException;
import com.tibco.tibjms.admin.TibjmsAdminInvalidNameException;
import com.tibco.tibjms.admin.UserInfo;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.ldap.LdapSender;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.pipes.TimeoutGuardPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Returns information about Tibco queues in a XML string.
 * <p>
 * If the parameter <code>queueName</code> is empty then
 * <ul><li>all Tibco queues including information about these queues are returned</li></ul>
 * else
 * <ul><li>one message on a specific Tibco queue including information about this message is returned (without removing it)</li></ul>
 * </p>
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUrl(String) url}</td><td>URL or base of URL to be used. When multiple URLs are defined (comma separated list), the first URL is used of which the server has an active state</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAuthAlias(String) authAlias}</td><td>alias used to obtain credentials for authentication to host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUserName(String) userName}</td><td>username used in authentication to host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(String) password}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSkipTemporaryQueues(boolean) skipTemporaryQueues}</td><td>when set to <code>true</code>, temporary queues are skipped</td><td>false</td></tr>
 * <tr><td>{@link #setHideMessage(boolean) hideMessage}</td><td>when set to <code>true</code>, the length of the queue message is returned instead of the queue message self (when parameter <code>queueName</code> is not empty)</td><td>false</td></tr>
 * </table>
 * </p>
 * <p>
 * <table border="1">
 * <b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>url</td><td>string</td><td>When a parameter with name url is present, it is used instead of the url specified by the attribute</td></tr>
 * <tr><td>authAlias</td><td>string</td><td>When a parameter with name authAlias is present, it is used instead of the authAlias specified by the attribute</td></tr>
 * <tr><td>userName</td><td>string</td><td>When a parameter with name userName is present, it is used instead of the userName specified by the attribute</td></tr>
 * <tr><td>password</td><td>string</td><td>When a parameter with name password is present, it is used instead of the password specified by the attribute</td></tr>
 * <tr><td>queueName</td><td>string</td><td>the name of the queue which is used for browsing one queue</code></td></tr>
 * <tr><td>queueItem</td><td>string</td><td>the number of the queue message which is used for browsing one queue (default is 1)</td></tr>
 * <tr><td>showAge</td><td>boolean</td><td>when set to <code>true</code> and <code>pendingMsgCount&gt;0</code> and <code>receiverCount=0</code>, the age of the current first message in the queue is shown in the queues overview (default is false)</td></tr>
 * <tr><td>countOnly</td><td>boolean</td><td>when set to <code>true</code> and <code>queueName</code> is filled, only the number of pending messages is returned (default is false)</td></tr>
 * <tr><td>ldapUrl</td><td>string</td><td>When present, principal descriptions are retrieved from this LDAP server</td></tr>
 * </table>
 * </p>
 * 
 * @author Peter Leeuwenburgh
 * @version $Id$
 */

public class GetTibcoQueues extends TimeoutGuardPipe {
	private String url;
	private String authAlias;
	private String userName;
	private String password;
	private boolean skipTemporaryQueues = false;
	private boolean hideMessage = false;
	private String queueRegex;

	@Override
	public PipeRunResult doPipeWithTimeoutGuarded(Message input, PipeLineSession session) throws PipeRunException {
		String result;
		String url_work;
		String authAlias_work;
		String userName_work;
		String password_work;
		String queueName_work = null;

		ParameterValueList pvl = null;
		if (getParameterList() != null) {
			try {
				pvl = getParameterList().getValues(input, session);
			} catch (ParameterException e) {
				throw new PipeRunException(this, getLogPrefix(session) + "exception on extracting parameters", e);
			}
		}

		url_work = getParameterValue(pvl, "url");
		if (url_work == null) {
			url_work = getUrl();
		}
		authAlias_work = getParameterValue(pvl, "authAlias");
		if (authAlias_work == null) {
			authAlias_work = getAuthAlias();
		}
		userName_work = getParameterValue(pvl, "userName");
		if (userName_work == null) {
			userName_work = getUserName();
		}
		password_work = getParameterValue(pvl, "password");
		if (password_work == null) {
			password_work = getPassword();
		}

		CredentialFactory cf = new CredentialFactory(authAlias_work,
				userName_work, password_work);

		Connection connection = null;
		Session jSession = null;
		TibjmsAdmin admin = null;
		try {
			admin = TibcoUtils.getActiveServerAdmin(url_work, cf);
			if (admin == null) {
				throw new PipeRunException(this,
						"could not find an active server");
			}

			String ldapUrl = getParameterValue(pvl, "ldapUrl");
			LdapSender ldapSender = null;
			if (StringUtils.isNotEmpty(ldapUrl)) {
				ldapSender = retrieveLdapSender(ldapUrl, cf);
			}

			queueName_work = getParameterValue(pvl, "queueName");
			if (StringUtils.isNotEmpty(queueName_work)) {
				String countOnly_work = getParameterValue(pvl, "countOnly");
				boolean countOnly = ("true".equalsIgnoreCase(countOnly_work) ? true
						: false);
				if (countOnly) {
					return new PipeRunResult(getForward(), getQueueMessageCountOnly(admin, queueName_work));
				}
			}

			ConnectionFactory factory = new com.tibco.tibjms.TibjmsConnectionFactory(
					url_work);
			connection = factory.createConnection(cf.getUsername(),
					cf.getPassword());
			jSession = connection.createSession(false,
					javax.jms.Session.AUTO_ACKNOWLEDGE);

			if (StringUtils.isNotEmpty(queueName_work)) {
				String queueItem_work = getParameterValue(pvl, "queueItem");
				int qi;
				if (StringUtils.isNumeric(queueItem_work)) {
					qi = Integer.parseInt(queueItem_work);
				} else {
					qi = 1;
				}
				result = getQueueMessage(jSession, admin, queueName_work, qi, ldapSender);
			} else {
				String showAge_work = getParameterValue(pvl, "showAge");
				boolean showAge = ("true".equalsIgnoreCase(showAge_work) ? true
						: false);
				result = getQueuesInfo(jSession, admin, showAge, ldapSender);
			}
		} catch (Exception e) {
			String msg = getLogPrefix(session)
					+ "exception on showing Tibco queues, url ["
					+ url_work
					+ "]"
					+ (StringUtils.isNotEmpty(queueName_work) ? " queue ["
							+ queueName_work + "]" : "");
			throw new PipeRunException(this, msg, e);
		} finally {
			if (admin != null) {
				try {
					admin.close();
				} catch (TibjmsAdminException e) {
					log.warn(getLogPrefix(session)
							+ "exception on closing Tibjms Admin", e);
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (JMSException e) {
					log.warn(getLogPrefix(session)
							+ "exception on closing connection", e);
				}
			}
		}
		return new PipeRunResult(getForward(), result);
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
			log.warn(getLogPrefix(null) + "exception on retrieving ldapSender",
					e);
		}
		return null;
	}

	private String getQueueMessage(Session jSession, TibjmsAdmin admin,
			String queueName, int queueItem, LdapSender ldapSender) throws TibjmsAdminException,
			JMSException {
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
		qMessageXml.addAttribute("timestamp", DateUtils.getIsoTimeStamp());
		qMessageXml.addAttribute("startTime", DateUtils.format(
				serverInfo.getStartTime(), DateUtils.fullIsoFormat));
		XmlBuilder qNameXml = new XmlBuilder("qName");
		qNameXml.setCdataValue(queueName);

		Queue queue = jSession.createQueue(queueName);
		QueueBrowser queueBrowser = null;
		try {
			queueBrowser = jSession.createBrowser(queue);
			Enumeration<?> enm = queueBrowser.getEnumeration();
			int count = 0;
			boolean found = false;
			String chompCharSizeString = AppConstants.getInstance().getString(
					"browseQueue.chompCharSize", null);
			int chompCharSize = (int) Misc.toFileSize(chompCharSizeString, -1);

			while (enm.hasMoreElements() && !found) {
				count++;
				if (count == queueItem) {
					qNameXml.addAttribute("item", count);
					Object o = enm.nextElement();
					if (o instanceof javax.jms.Message) {
						javax.jms.Message msg = (javax.jms.Message) o;
						XmlBuilder qMessageId = new XmlBuilder("qMessageId");
						qMessageId.setCdataValue(msg.getJMSMessageID());
						qMessageXml.addSubElement(qMessageId);
						XmlBuilder qTimestamp = new XmlBuilder("qTimestamp");
						qTimestamp.setCdataValue(DateUtils.format(
								msg.getJMSTimestamp(), DateUtils.fullIsoFormat));
						qMessageXml.addSubElement(qTimestamp);

						StringBuffer sb = new StringBuffer("");
						Enumeration<?> propertyNames = msg.getPropertyNames();
						while (propertyNames.hasMoreElements()) {
							String propertyName = (String) propertyNames
									.nextElement();
							Object object = msg.getObjectProperty(propertyName);
							if (sb.length() > 0) {
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
								qTextXml.setCdataValue(msgText.substring(0,
										chompCharSize) + "...");
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
					log.warn(getLogPrefix(null)
							+ "exception on closing queue browser", e);
				}
			}
		}

		qMessageXml.addSubElement(qNameXml);

		Map<String, String> aclMap = getAclMap(admin, ldapSender);
		XmlBuilder aclXml = new XmlBuilder("acl");
		XmlBuilder qInfoXml = qInfoToXml(qInfo);
		aclXml.setValue((String) aclMap.get(qInfo.getName()));
		qInfoXml.addSubElement(aclXml);
		qMessageXml.addSubElement(qInfoXml);

		Map<String, LinkedList<String>> consumersMap = getConnectedConsumersMap(admin);
		XmlBuilder consumerXml = new XmlBuilder("connectedConsumers");
		if (consumersMap.containsKey(qInfo.getName())) {
			LinkedList<String> consumers = consumersMap.get(qInfo.getName());
			String consumersString = listToString(consumers);
			if (consumersString != null) {
				consumerXml.setCdataValue(consumersString);
			}
		}
		qInfoXml.addSubElement(consumerXml);

		return qMessageXml.toXML();
	}

	private String getQueueMessageCountOnly(TibjmsAdmin admin, String queueName)
			throws TibjmsAdminInvalidNameException, TibjmsAdminException {
		QueueInfo queueInfo = admin.getQueue(queueName);
		long pendingMessageCount = queueInfo.getPendingMessageCount();
		return "<qCount>" + String.valueOf(pendingMessageCount) + "</qCount>";
	}

	private String getQueuesInfo(Session jSession, TibjmsAdmin admin,
			boolean showAge, LdapSender ldapSender) throws TibjmsAdminException {
		XmlBuilder qInfosXml = new XmlBuilder("qInfos");
		ServerInfo serverInfo = admin.getInfo();
		String url = serverInfo.getURL();
		qInfosXml.addAttribute("url", url);
		String resolvedUrl = getResolvedUrl(url);
		if (resolvedUrl != null) {
			qInfosXml.addAttribute("resolvedUrl", resolvedUrl);
		}
		long currentTime = (new Date()).getTime();
		qInfosXml.addAttribute("timestamp",
				DateUtils.format(currentTime, DateUtils.fullIsoFormat));
		long startTime = serverInfo.getStartTime();
		qInfosXml.addAttribute("startTime",
				DateUtils.format(startTime, DateUtils.fullIsoFormat));
		qInfosXml.addAttribute("age", Misc.getAge(startTime));
		
		Map<String, String> aclMap = getAclMap(admin, ldapSender);
		Map<String, LinkedList<String>> consumersMap = getConnectedConsumersMap(admin);
		QueueInfo[] qInfos = admin.getQueues();
		for (int i = 0; i < qInfos.length; i++) {
			QueueInfo qInfo = qInfos[i];
			if (skipTemporaryQueues && qInfo.isTemporary()) {
				// skip
			} else {
				XmlBuilder qInfoXml = qInfoToXml(qInfo);
				qInfosXml.addSubElement(qInfoXml);
				XmlBuilder aclXml = new XmlBuilder("acl");
				aclXml.setValue((String) aclMap.get(qInfo.getName()));
				qInfoXml.addSubElement(aclXml);
				XmlBuilder consumerXml = new XmlBuilder("connectedConsumers");
				if (consumersMap.containsKey(qInfo.getName())) {
					LinkedList<String> consumers = consumersMap.get(qInfo.getName());
					String consumersString = listToString(consumers);
					if (consumersString != null) {
						consumerXml.setCdataValue(consumersString);
					}
				}
				qInfoXml.addSubElement(consumerXml);
				if (showAge) {
					if (qInfo.getReceiverCount() == 0
							&& qInfo.getPendingMessageCount() > 0) {
						String qfmAge;
						if (getQueueRegex() == null || qInfo.getName().matches(getQueueRegex())) {
							qfmAge = TibcoUtils.getQueueFirstMessageAgeAsString(jSession,
											qInfo.getName(), currentTime);
						} else {
							qfmAge = "?";
						}
						if (qfmAge != null) {
							XmlBuilder firstMsgAgeXml = new XmlBuilder(
									"firstMsgAge");
							firstMsgAgeXml.setCdataValue(qfmAge);
							qInfoXml.addSubElement(firstMsgAgeXml);
						}
					}
				}
			}
		}
		return qInfosXml.toXML();
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

	private String listToString(LinkedList<String> list) {
		String string = null;
		if (list != null) {
			for (Iterator<String> it = list.iterator(); it.hasNext();) {
				if (string == null) {
					string = it.next();
				} else {
					string = string + "; " + it.next();
				}
			}
		}
		return string;
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
			if (principal != null) {
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
			}
			String pp;
			if (principalDescription != null) {
				pp = principal + " (" + principalDescription + ")="
						+ permissions;
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
		nl.nn.adapterframework.stream.Message ldapRequest = new nl.nn.adapterframework.stream.Message("<req>" + principal + "</req>");
		try {
			String ldapResult = ldapSender.sendMessage(ldapRequest, null).asString();
			if (ldapResult != null) {
				Collection<String> c = XmlUtils.evaluateXPathNodeSet(ldapResult,"attributes/attribute[@name='description']/@value");
				if (c != null && c.size() > 0) {
					principalDescription = c.iterator().next();
				}
			}
		} catch (Exception e) {
			log.debug("Caught exception retrieving description for principal ["
					+ principal + "]: " + e.getMessage());
			return null;
		}
		return principalDescription;
	}

	private String getResolvedUrl(String url) {
		URI uri = null;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			log.debug("Caught URISyntaxException while resolving url [" + url + "]: "
					+ e.getMessage());
			return null;
		}
		InetAddress inetAddress = null;
		try {
			inetAddress = InetAddress.getByName(uri.getHost());
		} catch (UnknownHostException e) {
			log.debug("Caught UnknownHostException while resolving url [" + url + "]: "
					+ e.getMessage());
			return null;
		}
		return inetAddress.getCanonicalHostName();
	}

	private Map<String, LinkedList<String>> getConnectedConsumersMap(TibjmsAdmin admin) throws TibjmsAdminException {
		Map<Long, String> connectionMap = new HashMap<>();
		ConnectionInfo[] connectionInfos = admin.getConnections();
		for (int i = 0; i < connectionInfos.length; i++) {
			ConnectionInfo connectionInfo = connectionInfos[i];
			long id = connectionInfo.getID();
			String clientId = connectionInfo.getClientID();
			if (StringUtils.isNotEmpty(clientId)) {
				connectionMap.put(id, clientId);
			}
		}

		Map<String, LinkedList<String>> consumerMap = new HashMap<>();
		ConsumerInfo[] consumerInfos = admin.getConsumers();
		for (int i = 0; i < consumerInfos.length; i++) {
			ConsumerInfo consumerInfo = consumerInfos[i];
			String destinationName = consumerInfo.getDestinationName();
			long connectionId = consumerInfo.getConnectionID();
			if (connectionMap.containsKey(connectionId)) {
				String ci = (String) connectionMap.get(connectionId);
				if (consumerMap.containsKey(destinationName)) {
					LinkedList<String> consumers = consumerMap.get(destinationName);
					if (!consumers.contains(ci)) {
						consumers.add(ci);
						consumerMap.put(destinationName, consumers);
					}
				} else {
					LinkedList<String> consumers = new LinkedList<>();
					consumers.add(ci);
					consumerMap.put(destinationName, consumers);
				}
			}
		}
		return consumerMap;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String string) {
		url = string;
	}

	public String getAuthAlias() {
		return authAlias;
	}

	public void setAuthAlias(String string) {
		authAlias = string;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String string) {
		userName = string;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String string) {
		password = string;
	}

	public boolean isSkipTemporaryQueues() {
		return skipTemporaryQueues;
	}

	public void setSkipTemporaryQueues(boolean b) {
		skipTemporaryQueues = b;
	}

	public boolean isHideMessage() {
		return hideMessage;
	}

	public void setHideMessage(boolean b) {
		hideMessage = b;
	}

	public String getQueueRegex() {
		return queueRegex;
	}

	public void setQueueRegex(String string) {
		queueRegex = string;
	}
}
