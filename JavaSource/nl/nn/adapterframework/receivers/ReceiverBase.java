/*
 * $Log: ReceiverBase.java,v $
 * Revision 1.81.2.1  2009-09-09 07:18:27  m168309
 * - fixed support for passing back context parameters, while retaining use of context in afterMessageProcessed()
 * - detailed logging of asynchronously received exceptions
 *
 * Revision 1.81  2009/08/11 07:43:06  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * assign pipelinesession to threadcontext, to enable use of session
 * variables in listener.afterMessageProcessed()
 *
 * Revision 1.80  2009/08/04 11:20:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * set default for name when not specified
 *
 * Revision 1.79  2009/06/05 07:27:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * set pipeline result to formatted errormessage in case of exception
 * added throws clause to iterateOverStatistics()
 *
 * Revision 1.78  2009/04/16 14:01:45  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added hiddenInputSessionKeys attribute
 *
 * Revision 1.77  2009/03/30 12:23:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added counter for messagesRejected
 *
 * Revision 1.76  2009/03/13 14:33:10  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * *** empty log message ***
 *
 * Revision 1.75  2009/02/20 10:18:17  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * an addition to the javadoc
 *
 * Revision 1.74  2008/12/30 17:01:13  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added configuration warnings facility (in Show configurationStatus)
 *
 * Revision 1.73  2008/12/16 15:03:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for transactionAttribute
 *
 * Revision 1.72  2008/12/10 13:32:16  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * bugfix - when resend message from errorLog and an error occurs, the record in errorLog is not updated
 *
 * Revision 1.71  2008/12/05 09:46:23  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * clarified transaction management logging
 *
 * Revision 1.70  2008/12/05 09:40:40  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * clarified transaction management logging
 *
 * Revision 1.69  2008/12/04 15:51:10  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * clarified transaction management logging
 *
 * Revision 1.68  2008/12/04 14:44:08  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * clarified transaction management logging
 *
 * Revision 1.67  2008/12/02 13:10:20  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * clarified transaction management logging
 *
 * Revision 1.66  2008/09/23 12:05:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * send answer to separate sender for errors too
 *
 * Revision 1.65  2008/09/22 13:36:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use CounterStatistics for counters
 * removed redundant names from statistics
 *
 * Revision 1.64  2008/09/08 07:21:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support interval statistics
 *
 * Revision 1.63  2008/09/02 12:15:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * escaped errormessage contents
 *
 * Revision 1.62  2008/08/27 16:20:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified event registration
 * modified delivery count calculation
 * introduced queing statistics
 *
 * Revision 1.61  2008/08/18 13:15:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed another NPE
 *
 * Revision 1.60  2008/08/18 11:20:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * avoid NPE in processRequest
 *
 * Revision 1.59  2008/08/13 17:50:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * perform PipeLineSession.setListenerParameters for processRequest() too
 *
 * Revision 1.58  2008/08/13 13:50:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * no changes
 *
 * Revision 1.57  2008/08/13 13:43:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added numRetries
 * added iterateOverStatistics
 *
 * Revision 1.56  2008/08/07 11:42:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed ReceiverBaseClassic
 * renamed ReceiverBaseSpring into ReceiverBase
 *
 * Revision 1.31  2008/07/24 14:43:08  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * avoid NPE
 *
 * Revision 1.30  2008/07/24 12:23:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fix transactional FXF
 * modified correlation ID calculation, should work with all listeners now
 *
 * Revision 1.29  2008/07/14 17:27:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use flexible monitoring
 *
 * Revision 1.28  2008/06/30 13:42:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * only warn for suspension >1 sec
 *
 * Revision 1.27  2008/06/30 09:08:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * increase max retry interval to 10 minutes
 *
 * Revision 1.26  2008/06/24 07:59:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * only check for duplicates in errorStore when explicitly instructed
 *
 * Revision 1.25  2008/06/19 11:09:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed two harmless messages to critical
 *
 * Revision 1.24  2008/06/19 08:12:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * other message when message seen too many times
 *
 * Revision 1.23  2008/06/18 12:38:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reduced suspension threshold for monitoring event from 10 to 1 minute
 * set default maxRetries to 1
 * modified logging statements
 * put messages with unsuccesful ExitState in errorStorage, for non transacted receivers
 * no retry for non transacted receivers
 *
 * Revision 1.22  2008/05/22 07:27:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * set default poll interval to 10 seconds
 *
 * Revision 1.21  2008/05/21 10:51:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified monitorAdapter interface
 *
 * Revision 1.20  2008/04/17 13:03:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * do not drop messages that cannot be stored in errorStorage
 *
 * Revision 1.19  2008/03/28 14:23:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed 'returnIfStopped' attributes, now just throw exception
 *
 * Revision 1.18  2008/02/28 16:25:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified handling of Business Correlation ID
 *
 * Revision 1.17  2008/02/22 14:33:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added feature to extract correlationId from message
 *
 * Revision 1.16  2008/02/08 09:49:22  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cacheProcessResult for non-transacted too
 *
 * Revision 1.15  2008/02/07 11:47:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unnessecary cast to serializable
 *
 * Revision 1.14  2008/02/06 16:01:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for setting of transaction timeout
 *
 * Revision 1.13  2008/01/29 12:16:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for thread number control
 * call afterMessageProcessed after moving to error store under XA
 *
 * Revision 1.12  2008/01/18 13:49:22  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * transacted: once and only once, move to error in same transaction
 *
 * Revision 1.11  2008/01/17 16:16:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added attribute checkForDuplicates
 *
 * Revision 1.10  2008/01/11 14:54:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added retry function
 *
 * Revision 1.9  2008/01/11 10:22:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected receiver.isOnErrorStop to isOnErrorContinue
 * corrected transaction handling
 * reduced default maxRetries to 2
 *
 * Revision 1.8  2007/12/10 10:20:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added monitoring
 * fixed poisonMessage handling
 *
 * Revision 1.7  2007/11/23 14:18:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * progagate transacted attribute to Jms and Jdbc Listeners
 *
 * Revision 1.6  2007/11/22 13:36:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved logging
 *
 * Revision 1.5  2007/11/05 13:06:55  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Rename and redefine methods in interface IListenerConnector to remove 'jms' from names
 *
 * Revision 1.4  2007/10/23 12:58:23  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Improve changing InProcessStorage to ErrorStorge: Do it before propagating names, and add logging
 *
 * Revision 1.3  2007/10/22 13:24:54  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Restore the ability to use the in-process storage as error-storage.
 *
 * Revision 1.2  2007/10/18 15:56:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added pollInterval attribute
 *
 * Revision 1.1  2007/10/16 13:02:09  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add ReceiverBaseSpring from EJB branch
 *
 * Revision 1.44.2.12  2007/09/28 13:38:13  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Remove unnecessary cast and fix type-name on some JavaDoc
 *
 * Revision 1.44.2.11  2007/09/28 10:50:29  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Updates for more robust and correct transaction handling
 * Update Xerces dependency to modern Xerces
 *
 * Revision 1.44.2.10  2007/09/26 14:59:03  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Updates for more robust and correct transaction handling
 *
 * Revision 1.44.2.9  2007/09/26 06:05:18  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add exception-propagation to new JMS Listener; increase robustness of JMS configuration
 *
 * Revision 1.44.2.8  2007/09/21 14:22:15  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Apply a number of fixes so that the framework starts again
 *
 * Revision 1.44.2.7  2007/09/21 13:48:59  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Enhancement to checking ErrorStorage for known bad messages: internal in-memory cache of bad messages which is checked always, even if there is no ErrorStorage for the receiver.
 * This should help to protect against poison-messages when a Receiver does not have an ErrorStorage.
 *
 * Revision 1.44.2.6  2007/09/21 13:23:34  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * * Add method to ITransactionalStorage to check if original message ID can be found in it
 * * Check for presence of original message id in ErrorStorage before processing, so it can be removed from queue if it has already once been recorded as unprocessable (but the TX in which it ran could no longer be committed).
 *
 * Revision 1.44.2.5  2007/09/21 12:29:34  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Move threaded processing from ReceiverBase into new class, PullingListenerContainer, to get better seperation of concerns.
 *
 * Revision 1.44.2.4  2007/09/21 09:20:34  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * * Remove UserTransaction from Adapter
 * * Remove InProcessStorage; refactor a lot of code in Receiver
 *
 * Revision 1.44.2.3  2007/09/19 14:19:43  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * * More objects from Spring Factory
 * * Fixes for Spring JMS Container
 * * Quartz Scheduler from Spring Factory
 *
 * Revision 1.44.2.2  2007/09/18 11:20:38  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * * Update a number of method-signatures to take a java.util.Map instead of HashMap
 * * Rewrite JmsListener to be instance of IPushingListener; use Spring JMS Container
 *
 * Revision 1.44.2.1  2007/09/13 13:27:17  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * First commit of work to use Spring for creating objects
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
 * allow for transacted="true" without inProcessStorage, (ohne Gew?hr!)
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
 * PipeLineSession became a extension of Map, using other iterator
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import javax.xml.transform.TransformerConfigurationException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IBulkDataListener;
import nl.nn.adapterframework.core.IKnowsDeliveryCount;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPortConnectedListener;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.IPushingListener;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.IReceiverStatistics;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.IThreadCountControllable;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.IbisTransaction;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jdbc.JdbcFacade;
import nl.nn.adapterframework.jdbc.JdbcTransactionalStorage;
import nl.nn.adapterframework.jms.JMSFacade;
import nl.nn.adapterframework.monitoring.EventHandler;
import nl.nn.adapterframework.monitoring.EventThrowing;
import nl.nn.adapterframework.monitoring.MonitorManager;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Counter;
import nl.nn.adapterframework.util.CounterStatistic;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.HasStatistics;
import nl.nn.adapterframework.util.JtaUtil;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.RunStateEnquiring;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.util.RunStateManager;
import nl.nn.adapterframework.util.SpringTxManagerProxy;
import nl.nn.adapterframework.util.StatisticsKeeper;
import nl.nn.adapterframework.util.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.util.TracingEventNumbers;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
 * <tr><td>{@link #setOnError(String) onError}</td><td>one of 'continue' or 'close'. Controls the behaviour of the receiver when it encounters an error sending a reply or receives an exception asynchronously</td><td>continue</td></tr>
 * <tr><td>{@link #setReturnedSessionKeys(String) returnedSessionKeys}</td><td>comma separated list of keys of session variables that should be returned to caller, for correct results as well as for erronous results. (Only for listeners that support it, like JavaListener)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTransacted(boolean) transacted} <i>deprecated</i></td><td>if set to <code>true</code>, messages will be received and processed under transaction control. If processing fails, messages will be sent to the error-sender. (see below)</code></td><td><code>false</code></td></tr>
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
 * <tr><td>{@link #setTransactionTimeout(int) transactionTimeout}</td><td>Timeout (in seconds) of transaction started to receive and process a message.</td><td><code>0</code> (use system default)</code></td></tr>
 * <tr><td>{@link #setMaxRetries(int) maxRetries}</td><td>The number of times a processing attempt is retried after an exception is caught or rollback is experienced (only applicable for transacted receivers)</td><td>1</td></tr>
 * <tr><td>{@link #setCheckForDuplicates(boolean) checkForDuplicates}</td><td>if set to <code>true</code>, each message is checked for presence in the message log. If already present, it is not processed again. (only required for non XA compatible messaging). Requires messagelog!</code></td><td><code>false</code></td></tr>
 * <tr><td>{@link #setPollInterval(int) pollInterval}</td><td>The number of seconds waited after an unsuccesful poll attempt before another poll attempt is made. (only for polling listeners, not for e.g. IFSA, JMS, WebService or JavaListeners)</td><td>10</td></tr>
 * <tr><td>{@link #setIbis42compatibility(boolean) ibis42compatibility}</td><td>if set to <code>true</code>, the result of a failed processing of a message is a formatted errormessage. Otherwise a listener specific error handling is performed</code></td><td><code>false</code></td></tr>
 * <tr><td>{@link #setBeforeEvent(int) beforeEvent}</td>      <td>METT eventnumber, fired just before a message is processed by this Receiver</td><td>-1 (disabled)</td></tr>
 * <tr><td>{@link #setAfterEvent(int) afterEvent}</td>        <td>METT eventnumber, fired just after message processing by this Receiver is finished</td><td>-1 (disabled)</td></tr>
 * <tr><td>{@link #setExceptionEvent(int) exceptionEvent}</td><td>METT eventnumber, fired when message processing by this Receiver resulted in an exception</td><td>-1 (disabled)</td></tr>
 * <tr><td>{@link #setCorrelationIDXPath(String) correlationIDXPath}</td><td>xpath expression to extract correlationID from message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setHiddenInputSessionKeys(String) hiddenInputSessionKeys}</td><td>comma separated list of keys of session variables which are available when the <code>PipeLineSession</code> is created and of which the value will not be shown in the log (replaced by asterisks)</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <p>
 * THE FOLLOWING TO BE UPDATED, attribute 'transacted' replaced by 'transactionAttribute'. 
 * <table border="1">
 * <tr><th>{@link #setTransactionAttribute(String) transactionAttribute}</th><th>{@link #setTransacted(boolean) transacted}</th></tr>
 * <tr><td>Required</td><td>true</td></tr>
 * <tr><td>RequiresNew</td><td>true</td></tr>
 * <tr><td>Mandatory</td><td>true</td></tr>
 * <tr><td>otherwise</td><td>false</td></tr>
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
public class ReceiverBase implements IReceiver, IReceiverStatistics, IMessageHandler, EventThrowing, IbisExceptionListener, HasSender, HasStatistics, TracingEventNumbers, IThreadCountControllable, BeanFactoryAware {
    
	public static final String version="$RCSfile: ReceiverBase.java,v $ $Revision: 1.81.2.1 $ $Date: 2009-09-09 07:18:27 $";
	protected Logger log = LogUtil.getLogger(this);

	public final static TransactionDefinition TXNEW = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	public final static TransactionDefinition TXREQUIRED = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);

	public static final String RCV_CONFIGURED_MONITOR_EVENT = "Receiver Configured";
	public static final String RCV_CONFIGURATIONEXCEPTION_MONITOR_EVENT = "Exception Configuring Receiver";
	public static final String RCV_STARTED_RUNNING_MONITOR_EVENT = "Receiver Started Running";
	public static final String RCV_SHUTDOWN_MONITOR_EVENT = "Receiver Shutdown";
	public static final String RCV_SUSPENDED_MONITOR_EVENT = "Receiver Operation Suspended";
	public static final String RCV_RESUMED_MONITOR_EVENT = "Receiver Operation Resumed";
	public static final String RCV_THREAD_EXIT_MONITOR_EVENT = "Receiver Thread Exited";
	public static final String RCV_MESSAGE_TO_ERRORSTORE_EVENT = "Receiver Moved Message to ErrorStorage";
	
	public static final int RCV_SUSPENSION_MESSAGE_THRESHOLD=60;
	public static final int MAX_RETRY_INTERVAL=600;
	private boolean suspensionMessagePending=false;
   
	private BeanFactory beanFactory;

	private int pollInterval=10;
    
	private String returnedSessionKeys=null;
	private String hiddenInputSessionKeys=null;
	private boolean checkForDuplicates=false;
	private String correlationIDXPath;

	public static final String ONERROR_CONTINUE = "continue";
	public static final String ONERROR_CLOSE = "close";

	private boolean active=true;
	private int transactionTimeout=0;

	private String name;
	private String onError = ONERROR_CONTINUE; 
	protected RunStateManager runState = new RunStateManager();
    
	private boolean ibis42compatibility=false;

	// the number of threads that may execute a pipeline concurrently (only for pulling listeners)
	private int numThreads = 1;
	// the number of threads that are activily polling for messages (concurrently, only for pulling listeners)
	private int numThreadsPolling = 1;
   
	private PullingListenerContainer listenerContainer;
    
	private Counter threadsProcessing = new Counter(0);
	        
	// number of messages received
	private CounterStatistic numReceived = new CounterStatistic(0);
	private CounterStatistic numRetried = new CounterStatistic(0);
	private CounterStatistic numRejected = new CounterStatistic(0);

	private List processStatistics = new ArrayList();
	private List idleStatistics = new ArrayList();
	private List queueingStatistics;

//	private StatisticsKeeper requestSizeStatistics = new StatisticsKeeper("request size");
//	private StatisticsKeeper responseSizeStatistics = new StatisticsKeeper("response size");

	// the adapter that handles the messages and initiates this listener
	private IAdapter adapter;

	private IListener listener;
	private ISender errorSender=null;
	private ITransactionalStorage errorStorage=null;
	// See configure() for explanation on this field
	private ITransactionalStorage tmpInProcessStorage=null;
	private ISender sender=null; // answer-sender
	private ITransactionalStorage messageLog=null;
	
	private int maxRetries=1;
    
	//private boolean transacted=false;
	private int transactionAttribute=TransactionDefinition.PROPAGATION_SUPPORTS;

	private TransformerPool correlationIDTp=null;
 
	// METT event numbers
	private int beforeEvent=-1;
	private int afterEvent=-1;
	private int exceptionEvent=-1;

	int retryInterval=1;
	private int poisonMessageIdCacheSize = 100;
	private int processResultCacheSize = 100;
   
	private PlatformTransactionManager txManager;

	private EventHandler eventHandler=null;
    
	/**
	 * The thread-pool for spawning threads, injected by Spring
	 */
	private TaskExecutor taskExecutor;
    
	/**
	 * Map containing message-ids which are currently being processed.
	 */
	private Map messageRetryCounters = new HashMap();

	/**
	 * The cache for poison messages acts as a sort of poor-mans error
	 * storage and is always available, even if an error-storage is not.
	 * Thus messages might be lost if they cannot be put in the error
	 * storage, but unless the server crashes, a message that has been
	 * put in the poison-cache will not be reprocessed even if it's
	 * offered again.
	 */
	private LinkedHashMap poisonMessageIdCache = new LinkedHashMap() {

		protected boolean removeEldestEntry(Entry eldest) {
			return size() > getPoisonMessageIdCacheSize();
		}
        
	};

	private LinkedHashMap processResultCache = new LinkedHashMap() {

		protected boolean removeEldestEntry(Entry eldest) {
			return size() > getProcessResultCacheSize();
		}
        
	};

	private class ProcessResultCacheItem {
		int tryCount;
		Date receiveDate;
		String correlationId;
		String comments;
	}
    
	private PipeLineSession createProcessingContext(String correlationId, Map threadContext, String messageId) {
		PipeLineSession pipelineSession = new PipeLineSession();
		if (threadContext != null) {
			pipelineSession.putAll(threadContext);
			if (log.isDebugEnabled()) {
				List hiddenSessionKeys = new ArrayList();
				if (getHiddenInputSessionKeys()!=null) {
					StringTokenizer st = new StringTokenizer(getHiddenInputSessionKeys(), " ,;");
					while (st.hasMoreTokens()) {
						String key = st.nextToken();
						hiddenSessionKeys.add(key);
					}
				}

				String contextDump = "PipeLineSession variables for messageId [" + messageId + "] correlationId [" + correlationId + "]:";
				for (Iterator it = pipelineSession.keySet().iterator(); it.hasNext();) {
					String key = (String) it.next();
					Object value = pipelineSession.get(key);
					if (key.equals("messageText")) {
						value = "(... see elsewhere ...)";
					}
					String strValue = String.valueOf(value);
					contextDump += " " + key + "=[" + (hiddenSessionKeys.contains(key)?hide(strValue):strValue) + "]";
				}
				log.debug(getLogPrefix()+contextDump);
			}
		}
		return pipelineSession;
	}

	private String hide(String string) {
		String hiddenString = "";
		for (int i = 0; i < string.toString().length(); i++) {
			hiddenString = hiddenString + "*";
		}
		return hiddenString;
	}

	private void putSessionKeysIntoThreadContext(Map threadContext, PipeLineSession pipelineSession) {
		if (StringUtils.isNotEmpty(getReturnedSessionKeys()) && threadContext != null) {
			if (log.isDebugEnabled()) {
				log.debug(getLogPrefix()+"setting returned session keys [" + getReturnedSessionKeys() + "]");
			}
			StringTokenizer st = new StringTokenizer(getReturnedSessionKeys(), " ,;");
			while (st.hasMoreTokens()) {
				String key = st.nextToken();
				Object value = pipelineSession.get(key);
				if (log.isDebugEnabled()) {
					log.debug(getLogPrefix()+"returning session key [" + key + "] value [" + value + "]");
				}
				threadContext.put(key, value);
			}
		}
	}
    
    
    
	protected String getLogPrefix() {
		return "Receiver ["+getName()+"] "; 
	}	
    
	/** 
	 * sends an informational message to the log and to the messagekeeper of the adapter
	 */
	protected void info(String msg) {
		log.info(msg);
		if (adapter != null)
			adapter.getMessageKeeper().add(msg);
	}

	/** 
	 * sends a warning to the log and to the messagekeeper of the adapter
	 */
	protected void warn(String msg) {
		log.warn(msg);
		if (adapter != null)
			adapter.getMessageKeeper().add("WARNING: " + msg);
	}

	/** 
	 * sends a warning to the log and to the messagekeeper of the adapter
	 */
	protected void error(String msg, Throwable t) {
		log.error(msg, t);
		if (adapter != null)
			adapter.getMessageKeeper().add("ERROR: " + msg+(t!=null?": "+t.getMessage():""));
	}


	protected void openAllResources() throws ListenerException {	
		// on exit resouces must be in a state that runstate is or can be set to 'STARTED'
		try {
			if (getSender()!=null) {
				getSender().open();
			}
			if (getErrorSender()!=null) {
				getErrorSender().open();
			}
			if (getErrorStorage()!=null) {
				getErrorStorage().open();
			}
			if (getMessageLog()!=null) {
				getMessageLog().open();
			}
		} catch (SenderException e) {
			throw new ListenerException(e);
		}
		getListener().open();
		throwEvent(RCV_STARTED_RUNNING_MONITOR_EVENT);
		if (getListener() instanceof IPullingListener){
			// start all threads
			if (getNumThreads() > 1) {
				for (int i = 1; i <= getNumThreads(); i++) {
					addThread("[" + i+"]");
				}
			} else {
				addThread("");
			}
		}
	}

	private void addThread(String nameSuffix) {
		if (getListener() instanceof IPullingListener){
			//Thread t = new Thread(this, getName() + (nameSuffix==null ? "" : nameSuffix));
			//t.start();
			taskExecutor.execute(listenerContainer);
		}
	}


	protected void tellResourcesToStop() throws ListenerException {
		 // must lead to a 'closeAllResources()'
		 // runstate is 'STOPPING'
		 // default just calls 'closeAllResources()'
		 if (getListener() instanceof IPushingListener) {
			closeAllResources();
		 }
		 // IPullingListeners stop as their threads finish, as the runstate is set to stopping
	}
	protected void closeAllResources() {
		// on exit resouces must be in a state that runstate can be set to 'STOPPED'
		try {
			log.debug(getLogPrefix()+"closing");
			getListener().close();
			if (getSender()!=null) {
				getSender().close();
			}
			if (getErrorSender()!=null) {
				getErrorSender().close();
			}
			if (getErrorStorage()!=null) {
				getErrorStorage().close();
			}
			if (getMessageLog()!=null) {
				getMessageLog().close();
			}
	
			log.debug(getLogPrefix()+"closed");
		} catch (Exception e) {
			error(getLogPrefix()+"error closing connection", e);
		}
		runState.setRunState(RunStateEnum.STOPPED);
		throwEvent(RCV_SHUTDOWN_MONITOR_EVENT);
		resetRetryInterval();
		info(getLogPrefix()+"stopped");
	}
	 
	protected void propagateName() {
		IListener listener=getListener();
		if (listener!=null && StringUtils.isEmpty(listener.getName())) {
			listener.setName("listener of ["+getName()+"]");
		}
		ISender errorSender = getErrorSender();
		if (errorSender != null) {
			errorSender.setName("errorSender of ["+getName()+"]");
		}
		ITransactionalStorage errorStorage = getErrorStorage();
		if (errorStorage != null) {
			errorStorage.setName("errorStorage of ["+getName()+"]");
		}
		ISender answerSender = getSender();
		if (answerSender != null) {
			answerSender.setName("answerSender of ["+getName()+"]");
		}
	}

	public void configure() throws ConfigurationException {		
		try {
			if (StringUtils.isEmpty(getName())) {
				if (getListener()!=null) {
					setName(ClassUtils.nameOf(getListener()));
				} else {
					setName(ClassUtils.nameOf(this));
				}
			}
			eventHandler = MonitorManager.getEventHandler();
			registerEvent(RCV_CONFIGURED_MONITOR_EVENT);
			registerEvent(RCV_CONFIGURATIONEXCEPTION_MONITOR_EVENT);
			registerEvent(RCV_STARTED_RUNNING_MONITOR_EVENT);
			registerEvent(RCV_SHUTDOWN_MONITOR_EVENT);
			registerEvent(RCV_SUSPENDED_MONITOR_EVENT);
			registerEvent(RCV_RESUMED_MONITOR_EVENT);
			registerEvent(RCV_THREAD_EXIT_MONITOR_EVENT);
            // Check if we need to use the in-process storage as
            // error-storage.
            // In-process storage is no longer used, but is often
            // still configured to be used as error-storage.
            // The rule is:
            // 1. if error-storage is configured, use it.
            // 2. If error-storage is not configure but an error-sender is,
            //    then use the error-sender.
            // 3. If neither error-storage nor error-sender are configured,
            //    but the in-process storage is, then use the in-process storage
            //    for error-storage.
            // Member variables are accessed directly, to avoid any possible
            // aliasing-effects applied by getter methods. (These have been
            // removed for now, but since the getter-methods were not
            // straightforward in the earlier versions, I felt it was safer
            // to use direct member variables).
            if (this.tmpInProcessStorage != null) {
                if (this.errorSender == null && this.errorStorage == null) {
                    this.errorStorage = this.tmpInProcessStorage;
                    info(getLogPrefix()+"has errorStorage in inProcessStorage, setting inProcessStorage's type to 'errorStorage'. Please update the configuration to change the inProcessStorage element to an errorStorage element, since the inProcessStorage is no longer used.");
                    errorStorage.setType(JdbcTransactionalStorage.TYPE_ERRORSTORAGE);
                } else {
                    info(getLogPrefix()+"has inProcessStorage defined but also has an errorStorage or errorSender. InProcessStorage is not used and can be removed from the configuration.");
                }
                // Set temporary in-process storage pointer to null
                this.tmpInProcessStorage = null;
            }
            
            // Do propagate-name AFTER changing the errorStorage!
			propagateName();
			if (getListener()==null) {
				throw new ConfigurationException(getLogPrefix()+"has no listener");
			}
			if (getListener() instanceof IPushingListener) {
				IPushingListener pl = (IPushingListener)getListener();
				pl.setHandler(this);
				pl.setExceptionListener(this);
			}
            if (getListener() instanceof IPortConnectedListener) {
                IPortConnectedListener pcl = (IPortConnectedListener) getListener();
                pcl.setReceiver(this);
            }
			if (getListener() instanceof IPullingListener) {
				setListenerContainer(createListenerContainer());
			}
			if (getListener() instanceof JdbcFacade) {
				((JdbcFacade)getListener()).setTransacted(isTransacted());
			}
			if (getListener() instanceof JMSFacade) {
				((JMSFacade)getListener()).setTransacted(isTransacted());
			}
			getListener().configure();
			if (getListener() instanceof HasPhysicalDestination) {
				info(getLogPrefix()+"has listener on "+((HasPhysicalDestination)getListener()).getPhysicalDestinationName());
			}
			if (getListener() instanceof HasSender) {
				// only informational
				ISender sender = ((HasSender)getListener()).getSender();
				if (sender instanceof HasPhysicalDestination) {
					info("Listener of receiver ["+getName()+"] has answer-sender on "+((HasPhysicalDestination)sender).getPhysicalDestinationName());
				}
			}
			ISender sender = getSender();
			if (sender!=null) {
				sender.configure();
				if (sender instanceof HasPhysicalDestination) {
					info(getLogPrefix()+"has answer-sender on "+((HasPhysicalDestination)sender).getPhysicalDestinationName());
				}
			}
			
			ISender errorSender = getErrorSender();
			if (errorSender!=null) {
				errorSender.configure();
				if (errorSender instanceof HasPhysicalDestination) {
					info(getLogPrefix()+"has errorSender to "+((HasPhysicalDestination)errorSender).getPhysicalDestinationName());
				}
			}
			ITransactionalStorage errorStorage = getErrorStorage();
			if (errorStorage!=null) {
				errorStorage.configure();
				if (errorStorage instanceof HasPhysicalDestination) {
					info(getLogPrefix()+"has errorStorage to "+((HasPhysicalDestination)errorStorage).getPhysicalDestinationName());
				}
				registerEvent(RCV_MESSAGE_TO_ERRORSTORE_EVENT);
			}
			ITransactionalStorage messageLog = getMessageLog();
			if (messageLog!=null) {
				messageLog.configure();
				if (messageLog instanceof HasPhysicalDestination) {
					info(getLogPrefix()+"has messageLog in "+((HasPhysicalDestination)messageLog).getPhysicalDestinationName());
				}
			}
			if (isTransacted()) {
//				if (!(getListener() instanceof IXAEnabled && ((IXAEnabled)getListener()).isTransacted())) {
//					warn(getLogPrefix()+"sets transacted=true, but listener not. Transactional integrity is not guaranteed"); 
//				}
				
				if (errorSender==null && errorStorage==null) {
					warn(getLogPrefix()+"sets transactionAttribute=" + getTransactionAttribute() + ", but has no errorSender or errorStorage. Messages processed with errors will be lost");
				} else {
//					if (errorSender!=null && !(errorSender instanceof IXAEnabled && ((IXAEnabled)errorSender).isTransacted())) {
//						warn(getLogPrefix()+"sets transacted=true, but errorSender is not. Transactional integrity is not guaranteed"); 
//					}
//					if (errorStorage!=null && !(errorStorage instanceof IXAEnabled && ((IXAEnabled)errorStorage).isTransacted())) {
//						warn(getLogPrefix()+"sets transacted=true, but errorStorage is not. Transactional integrity is not guaranteed"); 
//					}
				}
			} 

			if (StringUtils.isNotEmpty(getCorrelationIDXPath())) {
				try {
					correlationIDTp = new TransformerPool(XmlUtils.createXPathEvaluatorSource(getCorrelationIDXPath()));
				} catch (TransformerConfigurationException e) {
					throw new ConfigurationException(getLogPrefix() + "cannot create transformer for correlationID ["+getCorrelationIDXPath()+"]",e);
				}
			}

			if (adapter != null) {
				adapter.getMessageKeeper().add(getLogPrefix()+"initialization complete");
			}
			throwEvent(RCV_CONFIGURED_MONITOR_EVENT);
		} catch(ConfigurationException e){
			throwEvent(RCV_CONFIGURATIONEXCEPTION_MONITOR_EVENT);
			log.debug(getLogPrefix()+"Errors occured during configuration, setting runstate to ERROR");
			runState.setRunState(RunStateEnum.ERROR);
			throw e;
		}
	}


	public void startRunning() {
		// if this receiver is on an adapter, the StartListening method
		// may only be executed when the adapter is started.
		if (adapter != null) {
			RunStateEnum adapterRunState = adapter.getRunState();
			if (!adapterRunState.equals(RunStateEnum.STARTED)) {
				log.warn(getLogPrefix()+"on adapter ["
						+ adapter.getName()
						+ "] was tried to start, but the adapter is in state ["+adapterRunState+"]. Ignoring command.");
				adapter.getMessageKeeper().add(
					"ignored start command on [" + getName()  + "]; adapter is in state ["+adapterRunState+"]");
				return;
			}
		}
		try {
			String msg=(getLogPrefix()+"starts listening.");
			log.info(msg);
			if (adapter != null) { 
				adapter.getMessageKeeper().add(msg);
			}
			runState.setRunState(RunStateEnum.STARTING);
			openAllResources();
			runState.setRunState(RunStateEnum.STARTED);
            
		} catch (ListenerException e) {
			error(getLogPrefix()+"error occured while starting", e);
			runState.setRunState(RunStateEnum.ERROR);            
        
		}    
	}
	
	public void stopRunning() {

		if (getRunState().equals(RunStateEnum.STOPPED)){
			return;
		}
	
		if (!getRunState().equals(RunStateEnum.ERROR)) { 
			runState.setRunState(RunStateEnum.STOPPING);
			try {
				tellResourcesToStop();
			} catch (ListenerException e) {
				warn("exception stopping receiver: "+e.getMessage());
			}
		}
		else {
			closeAllResources();
			runState.setRunState(RunStateEnum.STOPPED);
		}
		NDC.remove();
	}

	protected void startProcessingMessage(long waitingDuration) {
		synchronized (threadsProcessing) {
			int threadCount = (int) threadsProcessing.getValue();
			
			if (waitingDuration>=0) {
				getIdleStatistics(threadCount).addValue(waitingDuration);
			}
			threadsProcessing.increase();
		}
		log.debug(getLogPrefix()+"starts processing message");
	}

	protected void finishProcessingMessage(long processingDuration) {
		synchronized (threadsProcessing) {
			int threadCount = (int) threadsProcessing.decrease();
			getProcessStatistics(threadCount).addValue(processingDuration);
		}
		log.debug(getLogPrefix()+"finishes processing message");
	}

	private void moveInProcessToError(String originalMessageId, String correlationId, String message, Date receivedDate, String comments, Object rawMessage, TransactionDefinition txDef) {
	
		throwEvent(RCV_MESSAGE_TO_ERRORSTORE_EVENT);
		log.info(getLogPrefix()+"moves message id ["+originalMessageId+"] correlationId ["+correlationId+"] to errorSender/errorStorage");
		cachePoisonMessageId(originalMessageId);
		ISender errorSender = getErrorSender();
		ITransactionalStorage errorStorage = getErrorStorage();
		if (errorSender==null && errorStorage==null) {
			log.warn(getLogPrefix()+"has no errorSender or errorStorage, message with id [" + originalMessageId + "] will be lost");
			return;
		}
		TransactionStatus txStatus = null;
		try {
			txStatus = txManager.getTransaction(txDef);
		} catch (Exception e) {
			log.error(getLogPrefix()+"Exception preparing to move input message with id [" + originalMessageId + "] to error sender", e);
			// no use trying again to send message on errorSender, will cause same exception!
			return;
		}
		try {
			if (errorSender!=null) {
				errorSender.sendMessage(correlationId, message);
			}
			Serializable sobj;
			if (rawMessage instanceof Serializable) {
				sobj=(Serializable)rawMessage;
			} else {
				try {
					sobj = new MessageWrapper(rawMessage, getListener());
				} catch (ListenerException e) {
					log.error(getLogPrefix()+"could not wrap non serializable message for messageId ["+originalMessageId+"]",e);
					sobj=message;
				}
			}
			if (errorStorage!=null) {
				errorStorage.storeMessage(originalMessageId, correlationId, receivedDate, comments, sobj);
			} 
			txManager.commit(txStatus);
		} catch (Exception e) {
			log.error(getLogPrefix()+"Exception moving message with id ["+originalMessageId+"] correlationId ["+correlationId+"] to error sender, original message: ["+message+"]",e);
			try {
				if (!txStatus.isCompleted()) {
					txManager.rollback(txStatus);
				}
			} catch (Exception rbe) {
				log.error(getLogPrefix()+"Exception while rolling back transaction for message  with id ["+originalMessageId+"] correlationId ["+correlationId+"], original message: ["+message+"]", rbe);
			}
		}
	}

	/**
	 * Process the received message with {@link #processRequest(IListener, String, String)}.
	 * A messageId is generated that is unique and consists of the name of this listener and a GUID
	 */
	public String processRequest(IListener origin, String message) throws ListenerException {
		return processRequest(origin, null, message, null, -1);
	}

	public String processRequest(IListener origin, String correlationId, String message)  throws ListenerException{
		return processRequest(origin, correlationId, message, null, -1);
	}

	public String processRequest(IListener origin, String correlationId, String message, Map context) throws ListenerException {
		return processRequest(origin, correlationId, message, context, -1);
	}

	public String processRequest(IListener origin, String correlationId, String message, Map context, long waitingTime) throws ListenerException {
		if (getRunState() != RunStateEnum.STARTED) {
			throw new ListenerException(getLogPrefix()+"is not started");
		}
		Date tsReceived = null;
		Date tsSent = null;
		if (context!=null) {
			tsReceived = (Date)context.get(PipeLineSession.tsReceivedKey);
			tsSent = (Date)context.get(PipeLineSession.tsSentKey);
		} else {
			context=new HashMap();
		}
		PipeLineSession.setListenerParameters(context, null, correlationId, tsReceived, tsSent);
		return processMessageInAdapter(origin, message, message, null, correlationId, context, waitingTime, false);
	}



	public void processRawMessage(IListener origin, Object message) throws ListenerException {
		processRawMessage(origin, message, null, -1);
	}
	public void processRawMessage(IListener origin, Object message, Map context) throws ListenerException {
		processRawMessage(origin, message, context, -1);
	}

	public void processRawMessage(IListener origin, Object rawMessage, Map threadContext, long waitingDuration) throws ListenerException {
		processRawMessage(origin, rawMessage, threadContext, waitingDuration, false);
	}

	/**
	 * All messages that for this receiver are pumped down to this method, so it actually
	 * calls the {@link nl.nn.adapterframework.core.Adapter adapter} to process the message.<br/>

	 * Assumes that a transation has been started where necessary
	 */
	private void processRawMessage(IListener origin, Object rawMessage, Map threadContext, long waitingDuration, boolean retry) throws ListenerException {
		if (rawMessage==null) {
			log.debug(getLogPrefix()+"received null message, returning directly");
			return;
		}		
		if (threadContext==null) {
			threadContext = new HashMap();
		}
		
		String message = origin.getStringFromRawMessage(rawMessage, threadContext);
		String technicalCorrelationId = origin.getIdFromRawMessage(rawMessage, threadContext);
		String messageId = (String)threadContext.get("id");
		processMessageInAdapter(origin, rawMessage, message, messageId, technicalCorrelationId, threadContext, waitingDuration, retry);
	}

	public void retryMessage(String messageId) throws ListenerException {
		if (getErrorStorage()==null) {
			throw new ListenerException(getLogPrefix()+"has no errorStorage, cannot retry messageId ["+messageId+"]");
		}
		PlatformTransactionManager txManager = getTxManager(); 
		TransactionStatus txStatus = txManager.getTransaction(TXNEW);
		Map threadContext = new HashMap();
		Object msg=null;
		try {
			try {
				ITransactionalStorage errorStorage = getErrorStorage();
				msg = errorStorage.getMessage(messageId);
				processRawMessage(getListener(), msg, threadContext, -1, true);
			} catch (Exception e) {
				txStatus.setRollbackOnly();
				throw new ListenerException(e);
			} finally {
				txManager.commit(txStatus);
			}
		} catch (ListenerException e) {
			txStatus = txManager.getTransaction(TXNEW);
			try {	
				if (msg instanceof Serializable) {
					String correlationId = (String)threadContext.get(PipeLineSession.businessCorrelationIdKey);
					String receivedDateStr = (String)threadContext.get(PipeLineSession.tsReceivedKey);
					Date receivedDate = DateUtils.parseToDate(receivedDateStr,DateUtils.FORMAT_FULL_GENERIC);
					errorStorage.deleteMessage(messageId);
					errorStorage.storeMessage(messageId,correlationId,receivedDate,"after retry: "+e.getMessage(),(Serializable)msg);	
				} else {
					log.warn(getLogPrefix()+"retried message is not serializable, cannot update comments");
				}
			} catch (SenderException e1) {
				txStatus.setRollbackOnly();
				log.warn(getLogPrefix()+"could not update comments in errorStorage",e1);
			} finally {
				txManager.commit(txStatus);
			}
			throw e;
		}
	}

	/*
	 * assumes message is read, and when transacted, transation is still open.
	 */
	private String processMessageInAdapter(IListener origin, Object rawMessage, String message, String messageId, String technicalCorrelationId, Map threadContext, long waitingDuration, boolean retry) throws ListenerException {
		String result=null;
		PipeLineResult pipeLineResult=null;
		long startProcessingTimestamp = System.currentTimeMillis();
//		if (message==null) {
//			requestSizeStatistics.addValue(0);
//		} else {
//			requestSizeStatistics.addValue(message.length());
//		}
		log.debug(getLogPrefix()+"received message with messageId ["+messageId+"] (technical) correlationId ["+technicalCorrelationId+"]");

		if (StringUtils.isEmpty(messageId)) {
			messageId=getName()+"-"+Misc.createSimpleUUID();
			if (log.isDebugEnabled()) 
				log.debug(getLogPrefix()+"generated messageId ["+messageId+"]");
		}

		String businessCorrelationId=technicalCorrelationId;
		if (correlationIDTp!=null) {
			try {
				businessCorrelationId=correlationIDTp.transform(message,null);
			} catch (Exception e) {
				throw new ListenerException(getLogPrefix()+"could not extract businessCorrelationId",e);
			}
			if (StringUtils.isEmpty(businessCorrelationId)) {
				if (StringUtils.isNotEmpty(technicalCorrelationId)) {
					log.warn(getLogPrefix()+"did not find correlationId using XpathExpression ["+getCorrelationIDXPath()+"], reverting to correlationId of transfer ["+technicalCorrelationId+"]");
					businessCorrelationId=technicalCorrelationId;
				} else {
					if (StringUtils.isNotEmpty(messageId)) {
						log.warn(getLogPrefix()+"did not find correlationId using XpathExpression ["+getCorrelationIDXPath()+"] or technical correlationId, reverting to messageId ["+messageId+"]");
						businessCorrelationId=messageId;
					}
				}
			}
			log.info(getLogPrefix()+"messageId [" + messageId + "] technicalCorrelationId [" + technicalCorrelationId + "] businessCorrelationId [" + businessCorrelationId + "]");
		}
		if (StringUtils.isEmpty(businessCorrelationId)) {
			if (log.isDebugEnabled()) { log.debug(getLogPrefix()+"did not find businessCorrelationId, reverting to correlationId of transfer ["+technicalCorrelationId+"]"); } 
			businessCorrelationId=messageId;
		}
		threadContext.put(PipeLineSession.businessCorrelationIdKey, businessCorrelationId);       
		if (checkTryCount(messageId, retry, rawMessage, message, threadContext, businessCorrelationId)) {
			if (!isTransacted()) {
				log.warn(getLogPrefix()+"received message with messageId [" + messageId + "] which is already stored in error storage or messagelog; aborting processing");
 			}
			numRejected.increase();
			return result;
		}
		if (getCachedProcessResult(messageId)!=null) {
			numRetried.increase();
		}

		int txOption = this.getTransactionAttributeNum();
		TransactionDefinition txDef = SpringTxManagerProxy.getTransactionDefinition(txOption,getTransactionTimeout());
		//TransactionStatus txStatus = txManager.getTransaction(txDef);
		IbisTransaction itx = new IbisTransaction(txManager, txDef, "receiver [" + getName() + "]");
		TransactionStatus txStatus = itx.getStatus();
        
		// update processing statistics
		// count in processing statistics includes messages that are rolled back to input
		startProcessingMessage(waitingDuration);
		
		PipeLineSession pipelineSession = null;
		String errorMessage="";
		boolean messageInError = false;
		try {
			String pipelineMessage;
			if (origin instanceof IBulkDataListener) {
				try {
					IBulkDataListener bdl = (IBulkDataListener)origin;
					pipelineMessage=bdl.retrieveBulkData(rawMessage,message,threadContext);
				} catch (Throwable t) {
					errorMessage = t.getMessage();
					messageInError = true;
					ListenerException l = wrapExceptionAsListenerException(t);
					throw l;
				}
			} else {
				pipelineMessage=message;
			}
			
			numReceived.increase();
			// Note: errorMessage is used to pass value from catch-clause to finally-clause!
			pipelineSession = createProcessingContext(businessCorrelationId, threadContext, messageId);
//			threadContext=pipelineSession; // this is to enable Listeners to use session variables, for instance in afterProcessMessage()
			try {
				// TODO: What about Ibis42 compat mode?
				if (isIbis42compatibility()) {
					pipeLineResult = adapter.processMessage(businessCorrelationId, pipelineMessage, pipelineSession);
					result=pipeLineResult.getResult();
					errorMessage = result;
					if (pipeLineResult.getState().equals(adapter.getErrorState())) {
						messageInError = true;
					}
				} else {
					// TODO: Find the right catch-clause where we decide about
					// retrying or swallowing and pushing to error-storage
					// Right now we make the decision before pushing a response
					// back which might be too early.
					try {
						if (getMessageLog()!=null) {
							getMessageLog().storeMessage(messageId, businessCorrelationId, new Date(),"log",pipelineMessage);
						}
						pipeLineResult = adapter.processMessageWithExceptions(businessCorrelationId, pipelineMessage, pipelineSession);
						result=pipeLineResult.getResult();
						errorMessage = "exitState ["+pipeLineResult.getState()+"], result ["+result+"]";
						if (log.isDebugEnabled()) { log.debug(getLogPrefix()+"received result: "+errorMessage); }
						messageInError=txStatus.isRollbackOnly();
						if (!messageInError && !isTransacted()) {
							String commitOnState=((Adapter)adapter).getPipeLine().getCommitOnState();
							
							if (StringUtils.isNotEmpty(commitOnState) && 
								!commitOnState.equalsIgnoreCase(pipeLineResult.getState())) {
								messageInError=true;
							}
						}							

					} catch (Throwable t) {
						if (TransactionSynchronizationManager.isActualTransactionActive()) {
							log.debug("<*>"+getLogPrefix() + "TX Update: Received failure, transaction " +
									(txStatus.isRollbackOnly()?"already":"not yet") +
									" marked for rollback-only");
						}
						errorMessage = t.getMessage();
						messageInError = true;
						if (pipeLineResult==null) {
							pipeLineResult=new PipeLineResult();
						}
						if (StringUtils.isEmpty(pipeLineResult.getResult())) {
							String formattedErrorMessage=adapter.formatErrorMessage("exception caught",t,message,messageId,this,startProcessingTimestamp);
							pipeLineResult.setResult(formattedErrorMessage);
						}
						ListenerException l = wrapExceptionAsListenerException(t);
						throw l;
					}
				}
			} finally {
				putSessionKeysIntoThreadContext(threadContext, pipelineSession);
			}
//			if (result==null) {
//				responseSizeStatistics.addValue(0);
//			} else {
//				responseSizeStatistics.addValue(result.length());
//			}
			if (getSender()!=null) {
				String sendMsg = sendResultToSender(technicalCorrelationId, result);
				if (sendMsg != null) {
					errorMessage = sendMsg;
				}
			}
		} finally {
			cacheProcessResult(messageId, businessCorrelationId, errorMessage, new Date(startProcessingTimestamp));
			if (!isTransacted() && messageInError) {
				// NB: Because the below happens from a finally-clause, any
				// exception that has occurred will still be propagated even
				// if we decide not to retry the message.
				// This should perhaps be avoided
				retryOrErrorStorage(rawMessage, startProcessingTimestamp, txStatus, errorMessage, message, messageId, businessCorrelationId, retry);
			}
			try {
				Map afterMessageProcessedMap;
				if (threadContext!=null) {
					afterMessageProcessedMap=threadContext;
					if (pipelineSession!=null) {
						threadContext.putAll(pipelineSession);
					}
				} else {
					afterMessageProcessedMap=pipelineSession;
				}
				// TODO: Should this be done in a finally, unconditionally?
				// Perhaps better to have separate methods for correct processing,
				// and cleanup after an error?
				origin.afterMessageProcessed(pipeLineResult,rawMessage, afterMessageProcessedMap);
			} finally {
				long finishProcessingTimestamp = System.currentTimeMillis();
				finishProcessingMessage(finishProcessingTimestamp-startProcessingTimestamp);
				if (!txStatus.isCompleted()) {
					// NB: Spring will take care of executing a commit or a rollback;
					// Spring will also ONLY commit the transaction if it was newly created
					// by the above call to txManager.getTransaction().
					//txManager.commit(txStatus);
					itx.commit();
				} else {
					throw new ListenerException(getLogPrefix()+"Transaction already completed; we didn't expect this");
				}
			}
		}
		if (log.isDebugEnabled()) log.debug(getLogPrefix()+"messageId ["+messageId+"] correlationId ["+businessCorrelationId+"] returning result ["+result+"]");
		return result;
	}



	private synchronized void cachePoisonMessageId(String messageId) {
		poisonMessageIdCache.put(messageId, messageId);
	}
	private synchronized boolean isMessageIdInPoisonCache(String messageId) {
		return poisonMessageIdCache.containsKey(messageId);
	}

	private synchronized void cacheProcessResult(String messageId, String correlationId, String errorMessage, Date receivedDate) {
		ProcessResultCacheItem cacheItem=getCachedProcessResult(messageId);
		if (cacheItem==null) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"caching first result for correlationId ["+correlationId+"]");
			cacheItem= new ProcessResultCacheItem();
			cacheItem.tryCount=1;
			cacheItem.correlationId=correlationId;
			cacheItem.receiveDate=receivedDate;
		} else {
			cacheItem.tryCount++;
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"increased try count for correlationId ["+correlationId+"] to ["+cacheItem.tryCount+"]");
		}
		cacheItem.comments=errorMessage;
		processResultCache.put(messageId, cacheItem);
	}
	private synchronized boolean isMessageIdInProcessResultCache(String messageId) {
		return processResultCache.containsKey(messageId);
	}
	private synchronized ProcessResultCacheItem getCachedProcessResult(String messageId) {
		return (ProcessResultCacheItem)processResultCache.get(messageId);
	}
    
    
	private long getAndIncrementMessageRetryCount(String messageId) {
		Counter retries;
		synchronized (messageRetryCounters) {
			retries = (Counter) messageRetryCounters.get(messageId);
			if (retries == null) {
				retries = new Counter(0);
				messageRetryCounters.put(messageId, retries);
				return 0L;
			}
		}
		retries.increase();
		return retries.getValue();
	}

	private long removeMessageRetryCount(String messageId) {
		synchronized (messageRetryCounters) {
			Counter retries = (Counter) messageRetryCounters.get(messageId);
			if (retries == null) {
				return 0;
			} else {
				messageRetryCounters.remove(messageId);
				return retries.getValue();
			}
		}
	}

	/*
	 * returns true if message is already processed
	 */
	private boolean checkTryCount(String messageId, boolean retry, Object rawMessage, String message, Map threadContext, String correlationId) throws ListenerException {
		if (!retry) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"checking try count for messageId ["+messageId+"]");
 //			if (isTransacted()) {
 				int deliveryCount=-1;
 				if (getListener() instanceof IKnowsDeliveryCount) {
					deliveryCount = ((IKnowsDeliveryCount)getListener()).getDeliveryCount(rawMessage);
 				}
				ProcessResultCacheItem prci = getCachedProcessResult(messageId);
				if (prci==null) {
					if (deliveryCount<=1) {
						resetRetryInterval();
						return false;
					}
				} else {
					if (deliveryCount<1) {
						deliveryCount=prci.tryCount;
					}
					prci.tryCount++;
				}
				
				if (deliveryCount<=getMaxRetries()+1) {
					log.warn(getLogPrefix()+"message with messageId ["+messageId+"] has already been processed ["+(deliveryCount-1)+"] times, will try again");
					resetRetryInterval();
					return false;
				}
				warn(getLogPrefix()+"message with messageId ["+messageId+"] has already been processed ["+(deliveryCount-1)+"] times, will not try again; maxRetries=["+getMaxRetries()+"]");
				if (deliveryCount>getMaxRetries()+2) {
					increaseRetryIntervalAndWait(null,getLogPrefix()+"saw message with messageId ["+messageId+"] too many times ["+deliveryCount+"]; maxRetries=["+getMaxRetries()+"]");
				}
				String comments="too many retries";
				Date rcvDate;
				if (prci!=null) {
					comments+="; "+prci.comments;
					rcvDate=prci.receiveDate;
				} else {
					rcvDate=new Date();
				}
				if (isTransacted() || (getErrorStorage() != null && (!isCheckForDuplicates() || !getErrorStorage().containsMessageId(messageId)))) {
					moveInProcessToError(messageId, correlationId, message, rcvDate, comments, rawMessage, TXREQUIRED);
				}
				PipeLineResult plr = new PipeLineResult();
				String result="<error>"+XmlUtils.encodeChars(comments)+"</error>";
				plr.setResult(result);
				plr.setState("ERROR");
				if (getSender()!=null) {
					// TODO correlationId should be technical correlationID!
					String sendMsg = sendResultToSender(correlationId, result);
					if (sendMsg != null) {
						log.warn("problem sending result:"+sendMsg);
					}
				}
				getListener().afterMessageProcessed(plr, rawMessage, threadContext);
				return true;
