package nl.nn.adapterframework.filesystem;

public class ExchangeMailListenerOldTest extends ExchangeMailListenerTestBase {

	@Override
	protected IExchangeMailListener createExchangeMailListener() {
		return new ExchangeMailListenerOldWrapper();
	}

}
