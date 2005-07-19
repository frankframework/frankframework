/*
 * $Log: GenericReceiver.java,v $
 * Revision 1.3  2005-07-19 15:27:45  europe\L190409
 * added errorStorage nested element
 *
 * Revision 1.2  2004/10/05 09:59:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused code
 *
 * Revision 1.1  2004/08/03 13:04:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of GenericReceiver
 *
 * Revision 1.3  2004/03/30 07:30:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.2  2004/03/26 10:43:03  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.1  2004/03/23 17:24:08  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * initial version
 *
 */
package nl.nn.adapterframework.receivers;

import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ITransactionalStorage;

/**
 * Plain extension of {@link ReceiverBase} that can be used directly in configurations.
 * Only extension is that the setters for its three worker-objects are public, and can therefore
 * be set from the configuration file.
 * For configuration options, see {@link ReceiverBase}.
 * 
 * @version Id
 * @author  Gerrit van Brakel
 * @since   4.1
 */
public class GenericReceiver extends ReceiverBase {
	public static final String version="$RCSfile: GenericReceiver.java,v $ $Revision: 1.3 $ $Date: 2005-07-19 15:27:45 $";

	public void setListener(IListener listener) {
		super.setListener(listener);
	}
	public void setInProcessStorage(ITransactionalStorage inProcessStorage) {
		super.setInProcessStorage(inProcessStorage);
	}
	public void setErrorSender(ISender errorSender) {
		super.setErrorSender(errorSender);
	}			
	public void setErrorStorage(ITransactionalStorage errorStorage) {
		super.setErrorStorage(errorStorage);
	}
	
	public void setSender(ISender sender) {
		super.setSender(sender);
	}
}
