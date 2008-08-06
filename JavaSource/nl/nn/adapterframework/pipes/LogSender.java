/*
 * $Log: LogSender.java,v $
 * Revision 1.7  2008-08-06 16:38:21  europe\L190409
 * moved from pipes to senders package
 *
 * Revision 1.6  2007/09/13 09:09:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * return message instead of correlationid
 *
 * Revision 1.5  2007/02/12 14:02:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.4  2006/06/14 09:50:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * avoid null pointer exception when prc==null
 *
 * Revision 1.3  2005/12/28 08:38:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected typo in attributename
 *
 * Revision 1.2  2005/10/24 09:59:24  John Dekker <john.dekker@ibissource.org>
 * Add support for pattern parameters, and include them into several listeners,
 * senders and pipes that are file related
 *
 * Revision 1.1  2005/06/20 09:05:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of LogSender
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;

/**
 * Sender that just logs its message.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setLogLevel(String) logLevel}</td><td>level on which messages are logged</td><td>info</td></tr>
 * <tr><td>{@link #setLogCategory(String) logCategory}</td><td>category under which messages are logged</td><td>name of the sender</td></tr>
 * </table>
 * 
 * @author Gerrit van Brakel
 * @since  4.3
 * @version Id
 * @deprecated Please replace with nl.nn.adapterframework.senders.LogSender
 */
public class LogSender extends nl.nn.adapterframework.senders.LogSender {

	public void configure() throws ConfigurationException {
		log.warn(getLogPrefix()+"The class ["+getClass().getName()+"] has been deprecated. Please change to ["+super.getClass().getName()+"]");
		super.configure();
	}
}
