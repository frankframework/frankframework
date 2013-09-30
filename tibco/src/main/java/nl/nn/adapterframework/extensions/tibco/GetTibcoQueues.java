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

import java.util.HashMap;
import java.util.Map;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;

import com.tibco.tibjms.admin.ACLEntry;
import com.tibco.tibjms.admin.BridgeTarget;
import com.tibco.tibjms.admin.QueueInfo;
import com.tibco.tibjms.admin.TibjmsAdmin;
import com.tibco.tibjms.admin.TibjmsAdminException;

/**
 * Returns all Tibco queues (including information about these queues) in a XML string.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUrl(String) url}</td><td>URL or base of URL to be used </td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAuthAlias(String) authAlias}</td><td>alias used to obtain credentials for authentication to host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUserName(String) userName}</td><td>username used in authentication to host</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(String) password}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * </p>
 * 
 * @author Peter Leeuwenburgh
 * @version $Id: GetTibcoQueues.java,v 1.1 2013/08/29 07:51:19 m168309 Exp $
 */

public class GetTibcoQueues extends FixedForwardPipe {
	private String url;
	private String authAlias;
	private String userName;
	private String password;

	public PipeRunResult doPipe(Object input, IPipeLineSession session)
			throws PipeRunException {
		CredentialFactory cf = new CredentialFactory(getAuthAlias(), getUserName(), getPassword());
		TibjmsAdmin admin = null;
		XmlBuilder qInfosXml = new XmlBuilder("qInfos");
		try {
			admin = new TibjmsAdmin(getUrl(), cf.getUsername(), cf.getPassword());

			Map aclMap = new HashMap();
			ACLEntry[] aclEntries = admin.getACLEntries();
			for (int j = 0; j < aclEntries.length; j++) {
				ACLEntry aclEntry = aclEntries[j];
				String destination = aclEntry.getDestination().getName();
				String principal = aclEntry.getPrincipal().getName();
				String permissions = aclEntry.getPermissions().toString();
				String pp = principal + "=" + permissions;
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
				XmlBuilder qInfoXml = new XmlBuilder("qInfo");
				QueueInfo qInfo = qInfos[i];
				XmlBuilder qNameXml = new XmlBuilder("qName");
				String qName = qInfo.getName();
				qNameXml.setCdataValue(qName);
				qInfoXml.addSubElement(qNameXml);
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
				long inTotalMsgs = qInfo.getInboundStatistics()
						.getTotalMessages();
				inTotalMsgsXml.setValue(Long.toString(inTotalMsgs));
				qInfoXml.addSubElement(inTotalMsgsXml);
				XmlBuilder outTotalMsgsXml = new XmlBuilder("outTotalMsgs");
				long outTotalMsgs = qInfo.getOutboundStatistics()
						.getTotalMessages();
				outTotalMsgsXml.setValue(Long.toString(outTotalMsgs));
				qInfoXml.addSubElement(outTotalMsgsXml);
				XmlBuilder isBridgedXml = new XmlBuilder("isBridged");
				BridgeTarget[] bta = qInfo.getBridgeTargets();
				isBridgedXml.setValue(bta.length==0?"false":"true");
				qInfoXml.addSubElement(isBridgedXml);
				qInfosXml.addSubElement(qInfoXml);
				XmlBuilder aclXml = new XmlBuilder("acl");
				aclXml.setValue((String)aclMap.get(qName));
				qInfoXml.addSubElement(aclXml);
			}

		} catch (TibjmsAdminException e) {
			throw new PipeRunException(this, getLogPrefix(session)
					+ " Exception on getting Tibco queues", e);
		} finally {
			try {
				if (admin != null) {
					admin.close();
				}
			} catch (TibjmsAdminException e) {
				log.warn(getLogPrefix(null) + "exception closing Tibjms Admin", e);
			}
		}
		return new PipeRunResult(getForward(), qInfosXml.toXML());
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
}
