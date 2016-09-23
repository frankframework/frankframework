package nl.nn.adapterframework.configuration;

import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

/**
 * This implemention of {@link AdapterService} also registers the adapters to Jmx, and configures the registered Adapters.

 * @author Michiel Meeuwissen
 * @since 5.0.29
 */
public class BasicAdapterServiceImpl extends AdapterServiceImpl {

    private static final Logger LOG = LogUtil.getLogger(BasicAdapterServiceImpl.class);

    @Override
    public void registerAdapter(IAdapter adapter) throws ConfigurationException {
        super.registerAdapter(adapter);
        try {
            // Throws javax.management.InstanceAlreadyExistsException when testing on
            // WebSphere 7. This code has probably never been enabled as previously it was
            // part of Configuration.java and was surrounded with "if (isEnableJMX())" with
            // enableJMX being false by default.
            LOG.debug("Registering adapter [" + adapter.getName() + "] to the JMX server");
            JmxMbeanHelper.hookupAdapter(adapter);
            LOG.info("[" + adapter.getName() + "] registered to the JMX server");
        } catch (Throwable t) {
            LOG.warn(t.getMessage());
        }
        adapter.configure();
    }

    @Override
    public void unRegisterAdapter(IAdapter adapter) {
        super.unRegisterAdapter(adapter);
        try {
            JmxMbeanHelper.unhookAdapter(adapter);
        } catch (Throwable t) {
            LOG.warn(t.getMessage());
        }
    }

}


