/*
 * $Log: GenericPullingReceiver.java,v $
 * Revision 1.2  2004-03-26 10:43:03  NNVZNL01#L180564
 * added @version tag in javadoc
 *
 * Revision 1.1  2004/03/23 17:24:08  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * initial version
 *
 */
package nl.nn.adapterframework.receivers;

import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ITransactionalStorage;

/**
 * Plain extension to {@link PullingReceiverBase} that can be used directly in configurations.
 * Only extension is that the setters for its three worker-objects are public. 
 * @version Id
 * @author  Gerrit van Brakel
 * @since 4.1
 */
public class GenericPullingReceiver extends PullingReceiverBase {
	public static final String version="$Id: GenericPullingReceiver.java,v 1.2 2004-03-26 10:43:03 NNVZNL01#L180564 Exp $";

	public void setListener(IPullingListener listener) {
		super.setListener(listener);
	}
	public void setInProcessStorage(ITransactionalStorage inProcessStorage) {
		super.setInProcessStorage(inProcessStorage);
	}
	public void setErrorSender(ISender errorSender) {
		super.setErrorSender(errorSender);
	}
}
