package nl.nn.adapterframework.senders;

public class ExchangeMailListenerOldTest extends ExchangeMailListenerTestBase {

	@Override
	protected IExchangeMailListener createExchangeMailListener() {
		return new ExchangeMailListenerOldWrapper();
	}

}
