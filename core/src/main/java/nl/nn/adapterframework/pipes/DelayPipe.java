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
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
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
	public static final String version="$RCSfile: DelayPipe.java,v $  $Revision: 1.7 $ $Date: 2012-06-01 10:52:49 $";

	private long delayTime=5000;
	
	public PipeRunResult doPipe (Object input, IPipeLineSession session) throws PipeRunException {
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
