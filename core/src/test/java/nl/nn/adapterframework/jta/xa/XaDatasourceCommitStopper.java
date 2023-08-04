package nl.nn.adapterframework.jta.xa;

import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Semaphore;

public class XaDatasourceCommitStopper extends XaResourceObserver{
	protected Logger log = LogUtil.getLogger(this);

	private static boolean stop;

	public static Semaphore prepareFinished;
	public static Semaphore commitCalled;
	public static Semaphore performCommit;


	public XaDatasourceCommitStopper(XAResource target) {
		super(target);
	}

	public static void stop(boolean stop) {
		if (XaDatasourceCommitStopper.stop && !stop && performCommit != null) {
			while (!performCommit.isReleased()) {
				performCommit.release();
			}
		}
		XaDatasourceCommitStopper.stop = stop;
		if (stop) {
			prepareFinished = new Semaphore();
			commitCalled = new Semaphore();
			performCommit = new Semaphore();
		}
	}

	public static XADataSource augmentXADataSource(XADataSource dataSource) {
		return new XaDatasourceObserver(dataSource, c -> new XaConnectionObserver(c, XaDatasourceCommitStopper::new));
	}

	@Override
	public void commit(Xid xid, boolean onePhase) throws XAException {
		if (stop) {
			try {
				log.warn("commit() waiting 'endless' to perform commit to simulate unresponsive RM");
				commitCalled.release();
				performCommit.acquire();
			} catch (InterruptedException e) {
				throw new XAException(e.getMessage());
			}
		}
		super.commit(xid, onePhase);
	}


	@Override
	public int prepare(Xid xid) throws XAException {
		int result = super.prepare(xid);
		if (prepareFinished!=null) {
			prepareFinished.release();
		}
		return result;
	}


}
