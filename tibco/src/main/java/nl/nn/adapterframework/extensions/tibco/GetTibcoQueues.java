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
package nl.nn.adapterframework.extensions.tibco;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.TextMessage;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.commons.lang.StringUtils;

import com.tibco.tibjms.admin.ACLEntry;
import com.tibco.tibjms.admin.BridgeTarget;
import com.tibco.tibjms.admin.QueueInfo;
import com.tibco.tibjms.admin.TibjmsAdmin;
import com.tibco.tibjms.admin.TibjmsAdminException;
import com.tibco.tibjms.admin.UserInfo;

/**
 * Returns information about Tibco queues in a XML string.
 * <p>
 * If the parameter <code>queueName</code> is empty then
 * - all Tibco queues including information about these queues are returned
 * else
 * - one message on a specific Tibco queue including information about this message is returned (without removing it)
 * </p>
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUrl(String) url}</td><td>URL or base of URL to be used </td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAuthAlias(String) authAlias}</td><td>alias used to obtain credentials for authentication to host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUserName(String) userName}</td><td>username used in authentication to host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(String) password}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSkipTemporaryQueues(boolean) skipTemporaryQueues}</td><td>>when set to <code>true</code>, temporary queues are skipped</td><td>false</td></tr>
 * <tr><td>{@link #setThrowException(boolean) throwException}</td><td>when <code>true</code>, a PipeRunException is thrown. Otherwise the output is only logged as an error (and returned in a XML string).</td><td>true</td></tr>
 * <tr><td>{@link #setHideMessage(boolean) hideMessage}</td><td>>when set to <code>true</code>, the length of the queue message is returned instead of the queue message self (when parameter <code>queueName</code> is not empty)</td><td>false</td></tr>
 * </table>
 * </p>
 * <p>
 * <table border="1">
 * <b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>url</td><td>string</td><td>When a parameter with name serviceId is present, it is used instead of the url specified by the attribute</td></tr>
 * <tr><td>authAlias</td><td>string</td><td>When a parameter with name authAlias is present, it is used instead of the authAlias specified by the attribute</td></tr>
 * <tr><td>userName</td><td>string</td><td>When a parameter with name userName is present, it is used instead of the userName specified by the attribute</td></tr>
 * <tr><td>password</td><td>string</td><td>When a parameter with name password is present, it is used instead of the password specified by the attribute</td></tr>
 * <tr><td>queueName</td><td>string</td><td>the name of the queue which is used for <code>browseQueue=true</code></td></tr>
 * <tr><td>queueItem</td><td>string</td><td>the number of the queue message which is used for <code>browseQueue=true</code> (default is 1)</td></tr>
 * </table>
 * </p>
 * 
 * @author Peter Leeuwenburgh
 * @version $Id$
 */

public class GetTibcoQueues extends FixedForwardPipe {
	private String url;
	private String authAlias;
	private String userName;
	private String password;
	private boolean skipTemporaryQueues = false;
	private boolean throwException = true;
	private boolean hideMessage = false;

