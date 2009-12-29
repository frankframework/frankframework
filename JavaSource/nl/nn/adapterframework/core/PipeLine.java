/*
 * $Log: PipeLine.java,v $
 * Revision 1.86  2009-12-29 14:32:20  L190409
 * modified imports to reflect move of statistics classes to separate package
 *
 * Revision 1.85  2009/12/10 15:32:48  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Show correct sessionKey name in Test Tool for storeResultInSessionKey (getGetInputFromSessionKey() was used)
 *
 * Revision 1.84  2009/12/04 18:23:34  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added ibisDebugger.senderAbort and ibisDebugger.pipeRollback
 *
 * Revision 1.83  2009/11/27 13:38:20  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Expose available session keys at the beginning of the pipeline to the debugger
 *
 * Revision 1.82  2009/11/18 17:28:04  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added senders to IbisDebugger
 *
 * Revision 1.81  2009/11/12 12:36:35  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Pipeline: added attributes messageSizeWarn and messageSizeError
 *
 * Revision 1.80  2009/11/04 08:28:35  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * cosmetic change
 *
 * Revision 1.79  2009/05/06 11:42:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved configuration of validators
 * log result at info-level
 *
 * Revision 1.78  2009/03/17 10:31:59  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * removed concurrentExecute attribute
 *
 * Revision 1.77  2009/03/10 11:15:33  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added configuration warnings facility (in Show configurationStatus)
 *
 * Revision 1.76  2009/02/24 09:45:27  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added configureScheduledJob method
 *
 * Revision 1.75  2009/02/20 12:52:28  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added attribute concurrentExecute
 *
 * Revision 1.74  2008/12/05 09:46:23  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * clarified transaction management logging
 *
 * Revision 1.73  2008/12/05 09:40:59  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * clarified transaction management logging
 *
 * Revision 1.72  2008/12/04 15:49:58  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * clarified transaction management logging
 *
 * Revision 1.71  2008/12/02 13:09:11  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * clarified transaction management logging
 *
 * Revision 1.70  2008/11/27 11:40:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * clarified transaction management logging
 *
 * Revision 1.69  2008/10/10 14:16:06  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added storeOriginalMessageWithoutNamespaces attribute
 *
 * Revision 1.68  2008/09/17 09:45:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * change declared type of Hashtables to Map
 *
 * Revision 1.67  2008/09/08 07:23:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed PipeDescription code
 *
 * Revision 1.66  2008/09/04 12:03:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added getOwner()
 *
 * Revision 1.65  2008/08/12 15:33:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for pipe exception event
 *
 * Revision 1.64  2008/08/07 11:20:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * catch other throwables while configuring pipes
 *
 * Revision 1.63  2008/07/17 16:14:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed signature of getPipeDescriptionProvider
 *
 * Revision 1.62  2008/07/14 17:16:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for debugging
 *
 * Revision 1.61  2008/05/21 08:40:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed pipeline output validation
 *
 * Revision 1.60  2008/02/15 14:05:08  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved logging
 *
 * Revision 1.59  2008/02/06 16:36:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for setting of transaction timeout
 *
 * Revision 1.58  2008/01/11 14:49:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed external pipe and pipeline executors
 *
 * Revision 1.57  2008/01/11 09:07:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * convert transaction management to Spring
 *
 * Revision 1.56  2007/12/27 16:01:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * avoid NPE for null pipeline results
 *
 * Revision 1.55  2007/12/17 08:49:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * input and output validation
 *
 * Revision 1.54  2007/12/10 10:04:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed commitOnState (rollback only, no exceptions)
 * added input/output validation
 *
 * Revision 1.53  2007/11/22 08:42:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * wrap exceptions
 *
 * Revision 1.52  2007/11/21 13:16:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * make commitOnState work
 *
 * Revision 1.51  2007/10/17 09:27:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * restore setting of message and messageId in the pipelinesession
 *
 * Revision 1.50  2007/10/16 07:51:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed argument order in pipelineexecute
 *
 * Revision 1.49  2007/10/10 07:57:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use configurable transactional executors
 *
 * Revision 1.48  2007/10/08 13:29:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ArrayList to List where possible
 *
 * Revision 1.47  2007/09/27 12:53:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved warning
 *
 * Revision 1.46  2007/09/04 07:58:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * clearified exception message
 *
 * Revision 1.45  2007/07/17 15:09:08  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added list of pipes, to access them in order
 *
 * Revision 1.44  2007/06/21 07:05:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * optimized debug-logging
 *
 * Revision 1.43  2007/06/19 12:10:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * remove superfluous exception loggings
 *
 * Revision 1.42  2007/06/08 12:16:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * restored operation of commitOnState
 *
 * Revision 1.41  2007/06/07 15:15:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * set names of isolated threads
 *
 * Revision 1.40  2007/05/02 11:31:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for attribute 'active'
 * added support for attribute getInputFromFixedValue
 *
 * Revision 1.39  2007/05/01 14:08:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of PipeLine-exithandlers
 *
 * Revision 1.38  2007/02/12 13:44:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.37  2007/02/05 14:54:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fix bug in starting isolated thread
 *
 * Revision 1.36  2006/12/21 12:55:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fix bug in isolated call of Pipe
 *
 * Revision 1.35  2006/12/13 16:23:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed NPE
 *
 * Revision 1.34  2006/09/25 09:23:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed bug in PipeRunWrapper
 *
 * Revision 1.33  2006/09/18 14:05:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected transactionAttribute handling for Pipes
 *
 * Revision 1.32  2006/09/18 11:55:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * difference in debug of transactionAttributes of PipeLine and Pipes
 *
 * Revision 1.31  2006/09/14 15:06:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added getPipe() and getPipes()
 *
 * Revision 1.30  2006/09/14 12:12:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.29  2006/08/22 12:51:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * handling of preserveInput attribute of Pipes
 *
 * Revision 1.28  2006/08/22 07:49:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of transaction attribute handling
 *
 * Revision 1.27  2006/02/20 15:42:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved METT-support to single entry point for tracing
 *
 * Revision 1.26  2006/02/09 07:57:22  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * METT tracing support
 *
 * Revision 1.25  2005/11/02 09:06:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected logging
 *
 * Revision 1.24  2005/09/20 13:32:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added check for emtpy-named pipes
 *
 * Revision 1.23  2005/09/08 15:52:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved extra functionality to IExtendedPipe
 *
 * Revision 1.22  2005/09/07 15:27:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * implemented processing for getInputFromSessionKey and storeResultInSessionKey
 *
 * Revision 1.21  2005/09/05 07:06:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * separate logger for durationThreshold
 *
 * Revision 1.20  2005/09/01 08:53:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added logging of messages for which processing exceeds maxDuration
 *
 * Revision 1.19  2005/08/30 15:54:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected typos in logging-statements
 *
 * Revision 1.18  2005/08/25 15:42:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * prohibit defining pipes with the same name
 *
 * Revision 1.17  2005/07/19 12:23:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 * Revision 1.16  2005/07/05 10:49:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved exception handling obtaining usertransaction
 *
 * Revision 1.15  2005/06/13 12:52:22  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * prepare for nested pipelines
 *
 * Revision 1.14  2005/02/10 07:49:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed clearing of pipelinesession a start of pipeline
 *
 * Revision 1.13  2005/01/13 08:55:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Make threadContext-attributes available in PipeLineSession
 *
 * Revision 1.12  2004/08/19 09:07:02  unknown <unknown@ibissource.org>
 * Add not-null validation for message
 *
 * Revision 1.11  2004/07/20 13:04:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Corrected erroneous pipe run statistics (yet another time)
 *
 * Revision 1.10  2004/07/05 09:54:44  Johan Verrips <johan.verrips@ibissource.org>
 * Improved errorhandling mechanism: when a PipeRunException occured, transactions where still committed
 *
 * Revision 1.9  2004/04/28 14:32:42  Johan Verrips <johan.verrips@ibissource.org>
 * Corrected erroneous pipe run statistics
 *
 * Revision 1.8  2004/04/06 12:43:14  Johan Verrips <johan.verrips@ibissource.org>
 * added CommitOnState
 *
 * Revision 1.7  2004/03/31 12:04:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed javadoc
 *
 * Revision 1.6  2004/03/30 07:29:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.5  2004/03/26 10:42:50  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.4  2004/03/24 08:29:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed String 'adapterName' to Adapter 'adapter'
 * enabled XA transactions
 *
 */
