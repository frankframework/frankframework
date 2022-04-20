package nl.nn.adapterframework.testutil;

import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Semaphore;

public class ConcurrentActionTester extends Thread {
	protected static Logger log = LogUtil.getLogger(ConcurrentActionTester.class);
	
	private @Setter Semaphore initActionDone;
	private @Setter Semaphore waitBeforeAction;
	private @Setter Semaphore actionDone;
	private @Setter Semaphore waitAfterAction;
	private @Setter Semaphore finalizeActionDone;
	
	private @Getter Exception caught;

	public void initAction() throws Exception {}
	public void action() throws Exception {}
	public void finalizeAction() throws Exception {}
		
	@Override
	public void run() {
		try {
			initAction();
			if (initActionDone!=null) initActionDone.release();
			try {
				if (waitBeforeAction!=null) waitBeforeAction.acquire();
				action();
				if (actionDone!=null) actionDone.release();
				if (waitAfterAction!=null) waitAfterAction.acquire();
			} finally {
				finalizeAction();
			}
		
		} catch (Exception e) {
			log.warn("Exception in ConcurrentActionTester: ", e);
			caught = e;
		} finally {
			if (finalizeActionDone!=null) finalizeActionDone.release();
		}
	}

}
