/*
 * $Log: IAdapter.java,v $
 * Revision 1.11  2008-08-27 15:54:21  europe\L190409
 * *** empty log message ***
 *
 * Revision 1.10  2007/10/09 15:33:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * copy changes from Ibis-EJB:
 * removed usertransaction-methods
 * added getErrorState()
 *
 * Revision 1.9  2005/12/28 08:34:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduced StatisticsKeeper-iteration
 *
 * Revision 1.8  2005/07/05 12:28:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added possibility to end processing with an exception
 *
 * Revision 1.7  2005/01/13 08:55:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Make threadContext-attributes available in PipeLineSession
 *
 * Revision 1.6  2004/08/09 08:43:46  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added formatErrorMessage()
 *
 * Revision 1.5  2004/06/16 12:34:46  Johan Verrips <johan.verrips@ibissource.org>
 * Added AutoStart functionality on Adapter
 *
 * Revision 1.4  2004/03/26 10:42:50  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.3  2004/03/23 17:36:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added methods for Transaction control
 *
 */
package nl.nn.adapterframework.core;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.MessageKeeper;
import nl.nn.adapterframework.util.StatisticsKeeperIterationHandler;

import java.util.Iterator;

import javax.transaction.UserTransaction;
/**
 * The Adapter is the central manager in the framework. It has knowledge of both
 * <code>IReceiver</code>s as well as the <code>PipeLine</code> and statistics.
 * The Adapter is the class that is responsible for configuring, initializing and
 * accessing/activating IReceivers, Pipelines, statistics etc.
 * 
 * @version Id
 **/
public interface IAdapter extends IManagable {
	public static final String version = "$RCSfile: IAdapter.java,v $ $Revision: 1.11 $ $Date: 2008-08-27 15:54:21 $";

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
	public PipeLineResult processMessage(String correlationID, String message, PipeLineSession pipeLineSession);
	public PipeLineResult processMessageWithExceptions(String messageId, String message, PipeLineSession pipeLineSession) throws ListenerException;

  	public void registerPipeLine (PipeLine pipeline) throws ConfigurationException;
  	public void setName(String name);
  	public boolean isAutoStart();
	public String toString();
	
	public String formatErrorMessage(
		String errorMessage,
		Throwable t,
		String originalMessage,
		String messageID,
		INamedObject objectInError,
		long receivedTime);
		
	public void forEachStatisticsKeeperBody(StatisticsKeeperIterationHandler hski, Object data, boolean reset);

    /**
     * state to put in PipeLineResult when a PipeRunException occurs.
     */
    String getErrorState();
}
