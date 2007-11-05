/*
 * $Log: ReceiverBase.java,v $
 * Revision 1.55  2007-11-05 10:33:16  europe\M00035F
 * Move interface 'IJmsConfigurator' from package 'configuration' to package 'core' in preparation of renaming it
 *
 * Revision 1.54  2007/10/16 12:40:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved code to ReceiverBaseClassic
 *
 * Revision 1.53  2007/10/10 08:53:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * transactions from JtaUtil
 * make runState externally accessible
 *
 * Revision 1.52  2007/10/08 13:33:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ArrayList to List where possible
 *
 * Revision 1.51  2007/10/04 12:01:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * limit number of error messages written to log
 *
 * Revision 1.50  2007/10/03 08:57:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map
 *
 * Revision 1.49  2007/09/27 12:55:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of monitoring
 *
 * Revision 1.48  2007/09/25 11:34:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added deprecation warning for ibi42compatibility
 *
 * Revision 1.47  2007/09/24 13:05:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed bug in close of errorStorage
 *
 * Revision 1.46  2007/09/12 09:27:06  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added attribute pollInterval
 *
 * Revision 1.45  2007/09/05 13:05:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved copying of context to Misc
 *
 * Revision 1.44  2007/08/27 11:51:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified afterMessageProcessed handling
 * added attribute 'returnedSessionKeys'
 *
 * Revision 1.43  2007/08/10 11:21:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * catch more exceptions
 *
 * Revision 1.42  2007/06/26 12:06:08  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * tuned logging
 *
 * Revision 1.41  2007/06/26 06:56:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * set inProcessStorage type to 'E' if combined with errorStorage
 *
 * Revision 1.40  2007/06/21 07:07:06  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed warnings about not transacted=true
 *
 * Revision 1.39  2007/06/19 12:07:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modifiy retryinterval handling
 *
 * Revision 1.38  2007/06/14 08:49:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * catch less specific types of exception
 *
 * Revision 1.37  2007/06/12 11:24:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected typeSettings of transactional storages
 *
 * Revision 1.36  2007/06/08 12:49:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.35  2007/06/08 12:17:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved error handling
 * introduced retry mechanisme with increasing wait interval
 *
 * Revision 1.34  2007/06/08 07:49:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed error to warning
 *
 * Revision 1.33  2007/06/07 15:22:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made stopping after receiving an exception configurable
 *
 * Revision 1.32  2007/05/23 09:25:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for attribute 'active' on transactional storages
 *
 * Revision 1.31  2007/05/21 12:22:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added setMessageLog()
 *
 * Revision 1.30  2007/05/02 11:37:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added attribute 'active'
 *
 * Revision 1.29  2007/02/12 14:03:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.28  2007/02/05 15:01:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * configure inProcessStorage when it is present, not only when transacted
 *
 * Revision 1.27  2006/12/13 16:30:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added maxRetries to configuration javadoc
 *
 * Revision 1.26  2006/08/24 07:12:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * documented METT tracing event numbers
 *
 * Revision 1.25  2006/06/20 14:10:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added stylesheet attribute
 *
 * Revision 1.24  2006/04/12 16:17:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * retry after failed storing of message in inProcessStorage
 *
 * Revision 1.23  2006/02/20 15:42:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved METT-support to single entry point for tracing
 *
 * Revision 1.22  2006/02/09 07:57:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * METT tracing support
 *
 * Revision 1.21  2005/10/27 08:46:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduced RunStateEnquiries
 *
 * Revision 1.20  2005/10/26 08:52:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * allow for transacted="true" without inProcessStorage, (ohne Gewähr!)
 *
 * Revision 1.19  2005/10/17 11:29:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed nullpointerexception in startRunning
 *
 * Revision 1.18  2005/09/26 11:42:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added fileNameIfStopped attribute and replace from/to processing when stopped
 *
 * Revision 1.17  2005/09/13 15:42:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved handling of non-serializable messages like Poison-messages
 *
 * Revision 1.16  2005/08/08 09:44:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * start transactions if needed and not already started
 *
 * Revision 1.15  2005/07/19 15:27:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified closing procedure
 * added errorStorage
 * modified implementation of transactionalStorage
 * allowed exceptions to bubble up
 * assume rawmessages to be serializable for transacted processing
 * added ibis42compatibility attribute, avoiding exception bubbling
 *
 * Revision 1.14  2005/07/05 12:54:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * allow to set parameters from context for processRequest() methods
 *
 * Revision 1.13  2005/06/02 11:52:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * limited number of actively polling threads to value of attriubte numThreadsPolling
 *
 * Revision 1.12  2005/04/13 12:53:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused imports
 *
 * Revision 1.11  2005/03/31 08:22:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed bug in getIdleStatistics
 *
 * Revision 1.10  2005/03/07 11:04:36  Johan Verrips <johan.verrips@ibissource.org>
 * PipeLineSession became a extension of HashMap, using other iterator
 *
 * Revision 1.9  2005/03/04 08:53:29  Johan Verrips <johan.verrips@ibissource.org>
 * Fixed IndexOutOfBoundException in getProcessStatistics  due to multi threading.
 * Adjusted this too for getIdleStatistics
 *
 * Revision 1.8  2005/02/10 08:17:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * included context dump in debug
 *
 * Revision 1.7  2005/01/13 08:56:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Make threadContext-attributes available in PipeLineSession
 *
 * Revision 1.6  2004/10/12 15:14:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused code
 *
 * Revision 1.5  2004/08/25 09:11:33  unknown <unknown@ibissource.org>
 * Add waitForRunstate with timeout
 *
 * Revision 1.4  2004/08/23 13:10:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated JavaDoc
 *
 * Revision 1.3  2004/08/16 14:09:58  unknown <unknown@ibissource.org>
 * Return returnIfStopped value in case adapter is stopped
 *
 * Revision 1.2  2004/08/09 13:46:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * various changes
 *
 * Revision 1.1  2004/08/03 13:04:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of GenericReceiver
 *
 */
