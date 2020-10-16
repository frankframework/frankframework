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
import nl.nn.adapterframework.util.flow.FlowDiagramManager;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Straight forward implementation of {@link IAdapterService}, which is only filled by calls to 
 * {@link #registerAdapter(nl.nn.adapterframework.core.IAdapter)}, typically by digester rules 
 * via {@link Configuration#registerAdapter(nl.nn.adapterframework.core.IAdapter)}
 *
 * @author Michiel Meeuwissen
 * @since 5.4
 */
public class AdapterService implements IAdapterService {

	protected final Logger log = LogUtil.getLogger(this);
	private final Map<String, IAdapter> adapters = new LinkedHashMap<>(); // insertion order map
	private FlowDiagramManager flowDiagramManager;

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

		if (flowDiagramManager != null) {
			try {
				flowDiagramManager.generate(adapter);
			} catch (IOException e) {
				ConfigurationWarnings.add(adapter, log, "error generating flow diagram", e);
			}
		}
	}

	@Override
	public void unRegisterAdapter(IAdapter adapter) {
		adapters.remove(adapter.getName());
		if(log.isDebugEnabled()) log.debug("unregistered adapter ["+adapter+"] from AdapterService ["+this+"]");
	}

	@Autowired(required = false)
	@Qualifier("flowDiagramManager")
	public void setFlowDiagramManager(FlowDiagramManager flowDiagramManager) {
		this.flowDiagramManager = flowDiagramManager;
	}
}
