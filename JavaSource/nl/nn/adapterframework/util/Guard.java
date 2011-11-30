package nl.nn.adapterframework.util;

import nl.nn.adapterframework.core.TimeOutException;

/**
 * A Guard is the counterpart of the {@link Semaphore} that waits till all resources have been released.
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public class Guard {
	
    private int counter;
    
    public Guard() {
        this(0);
    }
    public Guard(int numberOfResourcesToWaitFor) {
        if (numberOfResourcesToWaitFor < 0) throw new IllegalArgumentException(numberOfResourcesToWaitFor + " < 0");
        counter = numberOfResourcesToWaitFor;
    }
    
    /**
	 * Wait for the counter to get zero.
     *
     * @exception InterruptedException passed from this.wait().
     */
    public synchronized void waitForAllResources() throws InterruptedException {
        while (counter != 0) {
            this.wait();
        }
    }

	/**
	 * Wait for the counter to get zero.
	 *
	 * @exception InterruptedException passed from this.wait().
	 * @exception TimeOutException if the time specified has passed, but the counter did not reach zero.
	 */
	public synchronized void waitForAllResources(long timeout) throws InterruptedException, TimeOutException {
		while (counter != 0) {
			this.wait(timeout);
		}
		if (counter!=0) {
			throw new TimeOutException("Timeout of ["+timeout+"] ms expired");
		}
	}

	public synchronized void addResource() {
		counter++;
	}

    /**
     * decrements internal counter, possibly awakening the thread waiting for release
     * wait()ing in acquire()
     */
    public synchronized void releaseResource() {
        counter--;
        if (counter == 0) {
            this.notify();
        }
    }
    
    public synchronized boolean isReleased() {
    	return counter==0;
    }
}
