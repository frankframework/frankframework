package nl.nn.adapterframework.receivers;

import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.jms.JmsListener;

import java.util.HashMap;

/**
 * A {@link PullingReceiverBase PullingReceiver} that uses a {@link JmsListener} as its listener,
 * to receive messages from a JMS queue or topic.
/*
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.receivers.JmsMessageReceiver</td><td>&nbsp;</td></tr>
 * <tr><td>{@link PullingReceiverBase#setName(String) name}</td>  <td>name of the receiver as known to the adapter</td><td>&nbsp;</td></tr>
 * <tr><td>{@link PullingReceiverBase#setNumThreads(int) numThreads}</td><td>the number of threads listening in parallel for messages</td><td>1</td></tr>
 * <tr><td>{@link PullingReceiverBase#setOnError(String) onError}</td><td>one of 'continue' or 'close'. Controls the behaviour of the receiver when it encounters an error sending a reply</td><td>continue</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.jms.JmsListener#setDestinationName(String) listener.destinationName}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.jms.JmsListener#setDestinationType(String) listener.destinationType}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.jms.JmsListener#setPersistent(String) listener.persistent}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.jms.JmsListener#setAcknowledgeMode(String) listener.acknowledgeMode}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.jms.JmsListener#setTransacted(boolean) listener.transacted}</td><td>&nbsp;</td><td>false</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.jms.JmsListener#setCommitOnState(String) listener.commitOnState}</td><td>&nbsp;</td><td>"success"</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.jms.JmsListener#setTimeOut(long) listener.timeOut}</td><td>&nbsp;</td><td>3000 [ms]</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.jms.JmsListener#setUseReplyTo(boolean) listener.useReplyTo}</td><td>&nbsp;</td><td>true</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.jms.JmsListener#setJmsRealm(String) listener.jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ISender sender}</td><td>specification of sender to send the replies with. Can be a JmsSender, to send replies to a queue</td></tr>
 * </table>
 * </p>
 * </p><p><b>Using transacted() and acknowledgement</b><br/>
 * If transacted() is true: it should ensure that a message is received and processed on a both or nothing basis. IBIS will commit
 * the the message, otherwise perform rollback. However, IBIS does not bring transactions within the adapters under transaction
 * control, compromising the idea of atomic transactions. If the Adapter returns a "success" state, the message will be acknowledged
 * or rolled back. In the roll-back situation messages sent to other destinations within the Pipeline are NOT rolled back! In the 
 * failure situation the message is therefore completely processed, and the roll back does not mean that the processing is rolled back!</p>
 * <p>
 * Setting {@link nl.nn.adapterframework.jms.JmsListener#setAcknowledgeMode(String) listener.acknowledgeMode} to "auto" means that messages are allways acknowledged (removed from
 * the queue, regardless of what the status of the Adapter is. "client" means that the message will only be removed from the queue
 * when the state of the Adapter equals the defined state for committing (specified by {@link nl.nn.adapterframework.jms.JmsListener#setCommitOnState(String) listener.commitOnState}).
 * The "dups" mode instructs the session to lazily acknowledge the delivery of the messages. This is likely to result in the
 * delivery of duplicate messages if JMS fails. It should be used by consumers who are tolerant in processing duplicate messages. In cases where the client
 * is tolerant of duplicate messages, some enhancement in performance can be achieved using this mode, since a session has lower overhead in trying to
 * prevent duplicate messages.
 * </p>
 * <p>The setting for {@link  nl.nn.adapterframework.jms.JmsListener#setAcknowledgeMode(String) listener.acknowledgeMode} will only be processed if 
 * the setting for {@link  nl.nn.adapterframework.jms.JmsListener#setTransacted(boolean) listener.transacted} is false.</p>
 * <p>If {@link  nl.nn.adapterframework.jms.JmsListener#setUseReplyTo(boolean) useReplyTo} is set and a replyTo-destination is
 * specified in the message, the JmsListener sends the result of the processing
 * in the pipeline to this destination. Otherwise the result is sent using the (optionally)
 * specified {@link  #setSender(ISender) Sender}, that in turn sends the message to
 * whatever it is configured to.</p>
 * @author     Gerrit van Brakel
 */
public class JmsReceiver extends PullingReceiverBase {
	public static final String version="$Id: JmsReceiver.java,v 1.1 2004-02-04 08:36:19 a1909356#db2admin Exp $";
	
public JmsReceiver() {
	super();
	setListener(new JmsListener());
}
public Object getRawMessage(HashMap threadContext) throws ListenerException {
    synchronized (this) {
        if (getRunState().equals(RunStateEnum.STARTED)) {
            return getListener().getRawMessage(threadContext);
        }
        return null;
    }
}
    public void setSender(ISender sender) {
        ((JmsListener)getListener()).setSender(sender);
        log.debug("["+getName()+"] registered sender ["+sender.getName()+"] with properties ["+sender.toString()+"]");
    }
}