//			} else {
//				if (isMessageIdInPoisonCache(messageId)) {
//					return true;
//				}
//				if (getErrorStorage() != null && getErrorStorage().containsMessageId(messageId)) {
//					return true;
//				}
//			}
		} 
		if (isCheckForDuplicates() && getMessageLog()!= null && getMessageLog().containsMessageId(messageId)) {
			return true;
		}
		return false;
	}

	/**
	 * Decide if a failed message can be retried, or should be removed from the
	 * queue and put to the error-storage.
	 * 
	 * <p>
	 * In the former case, the current transaction is marked rollback-onle.
	 * </p>
	 * <p>
	 * In the latter case, the message is also moved to the error-storage and
	 * it's message-id is 'blacklisted' in the internal cache of poison-messages.
	 * </p>
	 * <p>
	 * NB: Because the current global transaction might have already been marked for
	 * rollback-only, even if we decide not to retry the message it might still
	 * be redelivered to us. In that case, the poison-cache will save the day.
	 * </p>
	 * 
	 * @return Returns <code>true</code> if the message can still be retried,
	 * or <code>false</code> if the message will not be retried.
	 */
	private boolean retryOrErrorStorage(Object rawMessage, long startProcessingTimestamp, TransactionStatus txStatus, String errorMessage, String message, String messageId, String correlationId, boolean wasRetry) {

		long retryCount = getAndIncrementMessageRetryCount(messageId);
		log.error(getLogPrefix()+"message with id ["+messageId+"] had error in processing; current retry-count: " + retryCount);
        
		// Mark TX as rollback-only, because in any case updates done in the
		// transaction may not be performed.
		txStatus.setRollbackOnly();
		if (wasRetry) {
			return false;
		}
		if (isTransacted()) {
			// If not yet exceeded the max retry count,
			// mark TX as rollback-only and throw an
			// exception
			if (retryCount < maxRetries) {
				log.error(getLogPrefix()+"message with id ["+messageId+"] will be retried; transaction marked rollback-only");
				return true;
			} else {
				// Max retries exceeded; message to be moved
				// to error location (OR LOST!)
				log.error(getLogPrefix()+"message with id ["+messageId+"] retry count exceeded;");
				//removeMessageRetryCount(messageId);
				moveInProcessToError(messageId, correlationId, message, new Date(startProcessingTimestamp), errorMessage, rawMessage, TXNEW);
				return false;
			}
		} else {
			log.error(getLogPrefix()+"not transacted, message with id ["+messageId+"] will not be retried");
			moveInProcessToError(messageId, correlationId, message, new Date(startProcessingTimestamp), errorMessage, rawMessage, TXNEW);
			return false;
		}
	}

	public void exceptionThrown(INamedObject object, Throwable t) {
		String msg = getLogPrefix()+"received exception ["+t.getClass().getName()+"] from ["+object.getName()+"]";
		if (ONERROR_CONTINUE.equalsIgnoreCase(getOnError())) {
//			warn(msg+", will continue processing messages when they arrive: "+ t.getMessage());
			error(msg+", will continue processing messages when they arrive",t);
		} else {
			error(msg+", stopping receiver", t);
			stopRunning();
		}
	}

	public String getEventSourceName() {
		return getLogPrefix().trim();
	}
	protected void registerEvent(String eventCode) {
		if (eventHandler!=null) {
			eventHandler.registerEvent(this,eventCode);
		}		
	}
	protected void throwEvent(String eventCode) {
		if (eventHandler!=null) {
			eventHandler.fireEvent(this,eventCode);
		}
	}

	public void resetRetryInterval() {
		synchronized (this) {
			if (suspensionMessagePending) {
				suspensionMessagePending=false;
				throwEvent(RCV_RESUMED_MONITOR_EVENT);
			}
			retryInterval = 1;
		}
	}

	public void increaseRetryIntervalAndWait(Throwable t, String description) {
		long currentInterval;
		synchronized (this) {
			currentInterval = retryInterval;
			retryInterval = retryInterval * 2;
			if (retryInterval > MAX_RETRY_INTERVAL) {
				retryInterval = MAX_RETRY_INTERVAL;
			}
		}
		if (currentInterval>1) {
			error(description+", will continue retrieving messages in [" + currentInterval + "] seconds", t);
		} else {
			log.warn(getLogPrefix()+"will continue retrieving messages in [" + currentInterval + "] seconds", t);
		}
		if (currentInterval*2 > RCV_SUSPENSION_MESSAGE_THRESHOLD) {
			if (!suspensionMessagePending) {
				suspensionMessagePending=true;
				throwEvent(RCV_SUSPENDED_MONITOR_EVENT);
			}
		}
		while (isInRunState(RunStateEnum.STARTED) && currentInterval-- > 0) {
			try {
				Thread.sleep(1000);
			} catch (Exception e2) {
				error("sleep interupted", e2);
				stopRunning();
			}
		}
	}
	

	public void iterateOverStatistics(StatisticsKeeperIterationHandler hski, Object data, int action) throws SenderException {
		Object recData=hski.openGroup(data,getName(),"receiver");
		hski.handleScalar(recData,"messagesReceived", getMessagesReceived());
		hski.handleScalar(recData,"messagesRetried", getMessagesRetried());
		hski.handleScalar(recData,"messagesRejected", numRejected.getValue());
		hski.handleScalar(recData,"messagesReceivedThisInterval", numReceived.getIntervalValue());
		hski.handleScalar(recData,"messagesRetriedThisInterval", numRetried.getIntervalValue());
		hski.handleScalar(recData,"messagesRejectedThisInterval", numRejected.getIntervalValue());
		numReceived.performAction(action);
		numRetried.performAction(action);
		numRejected.performAction(action);
		Iterator statsIter=getProcessStatisticsIterator();
		Object pstatData=hski.openGroup(recData,null,"procStats");
		if (statsIter != null) {
			while(statsIter.hasNext()) {				    
				StatisticsKeeper pstat = (StatisticsKeeper) statsIter.next();
				hski.handleStatisticsKeeper(pstatData,pstat);
				pstat.performAction(action);
			}
		}
		hski.closeGroup(pstatData);

		statsIter = getIdleStatisticsIterator();
		if (statsIter != null) {
			Object istatData=hski.openGroup(recData,null,"idleStats");
			while(statsIter.hasNext()) {				    
				StatisticsKeeper pstat = (StatisticsKeeper) statsIter.next();
				hski.handleStatisticsKeeper(istatData,pstat);
				pstat.performAction(action);
			}
			hski.closeGroup(istatData);
		}

		statsIter = getQueueingStatisticsIterator();
		if (statsIter!=null) {
			Object qstatData=hski.openGroup(recData,null,"queueingStats");
			while(statsIter.hasNext()) {				    
				StatisticsKeeper qstat = (StatisticsKeeper) statsIter.next();
				hski.handleStatisticsKeeper(qstatData,qstat);
				qstat.performAction(action);
			}
			hski.closeGroup(qstatData);
		}


		hski.closeGroup(recData);
	}



	public boolean isThreadCountReadable() {
		if (getListener() instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)getListener();
			
			return tcc.isThreadCountReadable();
		}
		if (getListener() instanceof IPullingListener) {
			return true;
		}
		return false;
	}
	public boolean isThreadCountControllable() {
		if (getListener() instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)getListener();
			
			return tcc.isThreadCountControllable();
		}
		return false;
	}

	public int getCurrentThreadCount() {
		if (getListener() instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)getListener();
			
			return tcc.getCurrentThreadCount();
		}
		if (getListener() instanceof IPullingListener) {
			return listenerContainer.getThreadsRunning();
		}
		return -1;
	}

	public int getMaxThreadCount() {
		if (getListener() instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)getListener();
			
			return tcc.getMaxThreadCount();
		}
		if (getListener() instanceof IPullingListener) {
			return getNumThreads();
		}
		return -1;
	}

	public void increaseThreadCount() {
		if (getListener() instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)getListener();
			
			tcc.increaseThreadCount();
		}
	}

	public void decreaseThreadCount() {
		if (getListener() instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)getListener();
			
			tcc.decreaseThreadCount();
		}
	}

	public void setRunState(RunStateEnum state) {
		runState.setRunState(state);
	}

	public void waitForRunState(RunStateEnum requestedRunState) throws InterruptedException {
		runState.waitForRunState(requestedRunState);
	}
	public boolean waitForRunState(RunStateEnum requestedRunState, long timeout) throws InterruptedException {
		return runState.waitForRunState(requestedRunState, timeout);
	}
	
		/**
		 * Get the {@link RunStateEnum runstate} of this receiver.
		 */
	public RunStateEnum getRunState() {
		return runState.getRunState();
	}
	
	public boolean isInRunState(RunStateEnum someRunState) {
		return runState.isInState(someRunState);
	}
    
	protected synchronized StatisticsKeeper getProcessStatistics(int threadsProcessing) {
		StatisticsKeeper result;
		try {
			result = ((StatisticsKeeper)processStatistics.get(threadsProcessing));
		} catch (IndexOutOfBoundsException e) {
			result = null;
		}
	
		if (result==null) {
			while (processStatistics.size()<threadsProcessing+1){
				result = new StatisticsKeeper((processStatistics.size()+1)+" threads processing");
				processStatistics.add(processStatistics.size(), result);
			}
		}
		
		return (StatisticsKeeper) processStatistics.get(threadsProcessing);
	}
	
	protected synchronized StatisticsKeeper getIdleStatistics(int threadsProcessing) {
		StatisticsKeeper result;
		try {
			result = ((StatisticsKeeper)idleStatistics.get(threadsProcessing));
		} catch (IndexOutOfBoundsException e) {
			result = null;
		}

		if (result==null) {
			while (idleStatistics.size()<threadsProcessing+1){
			result = new StatisticsKeeper((idleStatistics.size())+" threads processing");
				idleStatistics.add(idleStatistics.size(), result);
			}
		}
		return (StatisticsKeeper) idleStatistics.get(threadsProcessing);
	}
	
	/**
	 * Returns an iterator over the process-statistics
	 * @return iterator
	 */
	public Iterator getProcessStatisticsIterator() {
		return processStatistics.iterator();
	}
	
	/**
	 * Returns an iterator over the idle-statistics
	 * @return iterator
	 */
	public Iterator getIdleStatisticsIterator() {
		return idleStatistics.iterator();
	}
	public Iterator getQueueingStatisticsIterator() {
		if (queueingStatistics==null) {
			return null;
		}
		return queueingStatistics.iterator();
	}		
	
	public ISender getSender() {
		return sender;
	}
	
	protected void setSender(ISender sender) {
		this.sender = sender;
	}

	public void setAdapter(IAdapter adapter) {
		this.adapter = adapter;
	}
	
	
	
	/**
	 * Returns the listener
	 * @return IListener
	 */
	public IListener getListener() {
		return listener;
	}/**
	 * Sets the listener. If the listener implements the {@link nl.nn.adapterframework.core.INamedObject name} interface and no <code>getName()</code>
	 * of the listener is empty, the name of this object is given to the listener.
	 * Creation date: (04-11-2003 12:04:05)
	 * @param newListener IListener
	 */
	protected void setListener(IListener newListener) {
		listener = newListener;
		if (StringUtils.isEmpty(listener.getName())) {
			listener.setName("listener of ["+getName()+"]");
		}
		if (listener instanceof RunStateEnquiring)  {
			((RunStateEnquiring) listener).SetRunStateEnquirer(runState);
		}
	}
	/**
	 * Sets the inProcessStorage.
	 * @param inProcessStorage The inProcessStorage to set
	 * @deprecated
	 */
	protected void setInProcessStorage(ITransactionalStorage inProcessStorage) {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = getLogPrefix()+"<*> In-Process Storage is not used anymore. Please remove from configuration. <*>";
		configWarnings.add(log, msg);
		// We do not use an in-process storage anymore, but we temporarily
		// store it if it's set by the configuration.
		// During configure, we check if we need to use the in-process storage
		// as error-storage.
		this.tmpInProcessStorage = inProcessStorage;
	}

	/**
	 * Returns the errorSender.
	 * @return ISender
	 */
	public ISender getErrorSender() {
		return errorSender;
	}

	public ITransactionalStorage getErrorStorage() {
		return errorStorage;
	}

	/**
	 * Sets the errorSender.
	 * @param errorSender The errorSender to set
	 */
	protected void setErrorSender(ISender errorSender) {
		this.errorSender = errorSender;
		errorSender.setName("errorSender of ["+getName()+"]");
	}

	protected void setErrorStorage(ITransactionalStorage errorStorage) {
		if (errorStorage.isActive()) {
			this.errorStorage = errorStorage;
			errorStorage.setName("errorStorage of ["+getName()+"]");
			if (StringUtils.isEmpty(errorStorage.getSlotId())) {
				errorStorage.setSlotId(getName());
			}
			errorStorage.setType(JdbcTransactionalStorage.TYPE_ERRORSTORAGE);
		}
	}
	
	/**
	 * Sets the messageLog.
	 */
	protected void setMessageLog(ITransactionalStorage messageLog) {
		if (messageLog.isActive()) {
			this.messageLog = messageLog;
			messageLog.setName("messageLog of ["+getName()+"]");
			if (StringUtils.isEmpty(messageLog.getSlotId())) {
				messageLog.setSlotId(getName());
			}
			messageLog.setType(JdbcTransactionalStorage.TYPE_MESSAGELOG);
		}
	}
	public ITransactionalStorage getMessageLog() {
		return messageLog;
	}


	/**
	 * Get the number of messages received.
	  * @return long
	 */
	public long getMessagesReceived() {
		return numReceived.getValue();
	}

	/**
	 * Get the number of messages retried.
	  * @return long
	 */
	public long getMessagesRetried() {
		return numRetried.getValue();
	}

	/**
	 * Get the number of messages rejected (discarded or put in errorstorage).
	  * @return long
	 */
	public long getMessagesRejected() {
		return numRejected.getValue();
	}

	
