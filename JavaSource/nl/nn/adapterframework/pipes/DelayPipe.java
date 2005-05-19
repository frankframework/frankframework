/*
 * $Log: DelayPipe.java,v $
 * Revision 1.1  2005-05-19 12:22:40  europe\l180564
 * Initial Version
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;

/**
 * @author L180564 - Johan Verrips
 *
 * Pipe the sleeps for a specified time, which defaults to 5000 msecs.
 * Usefull for testing purposes
 */
public class DelayPipe extends FixedForwardPipe {
	public static final String version="$Id: DelayPipe.java,v 1.1 2005-05-19 12:22:40 europe\l180564 Exp $";

	private long delayTime=5000;
	public PipeRunResult doPipe (Object input, PipeLineSession session) {
		try {
			Thread.sleep(getDelayTime());
		} catch (InterruptedException e) {
			
		}
		return new PipeRunResult(getForward(),input);
	}


	/**
	 * @return
	 */
	public long getDelayTime() {
		return delayTime;
	}

	/**
	 * @param l
	 */
	public void setDelayTime(long l) {
		delayTime = l;
	}

}
