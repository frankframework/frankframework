package nl.nn.adapterframework.util;

/**
 * Utility class to support run-state management
 * <p>$Id: RunStateManager.java,v 1.2 2004-02-04 10:02:02 a1909356#db2admin Exp $</p>
 *
 *
 * @author Gerrit van Brakel
 */
public class RunStateManager  {
	public static final String version="$Id: RunStateManager.java,v 1.2 2004-02-04 10:02:02 a1909356#db2admin Exp $";
	
	private RunStateEnum runState = RunStateEnum.STOPPED;

/**
 * Constructor
 */
public RunStateManager() {
	super();
}
public synchronized RunStateEnum getRunState() {
	return runState;
}
	public synchronized boolean isInState(RunStateEnum state) {
		return runState.equals(state);
	}
public synchronized void setRunState(RunStateEnum newRunState) {
	runState = newRunState;
	notifyAll();
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
}
