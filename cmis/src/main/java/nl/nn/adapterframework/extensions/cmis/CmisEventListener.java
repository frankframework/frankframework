package nl.nn.adapterframework.extensions.cmis;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.extensions.cmis.server.CmisEvent;
import nl.nn.adapterframework.extensions.cmis.server.CmisEventDispatcher;
import nl.nn.adapterframework.http.PushingListenerAdapter;
import nl.nn.adapterframework.receivers.ReceiverAware;

public class CmisEventListener extends PushingListenerAdapter<String> implements HasPhysicalDestination, ReceiverAware {

	private IReceiver receiver = null;
	private CmisEvent cmisEvent = null;

	public void configure() throws ConfigurationException {
		super.configure();

		if(cmisEvent == null)
			throw new ConfigurationException("no event has been defined to listen on");
	}

	@Override
	public void open() throws ListenerException {
		super.open();
		CmisEventDispatcher.getInstance().registerEventListener(this);
	}

	@Override
	public void close() {
		CmisEventDispatcher.getInstance().unregisterEventListener(this);
		super.close();
	}

	@Override
	public String getPhysicalDestinationName() {
		StringBuilder sb = new StringBuilder("event: ");
		sb.append(cmisEvent.toString());

		return sb.toString();
	}

	@Override
	public void setReceiver(IReceiver receiver) {
		this.receiver = receiver;
	}

	@Override
	public IReceiver getReceiver() {
		return receiver;
	}

	@Override
	public String getName() {
		if(super.getName() == null)
			return cmisEvent.toString()+"-EventListener";
		else
			return super.getName();
	}

	public void setEventListener(String event) {
		this.cmisEvent = CmisEvent.fromValue(event);
	}
	public CmisEvent getEvent() {
		return cmisEvent;
	}
}