//	public StatisticsKeeper getRequestSizeStatistics() {
//		return requestSizeStatistics;
//	}
//	public StatisticsKeeper getResponseSizeStatistics() {
//		return responseSizeStatistics;
//	}


	/**
	 * Sets the name of the Receiver. 
	 * If the listener implements the {@link nl.nn.adapterframework.core.INamedObject name} interface and <code>getName()</code>
	 * of the listener is empty, the name of this object is given to the listener.
	 */
	public void setName(String newName) {
		name = newName;
		propagateName();
	}


	public String getName() {
		return name;
	}
	
	/**
	 * Controls the use of XA-transactions.
	 */
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
	public boolean isTransacted() {
//		return transacted;
		int txAtt = getTransactionAttributeNum();
		return  txAtt==TransactionDefinition.PROPAGATION_REQUIRED || 
				txAtt==TransactionDefinition.PROPAGATION_REQUIRES_NEW ||
				txAtt==TransactionDefinition.PROPAGATION_MANDATORY;
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

	public void setOnError(String newOnError) {
		onError = newOnError;
	}
	public String getOnError() {
		return onError;
	}
    
	public boolean isOnErrorContinue() {
		return ONERROR_CONTINUE.equalsIgnoreCase(getOnError());
	}
	public IAdapter getAdapter() {
		return adapter;
	}


	/**
	 *  Returns a toString of this class by introspection and the toString() value of its listener.
	 *
	 * @return    Description of the Return Value
	 */
	public String toString() {
		String result = super.toString();
		ToStringBuilder ts=new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
		ts.append("name", getName() );
		result += ts.toString();
		result+=" listener ["+(listener==null ? "-none-" : listener.toString())+"]";
		return result;
	}

	
	/**
	 * The number of threads that this receiver is configured to work with.
	 */
	public void setNumThreads(int newNumThreads) {
		numThreads = newNumThreads;
	}
	public int getNumThreads() {
		return numThreads;
	}

	public String formatException(String extrainfo, String correlationId, String message, Throwable t) {
		return getAdapter().formatErrorMessage(extrainfo,t,message,correlationId,null,0);
	}


	public int getNumThreadsPolling() {
		return numThreadsPolling;
	}

	public void setNumThreadsPolling(int i) {
		numThreadsPolling = i;
	}

	public boolean isIbis42compatibility() {
		return ibis42compatibility;
	}

	public void setIbis42compatibility(boolean b) {
		ibis42compatibility = b;
	}
	

	// event numbers for tracing
	public void setBeforeEvent(int i) {
		beforeEvent = i;
	}
	public int getBeforeEvent() {
		return beforeEvent;
	}
	public void setAfterEvent(int i) {
		afterEvent = i;
	}
	public int getAfterEvent() {
		return afterEvent;
	}
	public void setExceptionEvent(int i) {
		exceptionEvent = i;
	}
	public int getExceptionEvent() {
		return exceptionEvent;
	}





	public int getMaxRetries() {
		return maxRetries;
	}

	public void setMaxRetries(int i) {
		maxRetries = i;
	}
	
	public void setActive(boolean b) {
		active = b;
	}
	public boolean isActive() {
		return active;
	}

	public void setReturnedSessionKeys(String string) {
		returnedSessionKeys = string;
	}
	public String getReturnedSessionKeys() {
		return returnedSessionKeys;
	}

	public void setHiddenInputSessionKeys(String string) {
		hiddenInputSessionKeys = string;
	}
	public String getHiddenInputSessionKeys() {
		return hiddenInputSessionKeys;
	}

	public void setTaskExecutor(TaskExecutor executor) {
		taskExecutor = executor;
	}
	public TaskExecutor getTaskExecutor() {
		return taskExecutor;
	}


	public void setTxManager(PlatformTransactionManager manager) {
		txManager = manager;
	}
	public PlatformTransactionManager getTxManager() {
		return txManager;
	}


	private String sendResultToSender(String correlationId, String result) {
		String errorMessage = null;
		try {
			if (getSender() != null) {
				if (log.isDebugEnabled()) { log.debug("Receiver ["+getName()+"] sending result to configured sender"); }
				getSender().sendMessage(correlationId, result);
			}
		} catch (Exception e) {
			String msg = "receiver [" + getName() + "] caught exception in message post processing";
			error(msg, e);
			errorMessage = msg + ": " + e.getMessage();
			if (ONERROR_CLOSE.equalsIgnoreCase(getOnError())) {
				log.info("receiver [" + getName() + "] closing after exception in post processing");
				stopRunning();
			}
		}
		return errorMessage;
	}

	private ListenerException wrapExceptionAsListenerException(Throwable t) {
		ListenerException l;
		if (t instanceof ListenerException) {
			l = (ListenerException) t;
		} else {
			l = new ListenerException(t);
		}
		return l;
	}

	public BeanFactory getBeanFactory() {
		return beanFactory;
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	public PullingListenerContainer getListenerContainer() {
		return listenerContainer;
	}

	public void setListenerContainer(PullingListenerContainer listenerContainer) {
		this.listenerContainer = listenerContainer;
	}

	public PullingListenerContainer createListenerContainer() {
		PullingListenerContainer plc = (PullingListenerContainer) beanFactory.getBean("listenerContainer");
		plc.setReceiver(this);
		plc.configure();
		return plc;
	}

	public int getPoisonMessageIdCacheSize() {
		return poisonMessageIdCacheSize;
	}

	public void setPoisonMessageIdCacheSize(int poisonMessageIdCacheSize) {
		this.poisonMessageIdCacheSize = poisonMessageIdCacheSize;
	}

	public int getProcessResultCacheSize() {
		return processResultCacheSize;
	}
	public void setProcessResultCacheSize(int processResultCacheSize) {
		this.processResultCacheSize = processResultCacheSize;
	}
	
	public void setPollInterval(int i) {
		pollInterval = i;
	}
	public int getPollInterval() {
		return pollInterval;
	}

	public void setCheckForDuplicates(boolean b) {
		checkForDuplicates = b;
	}
	public boolean isCheckForDuplicates() {
		return checkForDuplicates;
	}

	public void setTransactionTimeout(int i) {
		transactionTimeout = i;
	}
	public int getTransactionTimeout() {
		return transactionTimeout;
	}

	public void setCorrelationIDXPath(String string) {
		correlationIDXPath = string;
	}
	public String getCorrelationIDXPath() {
		return correlationIDXPath;
	}

}
