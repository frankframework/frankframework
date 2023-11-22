package nl.nn.adapterframework.testutil;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.dbms.DbmsException;
import nl.nn.adapterframework.dbms.JdbcException;

import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Semaphore;

import java.sql.SQLException;

public class ConcurrentActionTester extends Thread {
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
