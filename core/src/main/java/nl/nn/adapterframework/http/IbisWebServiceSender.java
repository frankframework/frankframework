/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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
package nl.nn.adapterframework.http;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.soap.SOAPException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.receivers.ServiceDispatcher_ServiceProxy;
import nl.nn.adapterframework.senders.SenderBase;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;

/**
 * Posts a message to another IBIS-adapter as a WebService.
 *
 *
 * @author Gerrit van Brakel
 * @since 4.2
 */
public class IbisWebServiceSender extends SenderBase implements HasPhysicalDestination {

	private String ibisHost = "localhost";
	private String ibisInstance = null;
	private String serviceName = "serviceListener";
	private ServiceDispatcher_ServiceProxy proxy;
	
	@Override
	public void configure() throws ConfigurationException {
		if (ibisInstance==null) {
			ibisInstance=AppConstants.getInstance().getResolvedProperty("instance.name");
		}
		try {
			proxy = new ServiceDispatcher_ServiceProxy();
			proxy.setEndPoint(new URL(getEndPoint()));
		} catch (MalformedURLException e) {
			throw new ConfigurationException("IbisWebServiceSender cannot find URL from ["+getEndPoint()+"]", e);
		}
	}

	@Override
	public void open() throws SenderException {
	}

	@Override
	public void close() throws SenderException {
	}

	@Override
	public boolean isSynchronous() {
		return true;
	}

	@Override
	public Message sendMessage(Message message, PipeLineSession session) throws SenderException, TimeOutException {
		String correlationID = session==null ? null : session.getMessageId();
		try {
			//TODO: afvangen als server gestopt is, en timeout van maken ofzo.
			return new Message(proxy.dispatchRequest(getServiceName(),correlationID,message.asString()));
		} catch (SOAPException | IOException e) {
			throw new SenderException("exception sending message with correlationID ["+correlationID+"] to endPoint["+getEndPoint()+"]",e);
		}
	}

	protected String getEndPoint() {
		return "http://"+getIbisHost()+"/"+getIbisInstance()+"/services";
	}

	@Override
	public String getPhysicalDestinationName() {
		return getEndPoint()+" - "+getServiceName();
	}

	public String getIbisHost() {
		return ibisHost;
	}

	@IbisDoc({"name (or ipaddress) and optinally port of the host where the ibis to be called is running", "localhost"})
	public void setIbisHost(String ibisHost) {
		this.ibisHost=ibisHost;
	}
	
	public String getIbisInstance() {
		return ibisInstance;
	}

	@IbisDoc({"name of the ibis instance to be called", "name of the current instance"})
	public void setIbisInstance(String ibisInstance) {
		this.ibisInstance=ibisInstance;
	}

	public String getServiceName() {
		return serviceName;
	}

	@IbisDoc({"name of the receiver that should be called", "servicelistener"})
	public void setServiceName(String serviceName) {
		this.serviceName=serviceName;
	}

}