package nl.nn.adapterframework.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.debug.IbisDebugger;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.JtaUtil;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.Semaphore;
import nl.nn.adapterframework.util.SpringTxManagerProxy;
import nl.nn.adapterframework.util.TracingUtil;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

/**
 * Processor and keeper of a line of {@link IPipe Pipes}.
 * <br/>
 * Pipelines also generate statics information per Pipe and keep forwards, that are registered
 * at individual pipes during the configure fase.
 * <br/>
 * In the AppConstants there may be a property named "log.logIntermediaryResults" (true/false)
 * which indicates wether the intermediary results (between calling pipes) have to be logged.
 * 
 * * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>name of the class, mostly a class that extends this class</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFirstPipe(String) firstPipe}</td><td>name of the receiver as known to the adapter</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTransacted(boolean) transacted} <i>deprecated</i></td><td>if set to <code>true, messages will be processed under transaction control. (see below)</code></td><td><code>false</code></td></tr>
 * <tr><td>{@link #setCommitOnState(String) commitOnState}</td><td>If the pipelineResult.getState() equals this value, the transaction is committed, otherwise it is rolled back.</td><td><code>success</code></td></tr>
 * <tr><td>{@link #setTransactionAttribute(String) transactionAttribute}</td><td>Defines transaction and isolation behaviour. Equal to <A href="http://java.sun.com/j2ee/sdk_1.2.1/techdocs/guides/ejb/html/Transaction2.html#10494">EJB transaction attribute</a>. Possible values are: 
 *   <table border="1">
 *   <tr><th>transactionAttribute</th><th>callers Transaction</th><th>Pipeline excecuted in Transaction</th></tr>
 *   <tr><td colspan="1" rowspan="2">Required</td>    <td>none</td><td>T2</td></tr>
 * 											      <tr><td>T1</td>  <td>T1</td></tr>
 *   <tr><td colspan="1" rowspan="2">RequiresNew</td> <td>none</td><td>T2</td></tr>
 * 											      <tr><td>T1</td>  <td>T2</td></tr>
 *   <tr><td colspan="1" rowspan="2">Mandatory</td>   <td>none</td><td>error</td></tr>
 * 											      <tr><td>T1</td>  <td>T1</td></tr>
 *   <tr><td colspan="1" rowspan="2">NotSupported</td><td>none</td><td>none</td></tr>
 * 											      <tr><td>T1</td>  <td>none</td></tr>
 *   <tr><td colspan="1" rowspan="2">Supports</td>    <td>none</td><td>none</td></tr>
 * 											      <tr><td>T1</td>  <td>T1</td></tr>
 *   <tr><td colspan="1" rowspan="2">Never</td>       <td>none</td><td>none</td></tr>
 * 											      <tr><td>T1</td>  <td>error</td></tr>
 *  </table></td><td>Supports</td></tr>
 * <tr><td>{@link #setTransactionTimeout(int) transactionTimeout}</td><td>Timeout (in seconds) of transaction started to process a message.</td><td><code>0</code> (use system default)</code></td></tr>
 * <tr><td>{@link #setStoreOriginalMessageWithoutNamespaces(boolean) storeOriginalMessageWithoutNamespaces}</td><td>when set <code>true</code> the original message without namespaces (and prefixes) is stored under the session key originalMessageWithoutNamespaces</td><td>false</td></tr>
 * <tr><td>{@link #setMessageSizeWarn(String) messageSizeWarn}</td><td>if messageSizeWarn>=0 and the size of the input or result pipe message exceeds the value specified a warning message is logged</td><td>application default (1MB)</td></tr>
 * <tr><td>{@link #setMessageSizeError(String) messageSizeError}</td><td>if messageSizeError>=0 and the size of the input or result pipe message exceeds the value specified an error message is logged</td><td>application default (10MB)</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>&lt;exits&gt; one or more {@link nl.nn.adapterframework.core.PipeLineExit exits}&lt;/exits&gt;</td><td>specifications of exit-paths, in the form &lt;exit path="<i>forwardname</i>" state="<i>statename</i>"/&gt;</td></tr>
 * <tr><td>&lt;inputValidator&gt;</td><td>specification of Pipe to validate input messages</td></tr>
 * <tr><td>&lt;outputValidator&gt;</td><td>specification of Pipe to validate output messages</td></tr>
 * </table>
 * </p>
 *
 * <p><b>Transaction control</b><br>
 * THE FOLLOWING TO BE UPDATED, attribute 'transacted' replaced by 'transactionAttribute' 
 * 
 * If {@link #setTransacted(boolean) transacted} is set to <code>true</code>, messages will be processed 
 * under transaction control. Processing by XA-compliant pipes (i.e. Pipes that implement the 
 * IXAEnabled-interface, set their transacted-attribute to <code>true</code> and use XA-compliant 
 * resources) will then either be commited or rolled back in one transaction.
 * 
 * If {@link #setTransacted(boolean) transacted} is set to <code>true</code>, either an existing transaction
 * (started by a transactional receiver) is joined, or new one is created (if the messsage processing request
 * is not initated by a receiver under transaction control.  
 * Messages are only committed or rolled back by the Pipeline if it started the transaction itself. If
 * the pipeline joined an exisiting transaction, the commit or rollback is left to the object that started 
 * the transaction, i.e. the receiver. In the latter case the pipeline can indicate to the receiver that the 
 * transaction should be rolled back (by calling UserTransaction.setRollBackOnly()). 
 * 
 * The choice whether to either commit (by Pipeline or Receiver) or rollback (by Pipeline or Receiver) 
 * is made as follows:
 * 
 * If the processing of the message concluded without exceptions and the status of the transaction is
 * STATUS_ACTIVE (i.e. normal) the transaction will be committed. Otherwise it will be rolled back,
 * or marked for roll back by the calling party.
 
 * </p>
 * 
 * @version Id
 * @author  Johan Verrips
 */
