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

	public static int endless = 20; // seconds
	
	public static Semaphore prepareFinished;
	public static Semaphore commitCalled;


	public XaDatasourceCommitStopper(XAResource target) {
		super(target);
	}

	public static void stop(boolean stop) {
		XaDatasourceCommitStopper.stop = stop;
		prepareFinished = new Semaphore();
		commitCalled = new Semaphore();
	}

	public static XADataSource augmentXADataSource(XADataSource dataSource) {
		return new XaDatasourceObserver(dataSource, c -> new XaConnectionObserver(c,r -> new XaDatasourceCommitStopper(r)));
	}

	@Override
	public void commit(Xid xid, boolean onePhase) throws XAException {
		if (stop) {
			try {
				log.warn("commit() starting 'endless' sleep to simulate unresponsive RM");
				commitCalled.release();
				Thread.sleep(endless*1000);
			} catch (InterruptedException e) {
				throw new XAException(e.getMessage());
			}
		}
		super.commit(xid, onePhase);
	}


	@Override
	public void rollback(Xid xid) throws XAException {
		if (stop) {
			try {
				log.warn("rollback() starting 'endless' sleep to simulate unresponsive RM");
				Thread.sleep(endless*1000);
			} catch (InterruptedException e) {
				throw new XAException(e.getMessage());
			}
		}
		super.rollback(xid);
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
