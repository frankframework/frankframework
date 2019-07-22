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
package nl.nn.adapterframework.http;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.doc.IbisDescription; 
import nl.nn.adapterframework.receivers.ServiceDispatcher;

import org.apache.commons.lang.StringUtils;


/** 
 * @author  Gerrit van Brakel 
 * @since   4.4.x (still experimental)
 */
@IbisDescription(
	"Implementation of a {@link IPushingListener IPushingListener} that enables a {@link nl.nn.adapterframework.receivers.GenericReceiver} \n" + 
	"to receive messages from HTTP requests. \n" + 
	"</table> \n" 
)
public class HttpListener extends PushingListenerAdapter implements HasPhysicalDestination {

	private String serviceName;

	/**
	 * initialize listener and register <code>this</code> to the JNDI
	 */
	public void configure() throws ConfigurationException {
		super.configure();
		try {
			if (StringUtils.isEmpty(getServiceName())) {
				log.debug("registering listener ["+getName()+"] with ServiceDispatcher");
				ServiceDispatcher.getInstance().registerServiceClient(getName(), this);
			} else {
				log.debug("registering listener ["+getName()+"] with ServiceDispatcher by serviceName ["+getServiceName()+"]");
				ServiceDispatcher.getInstance().registerServiceClient(getServiceName(), this);
			}
		} catch (Exception e){
			throw new ConfigurationException(e);
		}
	}

	public String getPhysicalDestinationName() {
		return "serviceName: "+getServiceName();
	}


	public String getServiceName() {
		return serviceName;
	}

	@IbisDoc({"name of the service that is provided by the adapter of this listener", ""})
	public void setServiceName(String string) {
		serviceName = string;
	}

}