public class PipeLine {
	public static final String version = "$RCSfile: PipeLine.java,v $ $Revision: 1.86 $ $Date: 2009-12-29 14:32:20 $";
    private Logger log = LogUtil.getLogger(this);
	private Logger durationLog = LogUtil.getLogger("LongDurationMessages");
    
	private IbisDebugger ibisDebugger;

	private Adapter adapter;    // for transaction managing
	private INamedObject owner; // for logging purposes

    private Map pipeStatistics = new Hashtable();
    private Map pipeWaitingStatistics = new Hashtable();
    private Map globalForwards = new Hashtable();
    private String firstPipe;
	private int transactionAttribute=TransactionDefinition.PROPAGATION_SUPPORTS;
	private int transactionTimeout=0;
     
	private IPipe inputValidator=null;
	private IPipe outputValidator=null;
     
	private TransactionDefinition txDef=null;
	private PlatformTransactionManager txManager;
    
    private Map pipesByName=new Hashtable();
    private List pipes=new ArrayList();
    // set of exits paths with their state
    private Map pipeLineExits=new Hashtable();
	private Map pipeThreadCounts=new Hashtable();
	
	private String commitOnState="success"; // exit state on which receiver will commit XA transactions
	private boolean storeOriginalMessageWithoutNamespaces=false;
	private long messageSizeWarn=Misc.getMessageSizeWarnByDefault();
	private long messageSizeError=Misc.getMessageSizeErrorByDefault();

	private List exitHandlers = new ArrayList();
    
	/**
	 * Register an Pipe at this pipeline.
	 * The name is also put in the globalForwards table (with 
	 * forward-name=pipename and forward-path=pipename, so that
	 * pipe can look for a specific pipe-name. If already a globalForward
	 * exists under that name, the pipe is NOT added, allowing globalForwards
	 * to prevail.
	 * @see nl.nn.adapterframework.pipes.AbstractPipe
	 **/
	public void addPipe(IPipe pipe) throws ConfigurationException {
		if (pipe==null) {
			throw new ConfigurationException("pipe to be added is null, pipelineTable size ["+pipesByName.size()+"]");
		}
		if (pipe instanceof IExtendedPipe && !((IExtendedPipe)pipe).isActive()) {
			log.debug("Pipe [" + pipe.getName() + "] is not active, therefore not included in configuration");
			return;
		} 
		String name=pipe.getName();
		if (StringUtils.isEmpty(name)) {
			throw new ConfigurationException("pipe ["+pipe.getClass().getName()+"] to be added has no name, pipelineTable size ["+pipesByName.size()+"]");
		}
		IPipe current=getPipe(name);
		if (current!=null) {
			throw new ConfigurationException("pipe ["+name+"] defined more then once");
		}
	    pipesByName.put(name, pipe);
	    pipes.add(pipe);
	    pipeStatistics.put(name, new StatisticsKeeper(name));
	    if (pipe.getMaxThreads() > 0) {
	        pipeWaitingStatistics.put(name, new StatisticsKeeper(name));
	    }
	    log.debug("added pipe [" + pipe.toString() + "]");
	    if (globalForwards.get(name) == null) {
	        PipeForward pw = new PipeForward();
	        pw.setName(name);
	        pw.setPath(name);
	        registerForward(pw);
	    } else {
	        log.info("already had a pipeForward with name ["+ name+ "] skipping the implicit one to Pipe ["+ pipe.getName()+ "]");
	    }
	}
	
	public IPipe getPipe(String pipeName) {
		return (IPipe)pipesByName.get(pipeName);
	}
	public IPipe getPipe(int index) {
		return (IPipe)pipes.get(index);
	}

	public List getPipes() {
		return pipes;
	}

	public void registerExitHandler(IPipeLineExitHandler exitHandler) {
		exitHandlers.add(exitHandler);
		log.info("registered exithandler ["+exitHandler.getName()+"]");
	}
	
