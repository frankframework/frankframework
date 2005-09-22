/*
 * $Log: IfsaMessageWrapper.java,v $
 * Revision 1.1  2005-09-22 16:07:50  europe\L190409
 * introduction of IfsaMessageWrapper
 *
 */
package nl.nn.adapterframework.extensions.ifsa;

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
public class IfsaMessageWrapper implements Serializable {
	
	private HashMap context = new HashMap();
	private String text; 
	private String id; 
	
	public IfsaMessageWrapper(Object message, IListener listener) throws ListenerException  {
		super();
		text = listener.getStringFromRawMessage(message, context);
		id = listener.getIdFromRawMessage(message, context);
	}

	public HashMap getContext() {
		return context;
	}

	public String getId() {
		return id;
	}

	public String getText() {
		return text;
	}

}
