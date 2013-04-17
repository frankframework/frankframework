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

/**
 * A Pipe represents an action to take in a {@link PipeLine}.
 * 
 * @version $Id$
 * @author Johan Verrips
 */
public interface IPipe extends INamedObject {
/**
 * <code>configure()</code> is called once after the {@link PipeLine} is registered
 * at the {@link Adapter}. Purpose of this method is to reduce
 * creating connections to databases etc. in the {@link #doPipe(Object, PipeLineSession) doPipe()} method.
 * As much as possible class-instantiating should take place in the
 * <code>configure()</code> method, to improve performance.
 */ 
void configure() throws ConfigurationException;

/**
 * This is where the action takes place. Pipes may only throw a PipeRunException,
 * to be handled by the caller of this object.
 */
PipeRunResult doPipe (Object input, IPipeLineSession session) throws PipeRunException;

/**
 * Indicates the maximum number of treads that may call {@link #doPipe(Object, PipeLineSession) doPipe()} simultaneously.
 * A value of 0 indicates an unlimited number of threads.
 * Pipe implementations that are not thread-safe, i.e. where <code>doPipe()</code> may only be
 * called by one thread at a time, should make sure getMaxThreads always returns a value of 1.
 */
int getMaxThreads();

/**
  * Register a PipeForward object to this Pipe. Global Forwards are added
  * by the PipeLine. If a forward is already registered, it logs a warning.
  * @param forward
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
