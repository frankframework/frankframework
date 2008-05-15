/*
 * $Log: FixedResultSender.java,v $
 * Revision 1.2  2008-05-15 15:10:47  europe\L190409
 * moved implementation to senders package
 *
 * Revision 1.1  2007/05/01 14:10:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of FixedResultSender, modeled after FixedResult-pipe
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;

/**
 * FixedResultSender, same behaviour as {@link FixedResult}, but now as a ISender.
 * 
 * @deprecated This sender has been moved to the {@link nl.nn.adapterframework.senders.FixedResultSender senders}-package.
 * 
 * @author  Gerrit van Brakel
 * @since   4.6.0
 * @version Id
 */
public class FixedResultSender extends nl.nn.adapterframework.senders.FixedResultSender {

	public void configure() throws ConfigurationException {
		log.warn(getLogPrefix()+"The class ["+getClass().getName()+"] has been deprecated. Please change to ["+nl.nn.adapterframework.senders.FixedResultSender.class.getName()+"]");
		super.configure();
	}

}
