/*
   Copyright 2016, 2018-2019 Nationale-Nederlanden, 2020 WeAreFrank!

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
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.stream.Message;

/**
 * ClassLoader that retrieves a configuration jar from an IBIS adapter
 * The jar must be put in the sessionkey 'configurationJar'
 * 
 * @author Jaco de Groot
 *
 */
public class ServiceClassLoader extends JarBytesClassLoader {
	private String adapterName;

	public ServiceClassLoader(ClassLoader parent) {
		super(parent);
	}

	@Override
	protected Map<String, byte[]> loadResources() throws ConfigurationException {
		if (adapterName == null) {
			throw new ConfigurationException("Name of adapter to provide configuration jar not specified");
		}
		IAdapter adapter = getIbisContext().getIbisManager().getRegisteredAdapter(adapterName);
		if (adapter != null) {
			IPipeLineSession pipeLineSession = new PipeLineSessionBase();
			adapter.processMessage(getCorrelationId(), new Message(getConfigurationName()), pipeLineSession);
			//TODO check result of pipeline
			Object object = pipeLineSession.get("configurationJar");
			if (object != null) {
				if (object instanceof byte[]) {
					return readResources((byte[])object);
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

	public void setAdapterName(String adapterName) {
		this.adapterName = adapterName;
	}

	private String getCorrelationId() {
		return getClass().getName() + "-" + new UID().toString();
	}

}
