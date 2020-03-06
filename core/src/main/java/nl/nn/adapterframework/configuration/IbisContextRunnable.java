package nl.nn.adapterframework.configuration;

import org.apache.logging.log4j.LogManager;

import org.apache.logging.log4j.Logger;

public class IbisContextRunnable implements Runnable {
	private static final int IBIS_INIT_EXCEPTION_TIMEOUT = 60*1000;
	private Logger log = LogManager.getLogger(this);
	private IbisContext ibisContext;

	public IbisContextRunnable(IbisContext ibisContext) {
		this.ibisContext = ibisContext;
	}

	@Override
	public void run() {
		try {
			log.trace("Starting IbisContextRunnable thread...");
			Thread.sleep(IBIS_INIT_EXCEPTION_TIMEOUT);
			if(ibisContext.getIbisManager() != null) {
				log.debug("Tried to initialize the ibisContext but has already been initialized, cancelling...");
			}
			else {
				ibisContext.init();
			}
		} catch (InterruptedException e) {
			log.debug("Interrupted IbisContextRunnable", e);
		}
	}

	public void setIbisContext(IbisContext ibisContext) {
		this.ibisContext = ibisContext;
	}
}
