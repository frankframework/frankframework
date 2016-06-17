/*
   Copyright 2016 Nationale-Nederlanden

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
package nl.nn.adapterframework.configuration.classloaders;

import java.rmi.server.UID;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSessionBase;

public class ServiceClassLoader extends JarBytesClassLoader {

	public ServiceClassLoader(IbisManager ibisManager, String adapterName, String configurationName) throws ConfigurationException {
		super(ServiceClassLoader.class.getClassLoader());
		if (adapterName == null) {
			throw new ConfigurationException("Name of adapter to provide configuration jar not specified");
		}
		IAdapter adapter = ibisManager.getRegisteredAdapter(adapterName);
		if (adapter != null) {
			IPipeLineSession pipeLineSession = new PipeLineSessionBase();
			PipeLineResult processResult = adapter.processMessage(getCorrelationId(), configurationName, pipeLineSession);
			Object object = pipeLineSession.get("configurationJar");
			if (object != null) {
				if (object instanceof byte[]) {
					readResources((byte[])object, configurationName);
				} else {
					throw new ConfigurationException("SessionKey configurationJar not a byte array");
				}
			} else {
				throw new ConfigurationException("SessionKey configurationJar not found");
			}
			
		} else {
			throw new ConfigurationException("Could not find adapter: " + adapterName);
		}
	}

	public String getCorrelationId() {
		return getClass().getName() + "-" + new UID().toString();
	}

}
