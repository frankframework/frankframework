/*
 * $Log: TransactionalStorage.java,v $
 * Revision 1.4  2004-03-31 12:04:21  L190409
 * fixed javadoc
 *
 * Revision 1.3  2004/03/26 10:43:03  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.2  2004/03/26 09:50:52  Johan Verrips <johan.verrips@ibissource.org>
 * Updated javadoc
 *
 * Revision 1.1  2004/03/23 17:26:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * initial version
 *
 */
package nl.nn.adapterframework.receivers;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.ICorrelatedPullingListener;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.IXAEnabled;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;

import org.apache.log4j.Logger;

/**
 * Stores a message using a {@link ISender} and retrieves is back from a {@link ICorrelatedPullingListener listener}.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td><code>sender.*</td><td>any attribute of the sender instantiated by descendant classes</td><td>&nbsp;</td></tr>
 * <tr><td><code>listener.*</td><td>any attribute of the listener instantiated by descendant classes</td><td>&nbsp;</td></tr>
 * </table>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link ISender sender}</td><td>specification of sender to send messages with</td></tr>
 * <tr><td>{@link ICorrelatedPullingListener listener}</td><td>specification of listener to listen to for replies</td></tr>
 * </table>
 * </p>
 * @version Id
 * @author Gerrit van Brakel
 * @since 4.1
 */

public class TransactionalStorage implements ITransactionalStorage {
	public static final String version="$Id: TransactionalStorage.java,v 1.4 2004-03-31 12:04:21 L190409 Exp $";
    protected Logger log = Logger.getLogger(this.getClass());
	
    private ISender sender=null;
    private ICorrelatedPullingListener listener=null;
	private String name;


protected String getLogPrefix() {
	return "TransactionalStorage ["+getName()+"] ";
}

/**
 * Checks whether a sender is defined for this pipe.
 */
public void configure() throws ConfigurationException{
    if (getSender()==null){
       throw new ConfigurationException(getLogPrefix()+"has no sender defined");
    }
    if (getListener()==null){
       throw new ConfigurationException(getLogPrefix()+"has no listener defined");
    }
	if (getSender().isSynchronous()) {
    	throw new ConfigurationException(getLogPrefix()+"cannot have synchronous sender in TransactionalStorage");
   	}
    
    String senderName = getSender().getName();
    if (senderName == null || senderName.equals("")) {
        getSender().setName(getName()+"-sender");
    }
    getSender().configure();
    getListener().configure();
}


public boolean isTransacted() {
	return 
		getSender()!=null && 
		getSender() instanceof IXAEnabled && 
		((IXAEnabled)getSender()).isTransacted() &&
		getListener()!=null && 
		getListener() instanceof IXAEnabled && 
		((IXAEnabled)getListener()).isTransacted();
}

public ICorrelatedPullingListener getListener() {
	return listener;
}

public ISender getSender() {
	return sender;
}
protected void setListener(ICorrelatedPullingListener listener) {
	this.listener = listener;
	log.debug("pipe ["+getName()+" registered listener ["+ listener.toString()+ "]");
}

protected void setSender(ISender sender) {
    this.sender = sender;
    log.debug(
        "pipe ["
            + getName()
            + " registered sender ["
            + sender.getName()
            + "] with properties ["
            + sender.toString()
            + "]");
}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		getSender().setName("sender of ["+name+"]");
		if (getListener() instanceof INamedObject)  {
			((INamedObject) getListener()).setName("listener of ["+getName()+"]");
		}
	}

	public void close() throws SenderException {
	    log.info(getLogPrefix()+"is closing");
	    try {
		    getSender().close();
	    } catch (SenderException e) {
		    log.warn(getLogPrefix()+"exception closing sender", e);
	    }
	    if (getListener() != null) {
	        try {
	            log.info(getLogPrefix()+"is closing; closing listener");
	            getListener().close();
	        } catch (ListenerException e) {
	            log.warn(getLogPrefix()+"Exception closing listener", e);
	        }
	    }
	}

	public void open() throws SenderException, ListenerException {
        getSender().open();
        getListener().open();
	}

	public void deleteMessage(String messageId) throws ListenerException {
		ICorrelatedPullingListener l = getListener();
		Object rawmsg = null;
		
		try {
		   rawmsg = l.getRawMessage(messageId, null);
		} catch (TimeOutException e) {
		}

		if (rawmsg ==null) {
			throw new ListenerException("["+getName()+"] timeout deleting message with handle ["+messageId+"] from storage");
		}
	}

	public void storeMessage(String messageId, String message) throws SenderException {
		try {
			getSender().sendMessage(messageId, message);
		} catch (TimeOutException e) {
			throw new SenderException(e);
		}
	}

}
