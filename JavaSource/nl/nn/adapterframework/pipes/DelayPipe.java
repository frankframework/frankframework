/*
 * $Log: DelayPipe.java,v $
 * Revision 1.6  2011-11-30 13:51:50  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:45  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.4  2005/12/28 08:36:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.3  2005/07/28 07:39:29  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 * Revision 1.2  2005/05/19 12:33:22  Johan Verrips <johan.verrips@ibissource.org>
 * Updated VersionID
 *
 * Revision 1.1  2005/05/19 12:22:40  Johan Verrips <johan.verrips@ibissource.org>
 * Initial Version
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

/**
 * Pipe that sleeps for a specified time, which defaults to 5000 msecs.
 * Usefull for testing purposes.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDelayTime(long) delayTime}</td><td>the time the thread will be put to sleep</td><td>5000 [ms]</td></tr>
 * </table>
 * 
 * @author L180564 - Johan Verrips
 */
public class DelayPipe extends FixedForwardPipe {
	public static final String version="$RCSfile: DelayPipe.java,v $  $Revision: 1.6 $ $Date: 2011-11-30 13:51:50 $";

	private long delayTime=5000;
	
	public PipeRunResult doPipe (Object input, PipeLineSession session) throws PipeRunException {
		try {
			log.info(getLogPrefix(session)+"starts waiting for " + getDelayTime() + " ms.");
			Thread.sleep(getDelayTime());
		} catch (InterruptedException e) {
			throw new PipeRunException(this, getLogPrefix(session)+"delay interrupted", e);
		}
		log.info(getLogPrefix(session)+"ends waiting for " + getDelayTime() + " ms.");
		return new PipeRunResult(getForward(),input);
	}


	/**
	 * the time the thread will be put to sleep.
	 */
	public void setDelayTime(long l) {
		delayTime = l;
	}
	public long getDelayTime() {
		return delayTime;
	}


}
