/*
 * $Log: RunStateManager.java,v $
 * Revision 1.7  2007-09-05 13:06:07  europe\L190409
 * updated javadoc
 *
 * Revision 1.6  2007/02/12 14:12:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.5  2005/10/27 08:42:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * *** empty log message ***
 *
 */
package nl.nn.adapterframework.util;

import org.apache.log4j.Logger;

/**
 * Utility class to support run-state management.
 * 
 * @version Id
 * @author Gerrit van Brakel
 */
public class RunStateManager implements RunStateEnquirer {
	public static final String version="$RCSfile: RunStateManager.java,v $ $Revision: 1.7 $ $Date: 2007-09-05 13:06:07 $";
	protected Logger log = LogUtil.getLogger(this);

	private RunStateEnum runState = RunStateEnum.STOPPED;

	public synchronized RunStateEnum getRunState() {
		return runState;
	}
	public synchronized boolean isInState(RunStateEnum state) {
		return runState.equals(state);
	}
	public synchronized void setRunState(RunStateEnum newRunState) {
		if (! runState.equals(newRunState)) {
			if (log.isDebugEnabled())
				log.debug("Runstate [" + this + "] set from " + runState + " to " + newRunState);
			runState = newRunState;
			notifyAll();
		}
	}
	/**
	 * Performs a <code>wait()</code> until the object is in the requested state.
	 * @param requestedRunState    the RunStateEnum requested
	 * @throws InterruptedException when interruption occurs
	 */
	public synchronized void waitForRunState(RunStateEnum requestedRunState)
		throws InterruptedException {
		while (!runState.equals(requestedRunState)) {
			wait();
		}
	}
	
	/**
	 * Performs a <code>wait()</code> until the object is in the requested state, or maxWait ms elapsed.
	 * @param requestedRunState    the RunStateEnum requested
	 * @param maxWait              maximum amount of milliseconds to wait.
	 * @throws InterruptedException when interruption occurs
	 */
	public boolean waitForRunState(RunStateEnum state, long maxWait) throws InterruptedException {
		long cts = System.currentTimeMillis();
		RunStateEnum newState = null;
		synchronized(this) {
			while (! (newState = getRunState()).equals(state)) {
				long togo = maxWait - (System.currentTimeMillis() - cts);
				if (togo > 0) {
					wait(togo);
				}
				else {
					break;
				}
			}
		}
		return newState.equals(state);
	}
	
}
