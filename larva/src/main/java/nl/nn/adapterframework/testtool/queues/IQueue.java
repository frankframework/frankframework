package nl.nn.adapterframework.testtool.queues;

import nl.nn.adapterframework.configuration.ConfigurationException;

public interface IQueue {

	default void configure() throws ConfigurationException {}
}
