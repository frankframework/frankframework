/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2022 WeAreFrank!

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
package org.frankframework.http;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.IPushingListener;
import org.frankframework.http.rest.ApiListener;
import org.frankframework.receivers.Receiver;
import org.frankframework.receivers.ServiceDispatcher;

/**
 * Implementation of a {@link IPushingListener IPushingListener} that enables a {@link Receiver}
 * to receive messages from HTTP requests. If you are writing a new configuration, you are recommended to use
 * an {@link ApiListener} or a {@link WebServiceListener}
 * instead.
 *
 * @author  Gerrit van Brakel
 * @since   4.4.x (still experimental)
 */
@Deprecated(forRemoval = true, since = "7.8.0")
public class HttpListener extends PushingListenerAdapter implements HasPhysicalDestination {

	private final @Getter String domain = "Http";
	private @Getter String serviceName;

	@Override
	public void start() {
		if (StringUtils.isEmpty(getServiceName())) {
			log.debug("registering listener [{}] with ServiceDispatcher", getName());
			ServiceDispatcher.getInstance().registerServiceClient(getName(), this);
		} else {
			log.debug("registering listener [{}] with ServiceDispatcher by serviceName [{}]", getName(), getServiceName());
			ServiceDispatcher.getInstance().registerServiceClient(getServiceName(), this);
		}

		super.start();
	}

	@Override
	public void stop() {
		super.stop();

		if (StringUtils.isEmpty(getServiceName())) {
			log.debug("unregistering listener [{}] from ServiceDispatcher", getName());
			ServiceDispatcher.getInstance().unregisterServiceClient(getName());
		} else {
			log.debug("unregistering listener [{}] from ServiceDispatcher by serviceName [{}]", getName(), getServiceName());
			ServiceDispatcher.getInstance().unregisterServiceClient(getServiceName());
		}
	}

	@Override
	public String getPhysicalDestinationName() {
		return "serviceName: "+getServiceName();
	}

	/** name of the service that is provided by the adapter of this listener */
	public void setServiceName(String string) {
		serviceName = string;
	}

}
