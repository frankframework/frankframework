/*
 * $Log: IBulkDataListener.java,v $
 * Revision 1.2  2008-07-24 12:31:45  europe\L190409
 * fix
 *
 * Revision 1.1  2008/07/24 12:03:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fix transactional FXF
 *
 */
package nl.nn.adapterframework.core;

import java.util.Map;

/**
 * Listener extension that allows to transfer of a lot of data, and do it within the transaction handling.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public interface IBulkDataListener extends IListener {

	/**
	 * Retrieves the bulk data associated with the message, stores it in a file or something similar.
	 * It returns the handle to the file as a result, and uses that as the message for the pipeline.
	 * @return input message for adapter.
	 */
	String retrieveBulkData(Object rawMessage, String message, Map context) throws ListenerException;

}
