/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020 WeAreFrank!

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

import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.Locker;

/**
 * extra attributes to do logging and use sessionvariables.
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
	 * Must be set before calling configure()
	 */
	void setPipeLine(PipeLine pipeline);

	/** If set, input is taken from this session key, instead of regular input */
	public void setGetInputFromSessionKey(String string);
	public String getGetInputFromSessionKey();

	/** If set, this fixed value is taken as input, instead of regular input */
	public void setGetInputFromFixedValue(String string);
	public String getGetInputFromFixedValue();

	/** If set and the input is empty, this fixed value is taken as input */
	public void setEmptyInputReplacement(String string);
	public String getEmptyInputReplacement();

	/** If set <code>true</code>, the result of the pipe is replaced with the original input (i.e. the input before configured replacements of <code>getInputFromSessionKey</code>, <code>getInputFromFixedValue</code> or <code>emptyInputReplacement</code>) */
	public void setPreserveInput(boolean preserveInput);
	public boolean isPreserveInput();

	/** If set, the result (before replacing when <code>true</code>) is stored under this session key */
	public void setStoreResultInSessionKey(String string);
	public String getStoreResultInSessionKey();

	/** If set (>=0) and the character data length inside a xml element exceeds this size, the character data is chomped (with a clear comment) */
	public void setChompCharSize(String string);
	public String getChompCharSize();

	/** If set, the character data in this element is stored under a session key and in the message replaced by a reference to this session key: {sessionkey: + <code>elementToMoveSessionKey</code> + } */
	public void setElementToMove(String string);
	public String getElementToMove();

	/** (Only used when <code>elementToMove</code> is set) Name of the session key under which the character data is stored
	 * @ff.default ref_ + the name of the element
	 */
	public void setElementToMoveSessionKey(String string);
	public String getElementToMoveSessionKey();

	/** Like <code>elementToMove</code> but element is preceded with all ancestor elements and separated by semicolons (e.g. 'adapter;pipeline;pipe') */
	public void setElementToMoveChain(String string);
	public String getElementToMoveChain();

	public void setRemoveCompactMsgNamespaces(boolean b);
	public boolean isRemoveCompactMsgNamespaces();

	/** If set <code>true</code>, compacted messages in the result are restored to their original format (see also  {@link #setElementToMove(java.lang.String)}) */
	public void setRestoreMovedElements(boolean restoreMovedElements);
	public boolean isRestoreMovedElements();

	/** If durationThreshold >=0 and the duration of the message processing exceeded the value specified (in milliseconds) the message is logged informatory to be analyzed
	 * @ff.default -1
	 */
	public void setDurationThreshold(long maxDuration) ;
	public long getDurationThreshold();

	/** 
	 * Optional Locker, to avoid parallel execution of the Pipe by multiple threads or servers. An exception is thrown when the lock cannot be obtained, 
	 * e.g. in case another thread, may be in another server, holds the lock and does not release it in a timely manner.
	 */
	public void setLocker(Locker locker);
	public Locker getLocker();

	public void setWriteToSecLog(boolean b);
	public boolean isWriteToSecLog();

	/** (Only used when <code>writetoseclog=true</code>) Comma separated list of keys of session variables that is appended to the security log record */
	public void setSecLogSessionKeys(String string);
	public String getSecLogSessionKeys();

	/**Register an event for flexible monitoring. */
	public void registerEvent(String description);

	/** Throw an event for flexible monitoring. */
	default void throwEvent(String event) {
		throwEvent(event, null);
	}
	public void throwEvent(String event, Message eventMessage);

	public boolean hasSizeStatistics();

}
