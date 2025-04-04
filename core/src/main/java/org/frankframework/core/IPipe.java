/*
   Copyright 2013 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
package org.frankframework.core;

import java.util.Collection;
import java.util.Map;

import org.springframework.context.Lifecycle;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.doc.FrankDocGroup;
import org.frankframework.doc.FrankDocGroupValue;
import org.frankframework.pipes.FixedResultPipe;
import org.frankframework.stream.Message;
import org.frankframework.util.Locker;

/**
 * A Pipe represents an action to take in a {@link PipeLine}.
 *
 * @author Johan Verrips
 *
 * @ff.defaultElement org.frankframework.pipes.SenderPipe
 */
@FrankDocGroup(FrankDocGroupValue.PIPE)
public interface IPipe extends IConfigurable, IForwardTarget, FrankElement, NameAware, Lifecycle {

	String LONG_DURATION_MONITORING_EVENT = "Pipe Long Processing Duration";
	String PIPE_EXCEPTION_MONITORING_EVENT = "Pipe Exception";
	String MESSAGE_SIZE_MONITORING_EVENT = "Pipe Message Size Exceeding";

	/**
	 * This is where the action takes place. Pipes may only throw a PipeRunException,
	 * to be handled by the caller of this object.
	 * Implementations must either consume the message, or pass it on to the next Pipe in the PipeRunResult.
	 * If the result of the Pipe does not depend on the input, like for the {@link FixedResultPipe}, the Pipe
	 * can schedule the input to be closed at session exit, by calling {@link Message#closeOnCloseOf(PipeLineSession)}
	 * This allows the previous Pipe to release any resources (e.g. connections) that it might have kept open
	 * until the message was consumed. Doing so avoids connections leaking from pools, while it enables
	 * efficient streaming processing of data while it is being read from a stream.
	 */
	PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException;

	/**
	 * Indicates the maximum number of threads that may call {@link #doPipe(Message, PipeLineSession) doPipe()} simultaneously.
	 * A value of 0 indicates an unlimited number of threads.
	 * Pipe implementations that are not thread-safe, i.e. where <code>doPipe()</code> may only be
	 * called by one thread at a time, should make sure getMaxThreads always returns a value of 1.
	 */
	int getMaxThreads();

	/**
	 * Get pipe forwards.
	 */
	Map<String, PipeForward> getForwards();

	/**
	 * Register a PipeForward object to this Pipe. Global Forwards are added
	 * by the PipeLine. If a forward is already registered, it logs a warning.
	 *
	 * @throws ConfigurationException If the forward target cannot be registered.
	 * @see PipeLine
	 * @see PipeForward
	 */
	void addForward(PipeForward forward) throws ConfigurationException;

	/**
	 * Perform necessary action to start the pipe. This method is executed
	 * after the {@link #configure()} method, for each start and stop command of the
	 * adapter.
	 */
	void start();

	/**
	 * Perform necessary actions to stop the <code>Pipe</code>.<br/>
	 * For instance, closing JMS connections, DBMS connections etc.
	 */
	void stop();

	/**
	 * returns <code>true</code> if the pipe or one of its children use the named session variable.
	 * Callers can use this to determine if a message needs to be preserved.
	 */
	boolean consumesSessionVariable(String sessionKey);

	/**
	 * Allowing pipe to register things at Configuration time.
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

	boolean sizeStatisticsEnabled();

	/**
	 * Regular expression to mask strings in the log. For example, the regular expression <code>(?&lt;=&lt;password&gt;).*?(?=&lt;/password&gt;)</code>
	 * will replace every character between keys '&lt;password&gt;' and '&lt;/password&gt;'. <b>note:</b> this feature is used at adapter level,
	 * so a {@code hideRegex} set on one pipe affects all pipes in the pipeline (and multiple values in different pipes are combined into a single regex).
	 * The regular expressions are matched against part of the log lines. See {@link org.frankframework.util.StringUtil#hideAll(String, Collection, int)}
	 * with {@code mode = 0} for how regular expressions are matched and replaced.
	 */
	void setHideRegex(String hideRegex);
	String getHideRegex();

	/** when set, the value in AppConstants is overwritten (for this pipe only) */
	void setLogIntermediaryResults(String string);
	String getLogIntermediaryResults();
}
