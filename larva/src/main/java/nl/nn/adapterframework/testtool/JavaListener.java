package nl.nn.adapterframework.testtool;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.testtool.queues.IQueue;

public class JavaListener extends nl.nn.adapterframework.receivers.JavaListener implements IQueue {

	public JavaListener() {
		setHandler(new ListenerMessageHandler<>());
		setName("Test Tool JavaListener");
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
	}

	public void setTimeout(int timeout) {
		getHandler().setTimeout(timeout);
	}

	public void setRequestTimeOut(int timeout) {
		setTimeout(timeout);
	}
	public void setResponseTimeOut(int timeout) {
		setTimeout(timeout);
	}

	@Override
	public ListenerMessageHandler<String> getHandler() {
		return (ListenerMessageHandler<String>) super.getHandler();
	}
}
