package nl.nn.adapterframework.util;

import nl.nn.adapterframework.core.TimeOutException;

/**
 * Semaphorer-variant that supports exclusive and 
 * @version Id
 * @since   4.9
 * @see     Semaphore
 */
public class Lock {
	
    private int sharedLocks;
	private int exclusiveLocksRequested=0;
	private boolean exclusiveLockGranted=false;
    

    public Lock() {
        this(0);
    }
    public Lock(int i) {
        if (i < 0) throw new IllegalArgumentException(i + " < 0");
		sharedLocks = i;
    }

    /**
     * Decrements internal counter, blocking if the counter is already
     * zero.
     *
     * @exception InterruptedException passed from this.wait().
     */
    public synchronized void acquireShared() throws InterruptedException {
        while (exclusiveLocksRequested>0 || exclusiveLockGranted) {
            this.wait();
        }
		sharedLocks++;
    }
	public synchronized void acquireExclusive() throws InterruptedException {
		exclusiveLocksRequested++;
		try {
			while (sharedLocks>0 || exclusiveLockGranted) {
				this.wait();
			}
			exclusiveLockGranted=true;
		} finally {
			exclusiveLocksRequested--;
		}
	}

//	/**
//	 * Decrements internal counter, blocking if the counter is already
//	 * zero.
//	 *
//	 * @exception InterruptedException passed from this.wait().
//	 * @exception TimeOutException if the time specified has passed, but the counter cannot be decreased.
//	 */
//	public synchronized void acquireShared(long timeout) throws InterruptedException, TimeOutException {
//		if (exclusiveLocksRequested>0 || exclusiveLockGranted) {
//			this.wait(timeout);
//		}
//		if (exclusiveLocksRequested>0 || exclusiveLockGranted) {
//			throw new TimeOutException("Timeout of ["+timeout+"] ms expired");
//		}
//		sharedLocks++;
//	}
//	public synchronized void acquireExclusive(long timeout) throws InterruptedException, TimeOutException {
//		exclusiveLocksRequested++;
//		try {
//			if (sharedLocks>0 || exclusiveLockGranted) {
//				this.wait(timeout);
//			}
//			if (sharedLocks>0 || exclusiveLockGranted) {
//				throw new TimeOutException("Timeout of ["+timeout+"] ms expired");
//			}
//			exclusiveLockGranted=true;
//		} finally {
//			exclusiveLocksRequested--;
//		}
//	}

    /**
     * Increments internal counter, possibly awakening the thread
     * wait()ing in acquire()
     */
    public synchronized void releaseShared() {
		sharedLocks--;
        if (sharedLocks == 0) {
            this.notifyAll();
        }
    }
	public synchronized void releaseExclusive() {
		exclusiveLockGranted=false;
		this.notifyAll();
	}
    
    public synchronized boolean isReleased() {
    	return sharedLocks==0 && !exclusiveLockGranted;
    }
}
