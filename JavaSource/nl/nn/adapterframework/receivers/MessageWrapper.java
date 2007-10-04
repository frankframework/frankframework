/*
 * $Log: MessageWrapper.java,v $
 * Revision 1.1.2.1  2007-10-04 13:32:32  europe\L190409
 * synchronize with HEAD (4.7.0)
 *
 * Revision 1.1  2007/09/13 09:08:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * move message wrapper from ifsa to receivers
 *
 * Revision 1.1  2005/09/22 16:07:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of IfsaMessageWrapper
 *
 */
package nl.nn.adapterframework.receivers;

import java.io.Serializable;
import java.util.HashMap;

import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.ListenerException;

/**
 * Wrapper for messages that are not serializable.
 * 
 * @author  Gerrit van Brakel
 * @since   4.3
 * @version Id
 */
public class MessageWrapper implements Serializable {
	
	private HashMap context = new HashMap();
	private String text; 
	private String id; 
	
	public MessageWrapper()  {
		super();
	}
	public MessageWrapper(Object message, IListener listener) throws ListenerException  {
		this();
		text = listener.getStringFromRawMessage(message, context);
		id = listener.getIdFromRawMessage(message, context);
	}

	public HashMap getContext() {
		return context;
	}

	public void setId(String string) {
		id = string;
	}
	public String getId() {
		return id;
	}

	public void setText(String string) {
		text = string;
	}
	public String getText() {
		return text;
	}
}
