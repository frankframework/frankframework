package org.frankframework.testutil;

import java.sql.SQLException;
import java.util.concurrent.Semaphore;

import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.dbms.DbmsException;
import org.frankframework.dbms.JdbcException;
import org.frankframework.util.LogUtil;

public abstract class ConcurrentActionTester extends Thread {
	protected static Logger log = LogUtil.getLogger(ConcurrentActionTester.class);

	private @Setter Semaphore initActionDone;
	private @Setter Semaphore waitBeforeAction;
	private @Setter Semaphore actionDone;
	private @Setter Semaphore waitAfterAction;
	private @Setter Semaphore finalizeActionDone;

	private @Getter Exception caught;

	public void initAction() throws SQLException, ConfigurationException, SenderException, TimeoutException, DbmsException {}
	public void action() throws SQLException, JdbcException, ConfigurationException, SenderException, TimeoutException {}
	public void finalizeAction() throws SQLException {}

	@Override
	public void run() {
		log.info("Starting action tester");
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
