package nl.nn.adapterframework.testtool.queues;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ListenerException;

public interface IQueue {

	default void configure() throws ConfigurationException {}
	default void open() throws ListenerException {}
}