	/**
	 * Configures the pipes of this Pipeline and does some basic checks. It also
	 * registers the <code>PipeLineSession</code> object at the pipes.
	 * @see IPipe
	 */
	public void configurePipes() throws ConfigurationException {
		INamedObject owner = getOwner();
		IAdapter adapter=null;
		if (owner instanceof IAdapter) {
			adapter=(IAdapter)owner;
		}
		for (int i=0; i<pipes.size(); i++) {
			IPipe pipe=getPipe(i);

			String pipeName=pipe.getName();
			log.debug("Pipeline of ["+owner.getName()+"] configuring Pipe ["+pipeName+"]");
			// register the global forwards at the Pipes
			// the pipe will take care that if a local, pipe-specific
			// forward is defined, it is not overwritten by the globals
			Iterator globalForwardNames=globalForwards.keySet().iterator();
			while (globalForwardNames.hasNext()) {
				String gfName=(String)globalForwardNames.next();
				PipeForward pipeForward= (PipeForward) globalForwards.get(gfName);
				pipe.registerForward(pipeForward);
			}
			try {
				if (pipe instanceof IExtendedPipe) {
					IExtendedPipe epipe=(IExtendedPipe)pipe;
					epipe.configure(this);
					if (epipe.getDurationThreshold() >= 0) {
						epipe.registerEvent(IExtendedPipe.LONG_DURATION_MONITORING_EVENT);
					}
					epipe.registerEvent(IExtendedPipe.PIPE_EXCEPTION_MONITORING_EVENT);
					if (getMessageSizeErrorNum() >= 0) {
						epipe.registerEvent(IExtendedPipe.MESSAGE_SIZE_MONITORING_EVENT);
					}
				} else {
					pipe.configure();
				}
			} catch (Throwable t) {
				if (t instanceof ConfigurationException) {
					throw (ConfigurationException)t;
				} else {
					throw new ConfigurationException("Exception configuring Pipe ["+pipeName+"]",t);
				}
			}
			if (log.isDebugEnabled()) log.debug("Pipeline of ["+owner.getName()+"]: Pipe ["+pipeName+"] successfully configured: ["+pipe.toString()+"]");
			
		}
	    if (pipeLineExits.size()<1) {
		    throw new ConfigurationException("no PipeLine Exits specified");
	    }
	    if (this.firstPipe==null) {
		    throw new ConfigurationException("no firstPipe defined");
	    }
	    if (getPipe(firstPipe)==null) {
		    throw new ConfigurationException("no pipe found for firstPipe ["+firstPipe+"]");
	    }

		if (getInputValidator()!=null) {
			log.debug("Pipeline of ["+owner.getName()+"] configuring InputValidator");
			PipeForward pf = new PipeForward();
			pf.setName("success");
			getInputValidator().registerForward(pf);
			getInputValidator().setName("inputValidator of "+owner.getName());
			if (getInputValidator() instanceof IExtendedPipe) {
				((IExtendedPipe)getInputValidator()).configure(this);
			} else {
				getInputValidator().configure();
			}
		}
		if (getOutputValidator()!=null) {
			log.debug("Pipeline of ["+owner.getName()+"] configuring OutputValidator");
			PipeForward pf = new PipeForward();
			pf.setName("success");
			getOutputValidator().registerForward(pf);
			getOutputValidator().setName("outputValidator of "+owner.getName());
			if (adapter!=null && getOutputValidator() instanceof IExtendedPipe) {
				((IExtendedPipe)getOutputValidator()).configure(this);
			} else {
				getOutputValidator().configure();
			}
		}

		int txOption = this.getTransactionAttributeNum();
		if (log.isDebugEnabled()) log.debug("creating TransactionDefinition for transactionAttribute ["+getTransactionAttribute()+"], timeout ["+getTransactionTimeout()+"]");
		txDef = SpringTxManagerProxy.getTransactionDefinition(txOption,getTransactionTimeout());
		log.debug("Pipeline of ["+owner.getName()+"] successfully configured");
	}
    /**
     * @return the number of pipes in the pipeline
     */
    public int getPipeLineSize(){
        return pipesByName.size();
    }
    /**
     * @return a Hashtable with in the key the pipenames and in the
     * value a {@link StatisticsKeeper} object with the statistics
     */
	public Map getPipeStatistics(){
		return pipeStatistics;
	}
    /**
     * @return a Hashtable with in the key the pipenames and in the
     * value a {@link StatisticsKeeper} object with the statistics
     */
	public Map getPipeWaitingStatistics(){
		return pipeWaitingStatistics;
	}
	
	private Semaphore getSemaphore(IPipe pipeToRun) {
	    int maxThreads;
	
	    maxThreads = pipeToRun.getMaxThreads();
	    if (maxThreads > 0) {
	
	        Semaphore s;
	
	        synchronized (pipeThreadCounts) {
	            if (pipeThreadCounts.containsKey(pipeToRun)) {
	                s = (Semaphore) pipeThreadCounts.get(pipeToRun);
	            } else {
	                s = new Semaphore(maxThreads);
	                pipeThreadCounts.put(pipeToRun, s);
	            }
	        }
	        return s;
	    }
	    return null;
	
	}

	/**
	 * The <code>process</code> method does the processing of a message.<br/>
	 * It retrieves the first pipe to execute from the <code>firstPipe</code field,
	 * the call results in a PipRunResult, containing the next pipe to activate.
	 * While processing the process method keeps statistics.
	 * @param message The message as received from the Listener
	 * @param messageId A unique id for this message, used for logging purposes.
	 * @return the result of the processing.
	 * @throws PipeRunException when something went wrong in the pipes.
	 */
	public PipeLineResult process(String messageId, String message, PipeLineSession pipeLineSession) throws PipeRunException {
	
		if (pipeLineSession==null) {
			pipeLineSession= new PipeLineSession();
		}
		// reset the PipeLineSession and store the message and its id in the session
		if (messageId==null) {
				messageId=Misc.createSimpleUUID();
				log.error("null value for messageId, setting to ["+messageId+"]");
	
		}
		if (message == null) {
			throw new PipeRunException(null, "Pipeline of adapter ["+ owner.getName()+"] received null message");
		}
		// store message and messageId in the pipeLineSession
		pipeLineSession.set(message, messageId);
        
        try {
            return runPipeLineObeyingTransactionAttribute(
                messageId,
                message,
                pipeLineSession);
        } catch (RuntimeException e) {
            throw new PipeRunException(null, "RuntimeException calling PipeLine with tx attribute ["
                + getTransactionAttribute() + "]", e);
        }
	}
    
    private PipeLineResult runPipeLineObeyingTransactionAttribute(String messageId, String message, PipeLineSession session) throws PipeRunException {
		//TransactionStatus txStatus = txManager.getTransaction(txDef);
		IbisTransaction itx = new IbisTransaction(txManager, txDef, "pipeline of adapter [" + owner.getName() + "]");
		TransactionStatus txStatus = itx.getStatus();
		try {
			return processPipeLine(messageId, message, session, txStatus);
		} catch (Throwable t) {
			log.debug("setting RollBackOnly for pipeline after catching exception");
			txStatus.setRollbackOnly();
			if (log.isDebugEnabled() && ibisDebugger!=null) {
				t = ibisDebugger.pipeLineRollback(this, messageId, t);
			}
			if (t instanceof Error) {
				throw (Error)t;
			} else if (t instanceof RuntimeException) {
				throw (RuntimeException)t;
			} else if (t instanceof PipeRunException) {
				throw (PipeRunException)t;
			} else {
				throw new PipeRunException(null, "Caught unknown checked exception", t);
			}
		} finally {
			//txManager.commit(txStatus);
			itx.commit();
		}
    }
    
