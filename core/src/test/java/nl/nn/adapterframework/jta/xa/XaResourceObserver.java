package nl.nn.adapterframework.jta.xa;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.util.LogUtil;

public class XaResourceObserver implements XAResource {
	protected Logger log = LogUtil.getLogger(this);

	private XAResource target;
	
	public XaResourceObserver(XAResource target) {
		this.target = target;
	}

	@Override
	public void commit(Xid xid, boolean onePhase) throws XAException {
		log.debug("commit Xid [{}] onePhase [{}]", xid, onePhase);
		//throw new XAException(XAException.XAER_RMFAIL);
		target.commit(xid, onePhase);
	}

	@Override
	public void end(Xid xid, int flags) throws XAException {
		log.debug("end Xid [{}] flags [{}]", xid, flags);
		target.end(xid, flags);
	}

	@Override
	public void forget(Xid xid) throws XAException {
		log.debug("forget Xid [{}]", xid);
		target.forget(xid);
	}

	@Override
	public int getTransactionTimeout() throws XAException {
		return target.getTransactionTimeout();
	}

	@Override
	public boolean isSameRM(XAResource arg0) throws XAException {
		return target.isSameRM(arg0);
	}

	@Override
	public int prepare(Xid xid) throws XAException {
		log.debug("prepare Xid [{}]", xid);
		return target.prepare(xid);
	}

	@Override
	public Xid[] recover(int flag) throws XAException {
		log.debug("recover flag [{}]", flag);
		return target.recover(flag);
	}

	@Override
	public void rollback(Xid xid) throws XAException {
		log.debug("rollback Xid [{}]", xid);
		target.rollback(xid);
	}

	@Override
	public boolean setTransactionTimeout(int arg0) throws XAException {
		return target.setTransactionTimeout(arg0);
	}

	@Override
	public void start(Xid xid, int arg1) throws XAException {
		log.debug("start Xid [{}]", xid);
		target.start(xid, arg1);
	}

}
