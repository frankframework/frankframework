/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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

import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.pipes.FixedResultPipe;
import nl.nn.adapterframework.stream.Message;

/**
 * A Pipe represents an action to take in a {@link PipeLine}.
 * 
 * @author Johan Verrips
 */
public interface IPipe extends INamedObject, IForwardTarget {
	/**
	 * <code>configure()</code> is called once after the {@link PipeLine} is registered
	 * at the {@link Adapter}. Purpose of this method is to reduce
	 * creating connections to databases etc. in the {@link #doPipe(Message, IPipeLineSession) doPipe()} method.
	 * As much as possible class-instantiating should take place in the
	 * <code>configure()</code> method, to improve performance.
	 */ 
	void configure() throws ConfigurationException;

	/**
	 * This is where the action takes place. Pipes may only throw a PipeRunException,
	 * to be handled by the caller of this object.
	 * Implementations must either consume the message, or pass it on to the next Pipe in the PipeRunResult.
	 * If the result of the Pipe does not depend on the input, like for the {@link FixedResultPipe}, the Pipe
	 * can schedule the input to be closed at session exit, by calling {@link Message#closeOnCloseOf(IPipeLineSession)}
	 * This allows the previous Pipe to release any resources (e.g. connections) that it might have kept open
	 * until the message was consumed. Doing so avoids connections leaking from pools, while it enables
	 * efficient streaming processing of data while it is being read from a stream.
	 */
	PipeRunResult doPipe(Message message, IPipeLineSession session) throws PipeRunException;

	/**
	 * Indicates the maximum number of threads that may call {@link #doPipe(Message, IPipeLineSession) doPipe()} simultaneously.
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
	 * @see PipeLine
	 * @see PipeForward
	 */
	void registerForward(PipeForward forward);

	/**
	 * Perform necessary action to start the pipe. This method is executed
	 * after the {@link #configure()} method, for eacht start and stop command of the
	 * adapter.
	 */
	void start() throws PipeStartException;

	/**
	 * Perform necessary actions to stop the <code>Pipe</code>.<br/>
	 * For instance, closing JMS connections, dbms connections etc.
	 */
	void stop();
}