	private PipeRunResult runPipeObeyingTransactionAttribute(IPipe pipe, String messageId, Object message, PipeLineSession session) throws PipeRunException {
        int txOption;
        int txTimeout=0;
        if (pipe instanceof HasTransactionAttribute) {
            HasTransactionAttribute taPipe = (HasTransactionAttribute) pipe;
            txOption = taPipe.getTransactionAttributeNum();
            txTimeout= taPipe.getTransactionTimeout();
        } else {
            txOption = TransactionDefinition.PROPAGATION_SUPPORTS;
        }
		//TransactionStatus txStatus = txManager.getTransaction(SpringTxManagerProxy.getTransactionDefinition(txOption,txTimeout));
		IbisTransaction itx = new IbisTransaction(txManager, SpringTxManagerProxy.getTransactionDefinition(txOption,txTimeout), "pipe [" + pipe.getName() + "]");
		TransactionStatus txStatus = itx.getStatus();
		try {
			checkMessageSize(message, pipe, true);
			PipeRunResult prr = pipe.doPipe(message, session);
			Object result = prr.getResult();
			checkMessageSize(result, pipe, false);
			return prr;
		} catch (Throwable t) {
			log.debug("setting RollBackOnly for pipe [" + pipe.getName()+"] after catching exception");
			txStatus.setRollbackOnly();
			if (log.isDebugEnabled() && ibisDebugger!=null) {
				t = ibisDebugger.pipeRollback(this, pipe, messageId, t);
			}
			if (t instanceof Error) {
				throw (Error)t;
			} else if (t instanceof RuntimeException) {
				throw (RuntimeException)t;
			} else if (t instanceof PipeRunException) {
				throw (PipeRunException)t;
			} else {
				throw new PipeRunException(pipe, "Caught unknown checked exception", t);
			}
		} finally {
			//txManager.commit(txStatus);
			itx.commit();
		}
	}

	private void checkMessageSize(Object message, IPipe pipe, boolean input) {
		String logMessage = null;
		if (getMessageSizeErrorNum()>=0) {
			if (message instanceof String) {
				int messageLength = message.toString().length();
				if (messageLength>=getMessageSizeErrorNum()) {
					logMessage = "pipe [" + pipe.getName() + "] of adapter [" + owner.getName() + "], " + (input ? "input" : "result") + " message size [" + Misc.toFileSize(messageLength) + "] exceeds [" + Misc.toFileSize(getMessageSizeErrorNum()) + "]";
					log.error(logMessage);
					if (pipe instanceof IExtendedPipe) {
						IExtendedPipe pe = (IExtendedPipe)pipe;
						pe.throwEvent(IExtendedPipe.MESSAGE_SIZE_MONITORING_EVENT);
					}
				}
			}
		}
		if (logMessage == null) {
			if (getMessageSizeWarnNum()>=0) {
				if (message instanceof String) {
					int messageLength = message.toString().length();
					if (messageLength>=getMessageSizeWarnNum()) {
						logMessage = "pipe [" + pipe.getName() + "] of adapter [" + owner.getName() + "], " + (input ? "input" : "result") + " message size [" + Misc.toFileSize(messageLength) + "] exceeds [" + Misc.toFileSize(getMessageSizeWarnNum()) + "]";
						log.warn(logMessage);
					}
				}
			}
		}
	}

