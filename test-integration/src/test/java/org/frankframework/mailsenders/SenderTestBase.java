package org.frankframework.mailsenders;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;

import org.apache.logging.log4j.Logger;
import org.frankframework.core.ISender;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.util.LogUtil;

public abstract class SenderTestBase<S extends ISender> {

	protected Logger log = LogUtil.getLogger(this);
	protected S sender;

	@Mock
	protected PipeLineSession session;

	public abstract S createSender() throws Exception;

	@BeforeEach
	public void setup() throws Exception {
		sender = createSender();
		//		sender.open();
	}

	@AfterEach
	public void setdown() throws SenderException {
		if (sender != null) {
			sender.close();
		}
	}

}
