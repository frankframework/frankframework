package nl.nn.adapterframework.core;

import nl.nn.adapterframework.configuration.ConfigurationException;

/**
 * A Pipe represents an action to take in a {@link PipeLine}.
 * 
 * @version Id
 *
 * @author Johan Verrips
 */
public interface IPipe extends INamedObject {
	public static final String version="$Id: IPipe.java,v 1.3 2004-03-26 10:42:45 NNVZNL01#L180564 Exp $";

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
PipeRunResult doPipe (Object input, PipeLineSession session) throws PipeRunException;
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
