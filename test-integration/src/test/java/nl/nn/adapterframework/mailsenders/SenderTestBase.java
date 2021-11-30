package nl.nn.adapterframework.mailsenders;

import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;

import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.LogUtil;

public abstract class SenderTestBase<S extends ISender> {

	protected Logger log = LogUtil.getLogger(this);
	protected S sender;

	@Mock
	protected PipeLineSession session;

	public abstract S createSender() throws Exception;

	@Before
	public void setup() throws Exception {
		sender = createSender();
		//		sender.open();
	}

	@After
	public void setdown() throws SenderException {
		if (sender != null) {
			sender.close();
		}
	}

}