package nl.nn.adapterframework.receivers;

import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.jms.JmsMessageListener;

import java.util.HashMap;

/**
 * A {@link PullingReceiverBase PullingReceiver} that uses a {@link JmsMessageListener} as its listener,
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
 * <tr><td>{@link JmsMessageListener#setDestinationName(String) listener.destinationName}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link JmsMessageListener#setDestinationType(String) listener.destinationType}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link JmsMessageListener#setPersistent(String) listener.persistent}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link JmsMessageListener#setAcknowledgeMode(String) listener.acknowledgeMode}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link JmsMessageListener#setJmsRealm(String) listener.jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link ISender sender}</td><td>specification of queue to send the replies to</td></tr>
 * </table>
 * </p>
 * </p>
 * @deprecated This class is deprecated, as it uses the deprecated class {@link JmsMessageListener}. Please use 
 *             {@link JmsReceiver} instead.
 *
 * @author     Gerrit van Brakel
 */
public class JmsMessageReceiver extends PullingReceiverBase {
	public static final String version="$Id: JmsMessageReceiver.java,v 1.1 2004-02-04 08:36:19 a1909356#db2admin Exp $";
	
public JmsMessageReceiver() {
	super();
	setListener(new JmsMessageListener());
	log.warn("A deprecated version of JmsMessageReceiver is used. Use JmsReceiver instead");
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
        ((JmsMessageListener)getListener()).setSender(sender);
        log.debug("["+getName()+"] registered sender ["+sender.getName()+"] with properties ["+sender.toString()+"]");
    }
}
