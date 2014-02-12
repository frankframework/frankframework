package nl.nn.adapterframework.configuration;

import nl.nn.adapterframework.core.IAdapter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *  * Straight forward implemenation of {@link AdapterService}, which is only filled by calls to {@link #registerAdapter(nl.nn.adapterframework.core.IAdapter)}, typically by digester rules via {@link Configuration#registerAdapter(nl.nn.adapterframework.core.IAdapter)}
 *
 * @author Michiel Meeuwissen
 * @since 5.4
 */
public class AdapterServiceImpl implements AdapterService {


    private final Map<String, IAdapter> adapters = new LinkedHashMap<String, IAdapter>(); // insertion order map

    //@Override
    public IAdapter getAdapter(String name) {
        return getAdapters().get(name);
    }

    //@Override
    public Map<String, IAdapter> getAdapters() {
        return Collections.unmodifiableMap(adapters);
    }

    //@Override
    public void registerAdapter(IAdapter adapter) throws ConfigurationException {
        if (adapter.getName() == null) {
            throw new ConfigurationException("Adapter has no name");
        }
        if (adapters.containsKey(adapter.getName())) {
            throw new ConfigurationException("Adapter [" + adapter.getName() + "] already registered.");
        }
        adapters.put(adapter.getName(), adapter);
    }

    protected IAdapter unRegisterAdapter(String name) {
        return adapters.remove(name);
    }

}