package nl.nn.adapterframework.receivers;

import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.IReceiverStatistics;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.util.TracingEventNumbers;


/**
 * This {@link IReceiver Receiver} may be used as a base-class for developing receivers.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>name of the class, mostly a class that extends this class</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the receiver as known to the adapter</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setActive(boolean) active}</td>  <td>when set <code>false</code> or set to something else as "true", (even set to the empty string), the receiver is not included in the configuration</td><td>true</td></tr>
 * <tr><td>{@link #setNumThreads(int) numThreads}</td><td>the number of threads that may execute a pipeline concurrently (only for pulling listeners)</td><td>1</td></tr>
 * <tr><td>{@link #setNumThreadsPolling(int) numThreadsPolling}</td><td>the number of threads that are activily polling for messages concurrently. '0' means 'limited only by <code>numThreads</code>' (only for pulling listeners)</td><td>1</td></tr>
 * <tr><td>{@link #setStyleSheetName(String) styleSheetName}</td>  <td></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOnError(String) onError}</td><td>one of 'continue' or 'close'. Controls the behaviour of the receiver when it encounters an error sending a reply or receives an exception asynchronously</td><td>continue</td></tr>
 * <tr><td>{@link #setReturnedSessionKeys(String) returnedSessionKeys}</td><td>comma separated list of keys of session variables that should be returned to caller, for correct results as well as for erronous results. (Only for listeners that support it, like JavaListener)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTransacted(boolean) transacted}</td><td>if set to <code>true</code>, messages will be received and processed under transaction control. If processing fails, messages will be sent to the error-sender. (see below)</code></td><td><code>false</code></td></tr>
 * <tr><td>{@link #setMaxRetries(int) maxRetries}</td><td>The number of times a pulling listening attempt is retried after an exception is caught</td><td>3</td></tr>
 * <tr><td>{@link #setPollInterval(int) pollInterval}</td><td>The number of seconds waited after an unsuccesful poll attempt before another poll attempt is made.</td><td>0</td></tr>
 * <tr><td>{@link #setIbis42compatibility(boolean) ibis42compatibility}</td><td>if set to <code>true</code>, the result of a failed processing of a message is a formatted errormessage. Otherwise a listener specific error handling is performed</code></td><td><code>false</code></td></tr>
 * <tr><td>{@link #setBeforeEvent(int) beforeEvent}</td>      <td>METT eventnumber, fired just before a message is processed by this Receiver</td><td>-1 (disabled)</td></tr>
 * <tr><td>{@link #setAfterEvent(int) afterEvent}</td>        <td>METT eventnumber, fired just after message processing by this Receiver is finished</td><td>-1 (disabled)</td></tr>
 * <tr><td>{@link #setExceptionEvent(int) exceptionEvent}</td><td>METT eventnumber, fired when message processing by this Receiver resulted in an exception</td><td>-1 (disabled)</td></tr>
 * </table>
 * </p>
 * <p>
 * <table border="1">
 * <tr><th>nested elements (accessible in descender-classes)</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.IPullingListener listener}</td><td>the listener used to receive messages from</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ITransactionalStorage inProcessStorage}</td><td>mandatory for {@link #setTransacted(boolean) transacted} receivers: place to store messages during processing.</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ITransactionalStorage errorStorage}</td><td>optional for {@link #setTransacted(boolean) transacted} receivers: place to store messages if message processing has gone wrong. If no errorStorage is specified, the inProcessStorage is used for errorStorage</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ISender errorSender}</td><td>optional for {@link #setTransacted(boolean) transacted} receviers: 
 * will be called to store messages that failed to process. If no errorSender is specified, failed messages will remain in inProcessStorage</td></tr>
 * </table>
 * </p>
 * <p><b>Transaction control</b><br>
 * If {@link #setTransacted(boolean) transacted} is set to <code>true</code>, messages will be received and processed under transaction control.
 * This means that after a message has been read and processed and the transaction has ended, one of the following apply:
 * <ul>
 * <table border="1">
 * <tr><th>situation</th><th>input listener</th><th>Pipeline</th><th>inProcess storage</th><th>errorSender</th><th>summary of effect</th></tr>
 * <tr><td>successful</td><td>message read and committed</td><td>message processed</td><td>unchanged</td><td>unchanged</td><td>message processed</td></tr>
 * <tr><td>procesing failed</td><td>message read and committed</td><td>message processing failed and rolled back</td><td>unchanged</td><td>message sent</td><td>message only transferred from listener to errroSender</td></tr>
 * <tr><td>listening failed</td><td>unchanged: listening rolled back</td><td>no processing performed</td><td>unchanged</td><td>unchanged</td><td>no changes, input message remains on input available for listener</td></tr>
 * <tr><td>transfer to inprocess storage failed</td><td>unchanged: listening rolled back</td><td>no processing performed</td><td>unchanged</td><td>unchanged</td><td>no changes, input message remains on input available for listener</td></tr>
 * <tr><td>transfer to errorSender failed</td><td>message read and committed</td><td>message processing failed and rolled back</td><td>message present</td><td>unchanged</td><td>message only transferred from listener to inProcess storage</td></tr>
 * </table> 
 * If the application or the server crashes in the middle of one or more transactions, these transactions 
 * will be recovered and rolled back after the server/application is restarted. Then allways exactly one of 
 * the following applies for any message touched at any time by Ibis by a transacted receiver:
 * <ul>
 * <li>It is processed correctly by the pipeline and removed from the input-queue, 
 *     not present in inProcess storage and not send to the errorSender</li> 
 * <li>It is not processed at all by the pipeline, or processing by the pipeline has been rolled back; 
 *     the message is removed from the input queue and either (one of) still in inProcess storage <i>or</i> sent to the errorSender</li>
 * </ul>
 * </p>
 *
 * <p><b>commit or rollback</b><br>
 * If {@link #setTransacted(boolean) transacted} is set to <code>true</code>, messages will be either committed or rolled back.
 * All message-processing transactions are committed, unless one or more of the following apply:
 * <ul>
 * <li>The PipeLine is transacted and the exitState of the pipeline is not equal to {@link nl.nn.adapterframework.core.PipeLine#setCommitOnState(String) commitOnState} (that defaults to 'success')</li>
 * <li>a PipeRunException or another runtime-exception has been thrown by any Pipe or by the PipeLine</li>
 * <li>the setRollBackOnly() method has been called on the userTransaction (not accessible by Pipes)</li>
 * </ul>
 * </p>
 *
 * @version Id
 * @author     Gerrit van Brakel
 * @since 4.2
 */
public class ReceiverBase extends ReceiverBaseSpring {
}
