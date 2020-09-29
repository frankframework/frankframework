/*
   Copyright 2020 WeAreFrank!

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
package nl.nn.adapterframework.configuration;

import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.util.LogUtil;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

/**
 * Used with test cases.
 * This implementation keeps a list of all {@link IAdapter}s within this IAdapterService.
 * It does not wire any extra's such as JMX and flowDiagrams.
 */
public class DummyAdapterService implements IAdapterService {

	protected final Logger log = LogUtil.getLogger(this);
	private final Map<String, IAdapter> adapters = new LinkedHashMap<>(); // insertion order map

	@Override
	public IAdapter getAdapter(String name) {
		return getAdapters().get(name);
	}

	@Override
	public final Map<String, IAdapter> getAdapters() {
		return Collections.unmodifiableMap(adapters);
	}

	@Override
	public void registerAdapter(IAdapter adapter) throws ConfigurationException {
		if(log.isDebugEnabled()) log.debug("registering adapter ["+adapter+"] with AdapterService ["+this+"]");
		if(adapter.getName() == null) {
			throw new ConfigurationException("Adapter has no name");
		}
		if(adapters.containsKey(adapter.getName())) {
			throw new ConfigurationException("Adapter [" + adapter.getName() + "] already registered.");
		}
		adapters.put(adapter.getName(), adapter);

		if(log.isDebugEnabled()) log.debug("configuring adapter ["+adapter+"]");
		adapter.configure();
	}

	@Override
	public void unRegisterAdapter(IAdapter adapter) {
		adapters.remove(adapter.getName());
		if(log.isDebugEnabled()) log.debug("unregistered adapter ["+adapter+"] from AdapterService ["+this+"]");
	}

}
