package nl.nn.adapterframework.util;

/**
 * Utility class to support run-state management
 * @version Id
 *
 *
 * @author Gerrit van Brakel
 */
public class RunStateManager  {
	public static final String version="$Id: RunStateManager.java,v 1.3 2004-03-26 10:42:39 NNVZNL01#L180564 Exp $";
	
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