	/**
     * Run a PipeLine, without observing transaction status of the PipeLine.
     * 
     * This method is meant to be executed from the IPipeLineExecutor
     * implementation.
     * 
	 * @param messageId
	 * @param message
	 * @param pipeLineSession
	 * @return
	 * @throws PipeRunException
	 */
    public PipeLineResult processPipeLine(String messageId, String message, PipeLineSession pipeLineSession, TransactionStatus txStatus) throws PipeRunException {
	    // Object is the object that is passed to and returned from Pipes
	    Object object = (Object) message;
		if (log.isDebugEnabled() && ibisDebugger!=null) {
			object = ibisDebugger.pipeLineInput(this, messageId, message);
			TreeSet keys = new TreeSet(pipeLineSession.keySet());
			Iterator iterator = keys.iterator();
			while (iterator.hasNext()) {
				String sessionKey = (String)iterator.next();
				Object sessionValue = pipeLineSession.get(sessionKey);
				sessionValue = ibisDebugger.pipeLineSessionKey(messageId, sessionKey, sessionValue);
				pipeLineSession.put(sessionKey, sessionValue);
			}
		} 
	    Object preservedObject = object;
	    PipeRunResult pipeRunResult;
	    // the PipeLineResult 
		PipeLineResult pipeLineResult=new PipeLineResult();   
	
	
		
	    // ready indicates wether the pipeline processing is complete
	    boolean ready=false;

		// get the first pipe to run
		IPipe pipeToRun = getPipe(firstPipe);

		IPipe inputValidator = getInputValidator();
		if (inputValidator!=null) {
			PipeRunResult validationResult = inputValidator.doPipe(message,pipeLineSession);
			if (validationResult!=null && !validationResult.getPipeForward().getName().equals("success")) {
				PipeForward validationForward=validationResult.getPipeForward();
				if (validationForward.getPath()==null) {
					throw new PipeRunException(pipeToRun,"forward ["+validationForward.getName()+"] of inputValidator has emtpy forward path");
				}	
				log.warn("setting first pipe to ["+validationForward.getPath()+"] due to validation fault");
				pipeToRun = getPipe(validationForward.getPath());
				if (pipeToRun==null) {
					throw new PipeRunException(pipeToRun,"forward ["+validationForward.getName()+"], path ["+validationForward.getPath()+"] does not correspond to a pipe");
				}
			}
		}

		if (isStoreOriginalMessageWithoutNamespaces()) {
			if (XmlUtils.isWellFormed(message)) {
				String removeNamespaces_xslt = XmlUtils.makeRemoveNamespacesXslt(true,true);
				try{
					String xsltResult = null;
					Transformer transformer = XmlUtils.createTransformer(removeNamespaces_xslt);
					xsltResult = XmlUtils.transformXml(transformer, message);
					pipeLineSession.put("originalMessageWithoutNamespaces", xsltResult);
				} catch (IOException e) {
					throw new PipeRunException(pipeToRun,"cannot retrieve removeNamespaces", e);
				} catch (TransformerConfigurationException te) {
					throw new PipeRunException(pipeToRun,"got error creating transformer from removeNamespaces", te);
				} catch (TransformerException te) {
					throw new PipeRunException(pipeToRun,"got error transforming removeNamespaces", te);
				} catch (DomBuilderException te) {
					throw new PipeRunException(pipeToRun,"caught DomBuilderException", te);
				}
			} else {
				log.warn("original message is not well-formed");
				pipeLineSession.put("originalMessageWithoutNamespaces", message);
			}
		}
	
		boolean outputValidated=false;
		try {    
			while (!ready){
				IExtendedPipe pe=null;
			
				if (pipeToRun instanceof IExtendedPipe) {
					pe = (IExtendedPipe)pipeToRun;
				}
	    	
				TracingUtil.beforeEvent(pipeToRun);
				long pipeStartTime= System.currentTimeMillis();
			
				if (log.isDebugEnabled()){  // for performance reasons
					StringBuffer sb=new StringBuffer();
					String ownerName=owner==null?"<null>":owner.getName();
					String pipeToRunName=pipeToRun==null?"<null>":pipeToRun.getName();
					sb.append("Pipeline of adapter ["+ownerName+"] messageId ["+messageId+"] is about to call pipe ["+ pipeToRunName+"]");
					if (ibisDebugger!=null) {
						object = ibisDebugger.pipeInput(this, pipeToRun, messageId, object);
					} 
	
					if (AppConstants.getInstance().getProperty("log.logIntermediaryResults")!=null) {
						if (AppConstants.getInstance().getProperty("log.logIntermediaryResults").equalsIgnoreCase("true")) {
							sb.append(" current result ["+ object +"] ");
						}
					}
					log.info(sb.toString());
				}
	
				// start it
				long pipeDuration = -1;
			
				if (pe!=null) {
					if (StringUtils.isNotEmpty(pe.getGetInputFromSessionKey())) {
						if (log.isDebugEnabled()) log.debug("Pipeline of adapter ["+owner.getName()+"] replacing input for pipe ["+pe.getName()+"] with contents of sessionKey ["+pe.getGetInputFromSessionKey()+"]");
						object=pipeLineSession.get(pe.getGetInputFromSessionKey());
						if (log.isDebugEnabled() && ibisDebugger!=null) object = ibisDebugger.getInputFromSessionKey(messageId, pe.getGetInputFromSessionKey(), object);
					}
					if (StringUtils.isNotEmpty(pe.getGetInputFromFixedValue())) {
						if (log.isDebugEnabled()) log.debug("Pipeline of adapter ["+owner.getName()+"] replacing input for pipe ["+pe.getName()+"] with fixed value ["+pe.getGetInputFromFixedValue()+"]");
						object=pe.getGetInputFromFixedValue();
						if (log.isDebugEnabled() && ibisDebugger!=null) object = ibisDebugger.getInputFromFixedValue(messageId, object);
					}
				}
			
				try {
					Semaphore s = getSemaphore(pipeToRun);
					if (s != null) {
						long waitingDuration = 0;
						try {
							// keep waiting statistics for thread-limited pipes
							long startWaiting = System.currentTimeMillis();
							s.acquire();
							waitingDuration = System.currentTimeMillis() - startWaiting;
	
							StatisticsKeeper sk = (StatisticsKeeper) pipeWaitingStatistics.get(pipeToRun.getName());
							sk.addValue(waitingDuration);
	
							try { 
								pipeRunResult = runPipeObeyingTransactionAttribute(pipeToRun, messageId, object, pipeLineSession);
							} catch (PipeRunException e) {
								throw e;
							} catch (Throwable t) {
								throw new PipeRunException(pipeToRun, "caught exception", t);
							} finally {
								long pipeEndTime = System.currentTimeMillis();
								pipeDuration = pipeEndTime - pipeStartTime - waitingDuration;
		
								sk = (StatisticsKeeper) pipeStatistics.get(pipeToRun.getName());
								sk.addValue(pipeDuration);
							}
						} catch (InterruptedException e) {
							throw new PipeRunException(pipeToRun, "Interrupted waiting for pipe", e);
						} finally { 
							s.release();
						}
					} else { //no restrictions on the maximum number of threads (s==null)
						try {
							pipeRunResult = runPipeObeyingTransactionAttribute(pipeToRun, messageId, object, pipeLineSession);
						} catch (PipeRunException e) {
							throw e;
						} catch (Throwable t) {
							throw new PipeRunException(pipeToRun, "caught exception", t);
						} finally {
							long pipeEndTime = System.currentTimeMillis();
							pipeDuration = pipeEndTime - pipeStartTime;
	
							StatisticsKeeper sk = (StatisticsKeeper) pipeStatistics.get(pipeToRun.getName());
							sk.addValue(pipeDuration);
						}
					}
					if (pe !=null) {
						if (pipeRunResult!=null && StringUtils.isNotEmpty(pe.getStoreResultInSessionKey())) {
							if (log.isDebugEnabled()) log.debug("Pipeline of adapter ["+owner.getName()+"] storing result for pipe ["+pe.getName()+"] under sessionKey ["+pe.getStoreResultInSessionKey()+"]");
							Object result = pipeRunResult.getResult();
							if (log.isDebugEnabled() && ibisDebugger!=null) result = ibisDebugger.storeResultInSessionKey(messageId, pe.getStoreResultInSessionKey(), result);
							pipeLineSession.put(pe.getStoreResultInSessionKey(),result);
						}
						if (pe.isPreserveInput()) {
							pipeRunResult.setResult(preservedObject);
						}
					}
				} catch (PipeRunException pre) {
					TracingUtil.exceptionEvent(pipeToRun);
					if (pe!=null) {
						pe.throwEvent(IExtendedPipe.PIPE_EXCEPTION_MONITORING_EVENT);
					}
					throw pre;
				} catch (RuntimeException re) {
					TracingUtil.exceptionEvent(pipeToRun);
					if (pe!=null) {
						pe.throwEvent(IExtendedPipe.PIPE_EXCEPTION_MONITORING_EVENT);
					}
					throw new PipeRunException(pipeToRun, "Uncaught runtime exception running pipe '"
                            + (pipeToRun==null?"null":pipeToRun.getName()) + "'", re);
				} finally {
					TracingUtil.afterEvent(pipeToRun);
					if (pe!=null) {
						if (pe.getDurationThreshold() >= 0 && pipeDuration > pe.getDurationThreshold()) {
							durationLog.info("Pipe ["+pe.getName()+"] of ["+owner.getName()+"] duration ["+pipeDuration+"] ms exceeds max ["+ pe.getDurationThreshold()+ "], message ["+object+"]");
							pe.throwEvent(IExtendedPipe.LONG_DURATION_MONITORING_EVENT);
						}
					}
				}
	        	        
				if (pipeRunResult==null){
					throw new PipeRunException(pipeToRun, "Pipeline of ["+owner.getName()+"] received null result from pipe ["+pipeToRun.getName()+"]d");
				}
				object=pipeRunResult.getResult();
				if (log.isDebugEnabled() && ibisDebugger!=null) object = ibisDebugger.pipeOutput(this, pipeToRun, messageId, object);
				preservedObject=object;
				PipeForward pipeForward=pipeRunResult.getPipeForward();
	
	                
				if (pipeForward==null){
					throw new PipeRunException(pipeToRun, "Pipeline of ["+owner.getName()+"] received result from pipe ["+pipeToRun.getName()+"] without a pipeForward");
				}
				// get the next pipe to run
				String nextPath=pipeForward.getPath();
				if ((null==nextPath) || (nextPath.length()==0)){
					throw new PipeRunException(pipeToRun, "Pipeline of ["+owner.getName()+"] got an path that equals null or has a zero-length value from pipe ["+pipeToRun.getName()+"]. Check the configuration, probably forwards are not defined for this pipe.");
				}
	
				PipeLineExit plExit=(PipeLineExit)pipeLineExits.get(nextPath);
				if (null!=plExit){
					IPipe outputValidator = getOutputValidator();
					if (outputValidator !=null && !outputValidated) {
						outputValidated=true;
						log.debug("validating PipeLineResult");
						PipeRunResult validationResult = outputValidator.doPipe(object,pipeLineSession);
						if (validationResult!=null && !validationResult.getPipeForward().getName().equals("success")) {
							PipeForward validationForward=validationResult.getPipeForward();
							if (validationForward.getPath()==null) {
								throw new PipeRunException(pipeToRun,"forward ["+validationForward.getName()+"] of outputValidator has emtpy forward path");
							}	
							log.warn("setting next pipe to ["+validationForward.getPath()+"] due to validation fault");
							pipeToRun = getPipe(validationForward.getPath());
							if (pipeToRun==null) {
								throw new PipeRunException(pipeToRun,"forward ["+validationForward.getName()+"], path ["+validationForward.getPath()+"] does not correspond to a pipe");
							}
						} else {
							log.debug("validation succeeded");
							ready=true;
						}
					} else {
						ready=true;
					}
					if (ready) {
						String state=plExit.getState();
						pipeLineResult.setState(state);
						if (object!=null) {
							pipeLineResult.setResult(object.toString());
						} else { 
							pipeLineResult.setResult(null);
						}
						ready=true;
						if (log.isDebugEnabled()){  // for performance reasons
							log.debug("Pipeline of adapter ["+ owner.getName()+ "] finished processing messageId ["+messageId+"] result: ["+ object.toString()+ "] with exit-state ["+state+"]");
						}
					}
				} else {
					pipeToRun=getPipe(pipeForward.getPath());
					if (pipeToRun==null) {
						throw new PipeRunException(null, "Pipeline of adapter ["+ owner.getName()+"] got an erroneous definition. Pipe to execute ["+pipeForward.getPath()+ "] is not defined.");
					}
				}
			}
		} finally {
			for (int i=0; i<exitHandlers.size(); i++) {
				IPipeLineExitHandler exitHandler = (IPipeLineExitHandler)exitHandlers.get(i);
				try {
					if (log.isDebugEnabled()) log.debug("processing ExitHandler ["+exitHandler.getName()+"]");
					exitHandler.atEndOfPipeLine(messageId,pipeLineResult,pipeLineSession);
				} catch (Throwable t) {
					log.warn("Caught Exception processing ExitHandler ["+exitHandler.getName()+"]",t);
				}
			}
		}
		boolean mustRollback=false;
				
		if (pipeLineResult==null) {
			mustRollback=true;
			log.warn("Pipeline received null result for messageId ["+messageId+"], transaction (when present and active) will be rolled back");
		} else {
			if (StringUtils.isNotEmpty(getCommitOnState()) && !getCommitOnState().equalsIgnoreCase(pipeLineResult.getState())) {
				mustRollback=true;
				log.warn("Pipeline result state ["+pipeLineResult.getState()+"] for messageId ["+messageId+"] is not equal to commitOnState ["+getCommitOnState()+"], transaction (when present and active) will be rolled back");
			}
		}
		if (mustRollback) {
			try {
				txStatus.setRollbackOnly();
			} catch (Exception e) {
				throw new PipeRunException(null,"Could not set RollBackOnly",e);
			}
		}
		if (log.isDebugEnabled() && ibisDebugger!=null) pipeLineResult.setResult(ibisDebugger.pipeLineOutput(this, messageId, pipeLineResult.getResult()));
	    return pipeLineResult;
	}
	
