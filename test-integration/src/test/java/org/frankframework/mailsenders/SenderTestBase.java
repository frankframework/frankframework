package org.frankframework.mailsenders;

import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;

org.frankframework.core.ISender;
		org.frankframework.core.PipeLineSession;
		org.frankframework.core.SenderException;

import org.frankframework.util.LogUtil;

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
