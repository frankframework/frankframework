/*
   Copyright 2013, 2016 Nationale-Nederlanden

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
 * <tr><td>{@link #setDurationThreshold(long) durationThreshold}</td><td>if durationThreshold >=0 and the duration (in milliseconds) of the message processing exceeded the value specified the message is logged informatory</td><td>-1</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setGetInputFromFixedValue(String) getInputFromFixedValue}</td><td>when set, this fixed value is taken as input, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setEmptyInputReplacement(String) emptyInputReplacement}</td><td>when set and the input is empty, this fixed value is taken as input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPreserveInput(boolean) preserveInput}</td><td>when set <code>true</code>, the input of a pipe is restored before processing the next one</td><td>false</td></tr>
 * <tr><td>{@link #setRestoreMovedElements(boolean) restoreMovedElements}</td><td>when set <code>true</code>, compacted messages in the result are restored to their original format (see also  {@link nl.nn.adapterframework.receivers.ReceiverBase#setElementToMove(java.lang.String)})</td><td>false</td></tr>
 * <tr><td>{@link #setChompCharSize(String) chompCharSize}</td><td>if set (>=0) and the character data length inside a xml element exceeds this size, the character data is chomped (with a clear comment)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setElementToMove(String) elementToMove}</td><td>if set, the character data in this element is stored under a session key and in the message replaced by a reference to this session key: "{sessionKey:" + <code>elementToMoveSessionKey</code> + "}"</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setElementToMoveSessionKey(String) elementToMoveSessionKey}</td><td>(only used when <code>elementToMove</code> is set) name of the session key under which the character data is stored</td><td>"ref_" + the name of the element</td></tr>
 * <tr><td>{@link #setElementToMoveChain(String) elementToMoveChain}</td><td>like <code>elementToMove</code> but element is preceded with all ancestor elements and separated by semicolons (e.g. "adapter;pipeline;pipe")</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRemoveCompactMsgNamespaces (boolean) removeCompactMsgNamespaces}</td><td>when set <code>true</code>, namespaces (and prefixes) in the compacted message are removed</td><td>true</td></tr>
 * <tr><td>{@link #setWriteToSecLog (boolean) writeToSecLog}</td><td>when set <code>true</code> and property <code>sec.log.enabled=true</code>, a record is written to the security log when the pipe has finished successfully</td><td>false</td></tr>
 * <tr><td>{@link #setSecLogSessionKeys(String) secLogSessionKeys}</td><td>(only used when <code>writeToSecLog=true</code> and property <code>sec.log.enabled=true</code>) comma separated list of keys of session variables that is appended to the security log record</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * <p>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.util.Locker locker}</td><td>optional: the pipe will only be executed if a lock could be set successfully</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @since   4.3
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

	public void setChompCharSize(String string);
	public String getChompCharSize();

	public void setElementToMove(String string);
	public String getElementToMove();

	public void setElementToMoveSessionKey(String string);
	public String getElementToMoveSessionKey();

	public void setElementToMoveChain(String string);
	public String getElementToMoveChain();

	public void setRemoveCompactMsgNamespaces(boolean b);
	public boolean isRemoveCompactMsgNamespaces();
	
	public void setRestoreMovedElements(boolean restoreMovedElements);
	public boolean isRestoreMovedElements();

	public void setLocker(Locker locker);
	public Locker getLocker();

	public void setEmptyInputReplacement(String string);
	public String getEmptyInputReplacement();

	public void setWriteToSecLog(boolean b);
	public boolean isWriteToSecLog();

	public void setSecLogSessionKeys(String string);
	public String getSecLogSessionKeys();

	/**
	 * Register an event for flexible monitoring.
	 * @param description
	 */
	public void registerEvent(String description);
	/**
	 * Throw an event for flexible monitoring.
	 * @param event
	 */
	public void throwEvent(String event);

	public boolean hasSizeStatistics();

}
