/*
   Copyright 2019 Nationale-Nederlanden, 2020 WeAreFrank!

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
package nl.nn.adapterframework.extensions.cmis;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.extensions.cmis.server.CmisEvent;
import nl.nn.adapterframework.extensions.cmis.server.CmisEventDispatcher;
import nl.nn.adapterframework.http.PushingListenerAdapter;

public class CmisEventListener extends PushingListenerAdapter implements HasPhysicalDestination {

	private CmisEvent cmisEvent = null;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if(cmisEvent == null)
			throw new ConfigurationException("no event has been defined to listen on");
	}

	@Override
	public void open() throws ListenerException {
		super.open();
		CmisEventDispatcher.getInstance().registerEventListener(this);
	}

	@Override
	public void close() {
		CmisEventDispatcher.getInstance().unregisterEventListener(this);
		super.close();
	}

	@Override
	public String getPhysicalDestinationName() {
		StringBuilder sb = new StringBuilder("event: ");
		sb.append(cmisEvent.name());

		return sb.toString();
	}

	@Override
	public String getName() {
		if(super.getName() == null)
			return cmisEvent.name()+"-EventListener";
		else
			return super.getName();
	}

	public void setEventListener(String event) {
		this.cmisEvent = CmisEvent.fromValue(event);
	}
	public CmisEvent getEvent() {
		return cmisEvent;
	}
}
