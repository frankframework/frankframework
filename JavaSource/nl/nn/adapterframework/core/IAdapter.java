/*
 * $Log: IAdapter.java,v $
 * Revision 1.3  2004-03-23 17:36:58  L190409
 * added methods for Transaction control
 *
 */
package nl.nn.adapterframework.core;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.MessageKeeper;

import java.util.Iterator;

import javax.transaction.UserTransaction;
/**
 * The Adapter is the central manager in the framework. It has knowledge of both
 * <code>IReceiver</code>s as well as the <code>PipeLine</code> and statistics.
 * The Adapter is the class that is responsible for configuring, initializing and
 * accessing/activating IReceivers, Pipelines, statistics etc.
 * 
 * <p>$Id: IAdapter.java,v 1.3 2004-03-23 17:36:58 L190409 Exp $</p>
 **/
public interface IAdapter extends IManagable {
	public static final String version="$Id: IAdapter.java,v 1.3 2004-03-23 17:36:58 L190409 Exp $";
  /**
   * Instruct the adapter to configure itself. The adapter will call the
   * pipeline to configure itself, the pipeline will call the individual
   * pipes to configure themselves.
   * @see nl.nn.adapterframework.pipes.AbstractPipe#configure()
   * @see PipeLine#configurePipes()
   */
  public void configure() throws ConfigurationException;
 /**
  * The messagekeeper is used to keep the last x messages, relevant to
  * display in the web-functions.
  */ 
	public MessageKeeper getMessageKeeper();
	public IReceiver getReceiverByName(String receiverName);
	public Iterator getReceiverIterator();
	public PipeLineResult processMessage(String correlationID, String message);
  	public void registerPipeLine (PipeLine pipeline) throws ConfigurationException;
  	public void setName(String name);
	public String toString();
	
	/**
	 *  return the userTransaction object that can be used to demarcate (begin/commit/rollback) transactions
	 */
	public UserTransaction getUserTransaction() throws TransactionException;
	/**
	 *  return true when the current thread is running under a transaction.
	 */
	public boolean inTransaction() throws TransactionException;

}
