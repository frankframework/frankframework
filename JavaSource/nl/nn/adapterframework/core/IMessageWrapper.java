/*
 * $Log: IMessageWrapper.java,v $
 * Revision 1.2  2007-11-15 12:38:08  europe\L190409
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
