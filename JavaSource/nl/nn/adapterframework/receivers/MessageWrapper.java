/*
 * $Log: MessageWrapper.java,v $
 * Revision 1.6  2011-11-30 13:51:54  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:43  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.4  2008/01/11 14:52:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * remove non serializable original message from context
 *
 * Revision 1.3  2007/11/15 12:38:08  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed message wrapping
 *
 * Revision 1.2  2007/10/08 12:24:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map where possible
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
import java.util.Map;

import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageWrapper;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.extensions.ifsa.jms.PushingIfsaProviderListener;

/**
 * Wrapper for messages that are not serializable.
 * 
 * @author  Gerrit van Brakel
 * @since   4.3
 * @version Id
 */
public class MessageWrapper implements Serializable, IMessageWrapper {

	static final long serialVersionUID = -8251009650246241025L;
	
	private Map context = new HashMap();
	private String text; 
	private String id; 
	
	public MessageWrapper()  {
		super();
	}
	public MessageWrapper(Object message, IListener listener) throws ListenerException  {
		this();
		text = listener.getStringFromRawMessage(message, context);
		Object rm = context.remove(PushingIfsaProviderListener.THREAD_CONTEXT_ORIGINAL_RAW_MESSAGE_KEY);
		id = listener.getIdFromRawMessage(message, context);
	}

	public Map getContext() {
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
