/*
 * $Log: Timer.java,v $
 * Revision 1.3  2004-03-23 17:08:02  L190409
 * cosmetic changes
 *
 */
package nl.nn.adapterframework.util;

/**
 * Generic timer ('eierwekker') functionality.
 *
 * Calls a {@link TimerListener#onTimeout() onTimeOut()}-event handler when
 * a certain time has passed. 
 * <p>$Id: Timer.java,v 1.3 2004-03-23 17:08:02 L190409 Exp $</p>
 * 
 */
public class Timer {        
	public static final String version="$Id: Timer.java,v 1.3 2004-03-23 17:08:02 L190409 Exp $";
	
    private TimerListener listener_;
    private long millisecs_;
    private Thread thread_;

    private class Runner implements Runnable {
        public Runner() {
        }

        public void run() {
            try {
                Thread.sleep(millisecs_);
                listener_.onTimeout();
            }
            catch (InterruptedException ignore) {
            }
        }
    }
    public Timer(TimerListener listener, long millisecs) {
        listener_ = listener;
        millisecs_ = millisecs;       
    }
    public void reset() {
        stop();
        start();
    }
    public void reset(long milliseconds) {
        millisecs_ = milliseconds;
        reset();
    }
    public void start() {
        try
        {
            thread_ = new Thread(new Runner());
            thread_.start();
        }
        catch (Exception error) {
            error.printStackTrace();
        }
    }
    public void stop() {
        if ((thread_ != null) &&
            (Thread.currentThread() != thread_)) {
            while (thread_.isAlive())
            {
                thread_.interrupt();
                try {
                    Thread.sleep(500);
                }
                catch (InterruptedException ignore) {
                }
            }
        }
    }
}
