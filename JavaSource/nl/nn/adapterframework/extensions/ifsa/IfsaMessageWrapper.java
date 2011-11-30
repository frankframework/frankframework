/*
 * $Log: IfsaMessageWrapper.java,v $
 * Revision 1.5  2011-11-30 13:51:58  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:52  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.3  2007/11/15 12:38:08  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed message wrapping
 *
 * Revision 1.2.2.2  2007/11/15 10:01:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed message wrappers
 *
 * Revision 1.1  2005/09/22 16:07:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of IfsaMessageWrapper
 *
 */
package nl.nn.adapterframework.extensions.ifsa;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageWrapper;
import nl.nn.adapterframework.core.ListenerException;

/**
 * Wrapper for messages that are not serializable.
 * 
 * @author  Gerrit van Brakel
 * @since   4.3
 * @version Id
 */
public class IfsaMessageWrapper implements Serializable, IMessageWrapper {

	static final long serialVersionUID = 6543734487515204545L;
	
	private HashMap context = new HashMap();
	private String text; 
	private String id; 
	
	public IfsaMessageWrapper(Object message, IListener listener) throws ListenerException  {
		super();
		text = listener.getStringFromRawMessage(message, context);
		id = listener.getIdFromRawMessage(message, context);
	}

	public Map getContext() {
		return context;
	}

	public String getId() {
		return id;
	}

	public String getText() {
		return text;
	}

}
