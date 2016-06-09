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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.rmi.server.UID;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.util.Misc;

public class ServiceClassLoader extends BytesClassLoader {

	public ServiceClassLoader(IbisManager ibisManager, String adapterName, String configurationName) {
		super(ServiceClassLoader.class.getClassLoader());
		IAdapter adapter = ibisManager.getRegisteredAdapter(adapterName);
		if (adapter != null) {
			IPipeLineSession pipeLineSession = new PipeLineSessionBase();
			PipeLineResult processResult = adapter.processMessage(getCorrelationId(), configurationName, pipeLineSession);
			Object object = pipeLineSession.get("configurationJar");
			if (object != null) {
				if (object instanceof byte[]) {
					readResources((byte[])object, configurationName);
				} else {
					log.error("SessionKey configurationJar not a byte array");
				}
			} else {
				log.error("SessionKey configurationJar not found");
			}
			
		} else {
			log.error("Could not find adapter: " + adapterName);
		}
	}

	private void readResources(byte[] jar, String configurationName) {
		JarInputStream jarInputStream = null;
		try {
			jarInputStream = new JarInputStream(new ByteArrayInputStream(jar));
			JarEntry jarEntry;
			while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
				resources.put(jarEntry.getName(), Misc.streamToBytes(jarInputStream));
			}
		} catch (IOException e) {
			log.error("Could not read resources from jar input stream for configuration '"
					+ configurationName + "'", e);
		} finally {
			if (jarInputStream != null) {
				try {
					jarInputStream.close();
				} catch (IOException e) {
					log.warn("Could not close jar input stream for configuration '"
							+ configurationName + "'", e);
				}
			}
		}
	}

	public String getCorrelationId() {
		return getClass().getName() + "-" + new UID().toString();
	}

}
