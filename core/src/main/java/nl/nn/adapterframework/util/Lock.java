/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.util;

/**
 * Semaphorer-variant that supports exclusive and 
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
