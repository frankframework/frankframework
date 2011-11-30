/*
 * $Log: CommandSender.java,v $
 * Revision 1.6  2011-11-30 13:51:50  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:45  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.4  2008/12/30 17:01:12  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added configuration warnings facility (in Show configurationStatus)
 *
 * Revision 1.3  2008/11/26 09:38:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Fixed warning message in deprecated classes
 *
 * Revision 1.2  2008/08/06 16:38:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved from pipes to senders package
 *
 * Revision 1.1  2008/02/13 12:56:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;

/**
 * Sender that executes either its input or a fixed line, with all parametervalues appended, as a command.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.CommandSender</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>&nbsp;</td><td>the values of all parameters present are concatenated to the command line</td></tr>
 * </table>
 * </p>
 * 
 * @version Id
 * @since   4.8
 * @author  Gerrit van Brakel
 * @deprecated Please replace with nl.nn.adapterframework.senders.CommandSender
 */
public class CommandSender extends nl.nn.adapterframework.senders.CommandSender {

	public void configure() throws ConfigurationException {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = getLogPrefix()+"The class ["+getClass().getName()+"] has been deprecated. Please change to ["+getClass().getSuperclass().getName()+"]";
		configWarnings.add(log, msg);
		super.configure();
	}
}
