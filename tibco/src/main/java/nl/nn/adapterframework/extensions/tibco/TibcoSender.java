/*
   Copyright 2013 Nationale-Nederlanden, 2021 WeAreFrank!

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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.jms.JmsException;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.jms.MessagingSourceFactory;

import org.apache.commons.lang3.StringUtils;

/**
 * Dedicated sender on Tibco Destinations.
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public class TibcoSender extends JmsSender {

	private String serverUrl;

	public TibcoSender() {
		super();
		setSoap(true);
	}

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getServerUrl())) {
			throw new ConfigurationException("serverUrl must be specified");
		}
		super.configure();
	}

	@Override
	protected MessagingSourceFactory getMessagingSourceFactory() {
		return new TibcoMessagingSourceFactory(this, isUseTopicFunctions());
	}
	/*
	 * 
	 * Tibco uses serverUrl instead of connectionFactoryName.
	 */
	@Override
	public String getConnectionFactoryName() throws JmsException {
		return getServerUrl();
	}

	/** URL (hostname and port, separated by ':') of Tibco-Server */
	public void setServerUrl(String string) {
		serverUrl = string;
	}
	public String getServerUrl() {
		return serverUrl;
	}


}