   /**
    * Register global forwards.
    */
   public void registerForward(PipeForward forward){
      globalForwards.put(forward.getName(), forward);
      log.debug("registered global PipeForward "+forward.toString());
    }
    
    public void registerPipeLineExit(PipeLineExit exit) {
	    pipeLineExits.put(exit.getPath(), exit);
    }
    
    /**
     * Register the adapterName of this Pipelineprocessor. 
     * @param adapterName
     */
    public void setAdapter(Adapter adapter) {
        this.adapter = adapter;
        setOwner(adapter);
    }
	public Adapter getAdapter() {
		return adapter;
	}

	public void setOwner(INamedObject owner) {
		this.owner = owner;
	}
	public INamedObject getOwner() {
		return owner;
	}
   /**
    * The indicator for the end of the processing, with default state "undefined".
    * @deprecated since v 3.2 this functionality is superseded by the use of {@link nl.nn.adapterframework.core.PipeLineExit PipeLineExits}.
    * @see PipeLineExit
    */
    public void setEndPath(String endPath){
	    PipeLineExit te=new PipeLineExit();
	    te.setPath(endPath);
	    te.setState("undefined");
		registerPipeLineExit(te);
    }
    /**
     * set the name of the first pipe to execute when a message is to be
     * processed
     * @param pipeName the name of the pipe
     * @see nl.nn.adapterframework.pipes.AbstractPipe
     */
    public void setFirstPipe(String pipeName){
        firstPipe=pipeName;
    }
	public void start() throws PipeStartException {
	    log.info("Pipeline of ["+owner.getName()+"] is starting pipeline");
	
		for (int i=0; i<pipes.size(); i++) {
			IPipe pipe = getPipe(i);
			String pipeName = pipe.getName();
	
			log.debug("Pipeline of ["+owner.getName()+"] starting pipe [" + pipeName+"]");
			pipe.start();
			log.debug("Pipeline of ["+owner.getName()+"] successfully started pipe [" + pipeName + "]");
		}
	    log.info("Pipeline of ["+owner.getName()+"] is successfully started pipeline");
	
	}
	
