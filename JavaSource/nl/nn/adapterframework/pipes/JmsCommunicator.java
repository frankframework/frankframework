package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.ICorrelatedPullingListener;
import nl.nn.adapterframework.jms.JmsSender;

/**
 * Sends a message (the PipeInput) to a Topic or Queue, 
 *  and receives a message from another Topic or Queue after the input message has been sent.
 *
 * If a {@link nl.nn.adapterframework.core.ICorrelatedPullingListener listener} is specified it waits for a reply with
 * the correct correlationID.
 * </p>
 * <p>The receiving of messages is done with a selector on the JMSCorrelationId
 * on the queue or topic specified. Therefore there no objection to define this
 * receiver on a queue already in use.</p>
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setResultOnTimeOut(String) resultOnTimeOut}</td><td>result returned when no return-message was received within the timeout limit (e.g. "receiver timed out").</td><td>&nbsp;</td></tr>
 * <tr><td>{@link JmsSender#setReplyToName(String) sender.replyToName}</td><td>name of the queue the receiving party should reply to, as included in the message that is sent</td><td>&nbsp;</td></tr>
 * <tr><td>{@link JmsSender#setName(String) sender.Name}</td><td>name of the sender object</td><td>&nbsp;</td></tr>
 * <tr><td>{@link JmsSender#setDestinationName(String) sender.destinationName}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link JmsSender#setDestinationType(String) sender.destinationType}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link JmsSender#setAcknowledgeMode(String) sender.acknowledgeMode}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link JmsSender#setPersistent(boolean) sender.persistent}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link JmsSender#setJmsRealm(String) sender.jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * </table>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ICorrelatedPullingListener listener}</td><td>specification of queue to listen to for a reply</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default when a good message was retrieved, or the message was successfully sent and no receiver was specified</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified, and otherwise under same condition as "success"</td></tr>
 * <tr><td>"timeout"</td><td>no data was received (timeout on listening), while a receiver was specified.</td></tr>
 * </table>
 * </p>
 * @version Id
 * @author Johan Verrips
 * @deprecated please use GenericMessageSendingPipe with JmsSender (and if necessary JmsListener), that has same functionality
 */

public class JmsCommunicator extends MessageSendingPipe {

public JmsCommunicator() {
	super();
	setSender(new JmsSender());
}

	public void configure() throws ConfigurationException {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = getLogPrefix(null)+"The class ["+getClass().getName()+"] has been deprecated. Please change to ["+GenericMessageSendingPipe.class.getName()+"]";
		configWarnings.add(log, msg);
		super.configure();
	}
	public void setListener(ICorrelatedPullingListener listener) {
		super.setListener(listener);
	}
}
