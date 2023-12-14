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
 * extra attributes to do logging and use session variables.
 *
 * @author Gerrit van Brakel
 * @since 4.3
 */
public interface IExtendedPipe extends IPipe {

	String LONG_DURATION_MONITORING_EVENT = "Pipe Long Processing Duration";
	String PIPE_EXCEPTION_MONITORING_EVENT = "Pipe Exception";
	String MESSAGE_SIZE_MONITORING_EVENT = "Pipe Message Size Exceeding";

	/**
	 * Extension, allowing Pipes to register things with the PipeLine at Configuration time.
	 * Must be set before calling configure()
	 */
	void setPipeLine(PipeLine pipeline);

	/** If set, input is taken from this session key, instead of regular input */
	void setGetInputFromSessionKey(String string);
	String getGetInputFromSessionKey();

	/** If set, this fixed value is taken as input, instead of regular input */
	void setGetInputFromFixedValue(String string);
	String getGetInputFromFixedValue();

	/** If set and the input is empty, this fixed value is taken as input */
	void setEmptyInputReplacement(String string);
	String getEmptyInputReplacement();

	/** If set <code>true</code>, the result of the pipe is replaced with the original input (i.e. the input before configured replacements of <code>getInputFromSessionKey</code>, <code>getInputFromFixedValue</code> or <code>emptyInputReplacement</code>) */
	void setPreserveInput(boolean preserveInput);
	boolean isPreserveInput();

	/** If set, the result (before replacing when <code>true</code>) is stored under this session key */
	void setStoreResultInSessionKey(String string);
	String getStoreResultInSessionKey();

	/** If set (>=0) and the character data length inside a xml element exceeds this size, the character data is chomped (with a clear comment) */
	void setChompCharSize(String string);
	String getChompCharSize();

	/** If set, the character data in this element is stored under a session key and in the message replaced by a reference to this session key: {sessionKey: + <code>elementToMoveSessionKey</code> + } */
	void setElementToMove(String string);
	String getElementToMove();

	/**
	 * (Only used when <code>elementToMove</code> is set) Name of the session key under which the character data is stored
	 *
	 * @ff.default ref_ + the name of the element
	 */
	void setElementToMoveSessionKey(String string);
	String getElementToMoveSessionKey();

	/** Like <code>elementToMove</code> but element is preceded with all ancestor elements and separated by semicolons (e.g. 'adapter;pipeline;pipe') */
	void setElementToMoveChain(String string);
	String getElementToMoveChain();

	void setRemoveCompactMsgNamespaces(boolean b);
	boolean isRemoveCompactMsgNamespaces();

	/** If set <code>true</code>, compacted messages in the result are restored to their original format (see also  {@link #setElementToMove(java.lang.String)}) */
	void setRestoreMovedElements(boolean restoreMovedElements);
	boolean isRestoreMovedElements();

	/** If durationThreshold >=0 and the duration of the message processing exceeded the value specified (in milliseconds) the message is logged informatory to be analyzed
	 * @ff.default -1
	 */
	void setDurationThreshold(long maxDuration);
	long getDurationThreshold();

	/**
	 * Optional Locker, to avoid parallel execution of the Pipe by multiple threads or servers. An exception is thrown when the lock cannot be obtained,
	 * e.g. in case another thread, may be in another server, holds the lock and does not release it in a timely manner.
	 */
	void setLocker(Locker locker);
	Locker getLocker();

	void setWriteToSecLog(boolean b);
	boolean isWriteToSecLog();

	/** (Only used when <code>writetoseclog=true</code>) Comma separated list of keys of session variables that is appended to the security log record */
	void setSecLogSessionKeys(String string);
	String getSecLogSessionKeys();

	/** Register an event for flexible monitoring. */
	void registerEvent(String description);

	/** Throw an event for flexible monitoring. */
	default void throwEvent(String event) {
		throwEvent(event, null);
	}

	void throwEvent(String event, Message eventMessage);

	boolean hasSizeStatistics();

}
