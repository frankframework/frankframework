/*
 * $Log: IMessageWrapper.java,v $
 * Revision 1.4  2011-11-30 13:51:55  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2007/11/15 12:38:08  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed message wrapping
 *
 * Revision 1.1.2.1  2007/11/15 10:01:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed message wrappers
 *
 */
package nl.nn.adapterframework.core;

import java.util.Map;

/**
 * Interface for message wrappers.
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version Id
 */
public interface IMessageWrapper {
	
	public Map getContext();
	public String getId();
	public String getText();
}
