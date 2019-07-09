package nl.nn.adapterframework.senders;

import org.apache.commons.lang3.NotImplementedException;

import nl.nn.adapterframework.receivers.ExchangeMailListenerOld;

public class ExchangeMailListenerOldWrapper extends ExchangeMailListenerOld implements IExchangeMailListener {

	@Override
	public void setBaseFolder(String folder) {
		throw new NotImplementedException("setBaseFolder not implemented");
	}

	@Override
	public void setMinStableTime(long minStableTime) {
		throw new NotImplementedException("does not support minStableTime");
	}

	@Override
	public void setDelete(boolean delete) {
		throw new NotImplementedException("does not support delete");
	}

	@Override
	public void setMessageType(String messageType) {
		throw new NotImplementedException("does not support messageType");
	}

}