	/**
	 * Close the pipeline. This will call the <code>stop()</code> method
	 * of all registered <code>Pipes</code>
	 * @see IPipe#stop
	 */
	public void stop() {
	    log.info("Pipeline of ["+owner.getName()+"] is closing pipeline");
		for (int i=0; i<pipes.size(); i++) {
			IPipe pipe = getPipe(i);
			String pipeName = pipe.getName();

			log.debug("Pipeline of ["+owner.getName()+"] is stopping [" + pipeName+"]");
			pipe.stop();
			log.debug("Pipeline of ["+owner.getName()+"] successfully stopped pipe [" + pipeName + "]");
		}
	    log.debug("Pipeline of ["+owner.getName()+"] successfully closed pipeline");
	
	}
	
    /**
     *
     * @return an enumeration of all pipenames in the pipeline and the
     * startpipe and endpath
     * @see #setEndPath
     * @see #setFirstPipe
     */
    public String toString(){
        String result="";
		result+="[ownerName="+(owner==null ? "-none-" : owner.getName())+"]";
        result+="[adapterName="+(adapter==null ? "-none-" : adapter.getName())+"]";
        result+="[startPipe="+firstPipe+"]";
//        result+="[transacted="+transacted+"]";
		result+="[transactionAttribute="+getTransactionAttribute()+"]";
		for (int i=0; i<pipes.size(); i++) {
			result+="pipe"+i+"=["+getPipe(i).getName()+"]";
		}
        Iterator exitKeys=pipeLineExits.keySet().iterator();
        while (exitKeys.hasNext()){
            String exitPath=(String)exitKeys.next();
            PipeLineExit pe=(PipeLineExit)pipeLineExits.get(exitPath);
            result+="[path:"+pe.getPath()+" state:"+pe.getState()+"]";
        }
        return result;
    }


	public void setTransacted(boolean transacted) {
//		this.transacted = transacted;
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		if (transacted) {
			String msg = "implementing setting of transacted=true as transactionAttribute=Required";
			configWarnings.add(log, msg);
			setTransactionAttributeNum(TransactionDefinition.PROPAGATION_REQUIRED);
		} else {
			String msg = "implementing setting of transacted=false as transactionAttribute=Supports";
			configWarnings.add(log, msg);
			setTransactionAttributeNum(TransactionDefinition.PROPAGATION_SUPPORTS);
		}
	}
	/**
	 * the exit state of the pipeline on which the receiver will commit the transaction.
	 */
	public void setCommitOnState(String string) {
		commitOnState = string;
	}
	public String getCommitOnState() {
		return commitOnState;
	}
	
	
	public void setTransactionAttribute(String attribute) throws ConfigurationException {
		transactionAttribute = JtaUtil.getTransactionAttributeNum(attribute);
		if (transactionAttribute<0) {
			throw new ConfigurationException("illegal value for transactionAttribute ["+attribute+"]");
		}
	}
	public String getTransactionAttribute() {
		return JtaUtil.getTransactionAttributeString(transactionAttribute);
	}

	public void setTransactionAttributeNum(int i) {
		transactionAttribute = i;
	}
	public int getTransactionAttributeNum() {
		return transactionAttribute;
	}

		
	public void setInputValidator(IPipe inputValidator) {
		this.inputValidator = inputValidator;
	}
	public IPipe getInputValidator() {
		return inputValidator;
	}

	public void setOutputValidator(IPipe outputValidator) {
		this.outputValidator = outputValidator;
	}
	public IPipe getOutputValidator() {
		return outputValidator;
	}

	public void setTxManager(PlatformTransactionManager txManager) {
		this.txManager = txManager;
	}
	public PlatformTransactionManager getTxManager() {
		return txManager;
	}

	public void setTransactionTimeout(int i) {
		transactionTimeout = i;
	}
	public int getTransactionTimeout() {
		return transactionTimeout;
	}

	public void setIbisDebugger(IbisDebugger ibisDebugger) {
		this.ibisDebugger = ibisDebugger;
	}

	public void setStoreOriginalMessageWithoutNamespaces(boolean b) {
		storeOriginalMessageWithoutNamespaces = b;
	}
	public boolean isStoreOriginalMessageWithoutNamespaces() {
		return storeOriginalMessageWithoutNamespaces;
	}

	/**
	 * The <b>MessageSizeWarn</b> option takes a long
	 * integer in the range 0 - 2^63. You can specify the value
	 * with the suffixes "KB", "MB" or "GB" so that the integer is
	 * interpreted being expressed respectively in kilobytes, megabytes
	 * or gigabytes. For example, the value "10KB" will be interpreted
	 * as 10240.
	 */
	public void setMessageSizeWarn(String s) {
		messageSizeWarn = Misc.toFileSize(s, messageSizeWarn + 1);
	}
	public long getMessageSizeWarnNum() {
		return messageSizeWarn;
	}

	/**
	 * The <b>MessageSizeError</b> option takes a long
	 * integer in the range 0 - 2^63. You can specify the value
	 * with the suffixes "KB", "MB" or "GB" so that the integer is
	 * interpreted being expressed respectively in kilobytes, megabytes
	 * or gigabytes. For example, the value "10KB" will be interpreted
	 * as 10240.
	 */
	public void setMessageSizeError(String s) {
		messageSizeError = Misc.toFileSize(s, messageSizeError + 1);
	}
	public long getMessageSizeErrorNum() {
		return messageSizeError;
	}
}
