/*
 * $Log: GenericMessageSendingPipe.java,v $
 * Revision 1.1  2004-04-08 15:58:59  nnvznl01#l181303
 * Initial Version
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.ICorrelatedPullingListener;
import nl.nn.adapterframework.core.ISender;

/**
 * Plain extension to {@link MessageSendingPipe} that can be used directly in configurations.
 * Only extension is that the setters for listener and sender have been made public, and can therefore
 * be set from the configuration file.
 * For configuration options, see {@link MessageSendingPipe}.
 * 
 * @version Id
 * @author  Dennis van Loon
 * @since 4.1.1
 */

public class GenericMessageSendingPipe extends MessageSendingPipe {

	public static final String version="$Id: GenericMessageSendingPipe.java,v 1.1 2004-04-08 15:58:59 nnvznl01#l181303 Exp $";

	public void setListener(ICorrelatedPullingListener listener) {
		super.setListener(listener);
	}

	public void setSender(ISender sender) {
		super.setSender(sender);
	}


}
