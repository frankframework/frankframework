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
/*
 * $Log: IbisWebServiceSender.java,v $
 * Revision 1.9  2011-11-30 13:52:01  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:43  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.7  2010/03/10 14:30:05  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * rolled back testtool adjustments (IbisDebuggerDummy)
 *
 * Revision 1.5  2007/05/29 11:08:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected version string
 *
 * Revision 1.4  2007/05/29 11:07:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * implementation of HasPhysicalDestination
 *
 * Revision 1.3  2004/08/31 15:51:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added ToDo
 *
 * Revision 1.2  2004/08/09 13:55:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ServiceListenerName to ServiceName
 *
 * Revision 1.1  2004/07/15 07:40:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of http package
 *
 * Revision 1.1  2004/06/01 13:53:22  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * eerste versie
 *
 */
package nl.nn.adapterframework.http;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.soap.SOAPException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.receivers.ServiceDispatcher_ServiceProxy;
import nl.nn.adapterframework.util.AppConstants;

/**
 * Posts a message to another IBIS-adapter as a WebService.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.http.IbisWebServiceSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setIbisHost(String) ibisHost}</td><td>name (or ipaddress) and optinally port of the host where the ibis to be called is running</td><td>localhost</td></tr>
 * <tr><td>{@link #setIbisInstance(String) ibisInstance}</td><td>name of the ibis instance to be called</td><td>name of the current instance</td></tr>
 * <tr><td>{@link #setServiceName(String) serviceName}</td><td>Name of the receiver that should be called</td><td>"serviceListener"</td></tr>
 * </table>
 * </p>
 *
 * @author Gerrit van Brakel
 * @since 4.2
 */
public class IbisWebServiceSender implements ISender, HasPhysicalDestination {
	public static final String version="$RCSfile: IbisWebServiceSender.java,v $ $Revision: 1.9 $ $Date: 2011-11-30 13:52:01 $";

	private String name;
	private String ibisHost = "localhost";
	private String ibisInstance = null;
	private String serviceName = "serviceListener";
	private ServiceDispatcher_ServiceProxy proxy;
	
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

	public void open() throws SenderException {
	}

	public void close() throws SenderException {
	}

	public boolean isSynchronous() {
		return true;
	}

	public String sendMessage(String correlationID, String message)
		throws SenderException, TimeOutException {
		try {
			//TODO: afvangen als server gestopt is, en timeout van maken ofzo.
			return proxy.dispatchRequest(getServiceName(),correlationID,message);
		} catch (SOAPException e) {
			throw new SenderException("exception sending message with correlationID ["+correlationID+"] to endPoint["+getEndPoint()+"]",e);
		}
	}

	protected String getEndPoint() {
		return "http://"+getIbisHost()+"/"+getIbisInstance()+"/servlet/rpcrouter";
	}

	public String getPhysicalDestinationName() {
		return getEndPoint()+" - "+getServiceName();
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name=name;
	}
	
	public String getIbisHost() {
		return ibisHost;
	}
	public void setIbisHost(String ibisHost) {
		this.ibisHost=ibisHost;
	}
	
	public String getIbisInstance() {
		return ibisInstance;
	}
	public void setIbisInstance(String ibisInstance) {
		this.ibisInstance=ibisInstance;
	}

	public String getServiceName() {
		return serviceName;
	}
	public void setServiceName(String serviceName) {
		this.serviceName=serviceName;
	}

}