	public PipeRunResult doPipe(Object input, IPipeLineSession session)
			throws PipeRunException {
		String result;
		String url_work;
		String authAlias_work;
		String userName_work;
		String password_work;

		ParameterValueList pvl = null;
		if (getParameterList() != null) {
			ParameterResolutionContext prc = new ParameterResolutionContext(
					(String) input, session);
			try {
				pvl = prc.getValues(getParameterList());
			} catch (ParameterException e) {
				throw new PipeRunException(this, getLogPrefix(session)
						+ "exception on extracting parameters", e);
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

		String queueName_work = getParameterValue(pvl, "queueName");
		if (StringUtils.isNotEmpty(queueName_work)) {
			String queueItem_work = getParameterValue(pvl, "queueItem");
			int qi;
			if (StringUtils.isNumeric(queueItem_work)) {
				qi = Integer.parseInt(queueItem_work);
			} else {
				qi = 1;
			}
			result = getQueueMessage(getLogPrefix(session), url_work, cf,
					queueName_work, qi);
		} else {
			result = getQueuesInfo(getLogPrefix(session), url_work, cf);
		}

		return new PipeRunResult(getForward(), result);
	}

	private String getQueueMessage(String logPrefix, String url,
			CredentialFactory cf, String queueName, int queueItem)
			throws PipeRunException {
		XmlBuilder qMessageXml = new XmlBuilder("qMessage");
		qMessageXml.addAttribute("url", url);
		qMessageXml.addAttribute("timestamp", DateUtils.getIsoTimeStamp());
		XmlBuilder qNameXml = new XmlBuilder("qName");
		qNameXml.setCdataValue(queueName);
		Connection connection = null;
		Session jSession = null;
		try {
			ConnectionFactory factory = new com.tibco.tibjms.TibjmsConnectionFactory(
					url);
			connection = factory.createConnection(cf.getUsername(),
					cf.getPassword());
			jSession = connection.createSession(false,
					javax.jms.Session.AUTO_ACKNOWLEDGE);
			Queue queue = jSession.createQueue(queueName);
			QueueBrowser queueBrowser = jSession.createBrowser(queue);
			Enumeration enm = queueBrowser.getEnumeration();
			int count = 0;
			while (enm.hasMoreElements()) {
				count++;
				if (count == queueItem) {
					qNameXml.addAttribute("item", count);
					TextMessage msg = (TextMessage) enm.nextElement();
					XmlBuilder qMessageId = new XmlBuilder("qMessageId");
					qMessageId.setCdataValue(msg.getJMSMessageID());
					qMessageXml.addSubElement(qMessageId);
					XmlBuilder qTimestamp = new XmlBuilder("qTimestamp");
					qTimestamp.setCdataValue(DateUtils.format(msg.getJMSTimestamp(), DateUtils.fullIsoFormat));
					qMessageXml.addSubElement(qTimestamp);
					XmlBuilder qTextXml = new XmlBuilder("qText");
					if (isHideMessage()) {
						qTextXml.setCdataValue("[length=" + msg.getText().length() + "]");
					} else {
						qTextXml.setCdataValue(msg.getText());
					}
					qMessageXml.addSubElement(qTextXml);
				} else {
					enm.nextElement();
				}
			}
			qNameXml.addAttribute("count", count);
			qMessageXml.addSubElement(qNameXml);
		} catch (JMSException e) {
			String msg = logPrefix
					+ " exception on browsing Tibco queue [" + queueName
					+ "]";
			if (isThrowException()) {
				throw new PipeRunException(this, msg, e);
			} else {
				String msgString = msg + ": " + e.getMessage();
				log.error(msgString);
				String msgCdataString = "<![CDATA[" + msgString + "]]>";
				return "<error>" + msgCdataString + "</error>";
			}
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (JMSException e) {
					log.warn(logPrefix + "exception on closing connection", e);
				}
			}
		}
		return qMessageXml.toXML();
	}

	private String getQueuesInfo(String logPrefix, String url,
			CredentialFactory cf) throws PipeRunException {
		TibjmsAdmin admin = null;
		XmlBuilder qInfosXml = new XmlBuilder("qInfos");
		qInfosXml.addAttribute("url", url);
		qInfosXml.addAttribute("timestamp", DateUtils.getIsoTimeStamp());
		try {
			admin = new TibjmsAdmin(url, cf.getUsername(), cf.getPassword());

			Map userMap = new HashMap();
			Map aclMap = new HashMap();
			ACLEntry[] aclEntries = admin.getACLEntries();
			for (int j = 0; j < aclEntries.length; j++) {
				ACLEntry aclEntry = aclEntries[j];
				String destination = aclEntry.getDestination().getName();
				String principal = aclEntry.getPrincipal().getName();
				String permissions = aclEntry.getPermissions().toString();
				String principalDescription = null;
				if (principal != null) {
					if (userMap.containsKey(principal)) {
						principalDescription = (String) userMap.get(principal);
					} else {
						UserInfo principalUserInfo = admin.getUser(principal);
						if (principalUserInfo != null) {
							principalDescription = principalUserInfo
									.getDescription();
							userMap.put(principal, principalDescription);
						}
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
					String ppe = (String) aclMap.get(destination);
					aclMap.remove(destination);
					aclMap.put(destination, ppe + "; " + pp);
				} else {
					aclMap.put(destination, pp);
				}
			}

			QueueInfo[] qInfos = admin.getQueues();
			for (int i = 0; i < qInfos.length; i++) {
				QueueInfo qInfo = qInfos[i];
				if (skipTemporaryQueues && qInfo.isTemporary()) {
					// skip
				} else {
					XmlBuilder qInfoXml = new XmlBuilder("qInfo");
					XmlBuilder qNameXml = new XmlBuilder("qName");
					String qName = qInfo.getName();
					qNameXml.setCdataValue(qName);
					qInfoXml.addSubElement(qNameXml);
					XmlBuilder pendingMsgCountXml = new XmlBuilder(
							"pendingMsgCount");
					long pendingMsgCount = qInfo.getPendingMessageCount();
					pendingMsgCountXml.setValue(Long.toString(pendingMsgCount));
					qInfoXml.addSubElement(pendingMsgCountXml);
					XmlBuilder pendingMsgSizeXml = new XmlBuilder(
							"pendingMsgSize");
					long pendingMsgSize = qInfo.getPendingMessageSize();
					pendingMsgSizeXml.setValue(Misc.toFileSize(pendingMsgSize));
					qInfoXml.addSubElement(pendingMsgSizeXml);
					XmlBuilder receiverCountXml = new XmlBuilder(
							"receiverCount");
					int receiverCount = qInfo.getReceiverCount();
					receiverCountXml.setValue(Integer.toString(receiverCount));
					qInfoXml.addSubElement(receiverCountXml);
					XmlBuilder inTotalMsgsXml = new XmlBuilder("inTotalMsgs");
					long inTotalMsgs = qInfo.getInboundStatistics()
							.getTotalMessages();
					inTotalMsgsXml.setValue(Long.toString(inTotalMsgs));
					qInfoXml.addSubElement(inTotalMsgsXml);
					XmlBuilder outTotalMsgsXml = new XmlBuilder("outTotalMsgs");
					long outTotalMsgs = qInfo.getOutboundStatistics()
							.getTotalMessages();
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
						XmlBuilder bridgeTargetsXml = new XmlBuilder(
								"bridgeTargets");
						String btaString = null;
						for (int j = 0; j < bta.length; j++) {
							BridgeTarget bridgeTarget = bta[j];
							if (btaString == null) {
								btaString = bridgeTarget.toString();
							} else {
								btaString = btaString + "; "
										+ bridgeTarget.toString();
							}
						}
						bridgeTargetsXml.setCdataValue(btaString);
						qInfoXml.addSubElement(bridgeTargetsXml);
					}
					qInfosXml.addSubElement(qInfoXml);
					XmlBuilder aclXml = new XmlBuilder("acl");
					aclXml.setValue((String) aclMap.get(qName));
					qInfoXml.addSubElement(aclXml);
				}
			}
		} catch (TibjmsAdminException e) {
			String msg = logPrefix
					+ " Exception on getting Tibco queues for url [" + url
					+ "]";
			if (isThrowException()) {
				throw new PipeRunException(this, msg, e);
			} else {
				String msgString = msg + ": " + e.getMessage();
				log.error(msgString);
				String msgCdataString = "<![CDATA[" + msgString + "]]>";
				return "<error>" + msgCdataString + "</error>";
			}
		} finally {
			try {
				if (admin != null) {
					admin.close();
				}
			} catch (TibjmsAdminException e) {
				log.warn(getLogPrefix(null) + "exception closing Tibjms Admin",
						e);
			}
		}
		return qInfosXml.toXML();
	}

	private String getParameterValue(ParameterValueList pvl,
			String parameterName) {
		ParameterList parameterList = getParameterList();
		if (pvl != null && parameterList != null) {
			for (int i = 0; i < parameterList.size(); i++) {
				Parameter parameter = parameterList.getParameter(i);
				if (parameter.getName().equalsIgnoreCase(parameterName)) {
					return pvl.getParameterValue(i).asStringValue(null);
				}
			}
		}
		return null;
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

	public void setThrowException(boolean b) {
		throwException = b;
	}

	public boolean isThrowException() {
		return throwException;
	}

	public boolean isHideMessage() {
		return hideMessage;
	}

	public void setHideMessage(boolean b) {
		hideMessage = b;
	}
}
