package nl.nn.adapterframework.configuration;

import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.util.LogUtil;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Straight forward implemenation of {@link AdapterService}, which is only filled by calls to {@link #registerAdapter(nl.nn.adapterframework.core.IAdapter)}, typically by digester rules via {@link Configuration#registerAdapter(nl.nn.adapterframework.core.IAdapter)}
 *
 * @author Michiel Meeuwissen
 * @since 2.0.59
 */
public class BasicAdapterServiceImpl implements AdapterService {

    private static final Logger LOG = LogUtil.getLogger(BasicAdapterServiceImpl.class);


    private final Map<String, IAdapter> adapters = new LinkedHashMap<String, IAdapter>(); // insertion order map

    //@Override
    public IAdapter getAdapter(String name) {
        return adapters.get(name);
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
// Throws javax.management.InstanceAlreadyExistsException when testing on
// WebSphere 7. This code has probably never been enabled as previously it was
// part of Configuration.java and was surrounded with "if (isEnableJMX())" with
// enableJMX being false by default.
//        LOG.debug("Registering adapter [" + adapter.getName() + "] to the JMX server");
//        JmxMbeanHelper.hookupAdapter(adapter);
//        LOG.info("[" + adapter.getName() + "] registered to the JMX server");
        adapter.configure();
    }

    protected void unRegisterAdapter(String name) {
        IAdapter removed = adapters.remove(name);
//        if (removed != null) {
//            JmxMbeanHelper.unhookAdapter(removed);
//        }
    }

}


