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
package nl.nn.adapterframework.core;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.Locker;

/**
 * extra attributes to do logging and use sessionvariables.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setActive(boolean) active}</td><td>controls whether the Pipe is included in configuration. When set <code>false</code> or set to something else as "true", (even set to the empty string), the Pipe is not included in the configuration</td><td>true</td></tr>
 * <tr><td>{@link #setDurationThreshold(long) durationThreshold}</td><td>if durationThreshold >=0 and the duration (in milliseconds) of the message processing exceeded the value specified the message is logged informatory</td><td>-1</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setGetInputFromFixedValue(String) getInputFromFixedValue}</td><td>when set, this fixed value is taken as input, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPreserveInput(boolean) preserveInput}</td><td>when set <code>true</code>, the input of a pipe is restored before processing the next one</td><td>false</td></tr>
 * </table>
 * </p>
 * 
 * <p>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.scheduler.Locker locker}</td><td>optional: the pipe will only be executed if a lock could be set successfully</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.3
 * @version $Id$
 */
public interface IExtendedPipe extends IPipe {

	public static final String LONG_DURATION_MONITORING_EVENT="Pipe Long Processing Duration";
	public static final String PIPE_EXCEPTION_MONITORING_EVENT="Pipe Exception";
	public static final String MESSAGE_SIZE_MONITORING_EVENT="Pipe Message Size Exceeding";

	/**
	 * Extension, allowing Pipes to register things with the PipeLine at Configuration time.
	 * For IExtendedPipes, PileLine will call this method rather then the no-args configure().
	 */
	void configure(PipeLine pipeline) throws ConfigurationException;

	/**
	 * controls whether pipe is used in configuration. Can be used for debug-only pipes.
	 */
	public boolean isActive();

	/**
	 * Sets a threshold for the duration of message execution; 
	 * If the threshold is exceeded, the message is logged to be analyzed.
	 */
	public void setDurationThreshold(long maxDuration) ;
	public long getDurationThreshold();



	public void setGetInputFromSessionKey(String string);
	public String getGetInputFromSessionKey();

	public void setStoreResultInSessionKey(String string);
	public String getStoreResultInSessionKey();

	public void setGetInputFromFixedValue(String string);
	public String getGetInputFromFixedValue();

	public void setPreserveInput(boolean preserveInput);
	public boolean isPreserveInput();

	public void setLocker(Locker locker);
	public Locker getLocker();

	/**
	 * Register an event for flexible monitoring.
	 * @param description
	 */
	public void registerEvent(String description);
	/**
	 * Throw an event for flexible monitoring.
	 * @param description
	 */
	public void throwEvent(String event);

	public boolean hasSizeStatistics();

}
