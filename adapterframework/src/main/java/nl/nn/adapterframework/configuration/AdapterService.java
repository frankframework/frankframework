package nl.nn.adapterframework.configuration;

import nl.nn.adapterframework.core.IAdapter;

import java.util.Map;

/**
 * @author Michiel Meeuwissen
 * @since 2.0.59
 */
public interface AdapterService {

    IAdapter getAdapter(String name);

    Map<String, IAdapter> getAdapters();

    void registerAdapter(IAdapter adapter) throws ConfigurationException;
}
