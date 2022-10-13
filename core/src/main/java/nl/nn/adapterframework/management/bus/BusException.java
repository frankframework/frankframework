package nl.nn.adapterframework.management.bus;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Serialized and send as an ExceptionMessage over the Spring Bus
 */
public class BusException extends RuntimeException {
	private static final Logger LOG = LogUtil.getLogger(BusException.class);

	private static final long serialVersionUID = 1L;

	/**
	 * Seen as WARNING
	 */
	public BusException(String message) {
		this(message, null);
	}

	/**
	 * Seen as ERROR
	 * Stacktrace information is logged but not passed to the parent to limit sensitive information being sent over the 'bus'.
	 */
	public BusException(String message, Exception exception) {
		super(new IbisException(message, exception).getMessage());
		if(exception == null) {
			LOG.warn(super.getMessage()); // expanded message is logged directly
		} else {
			LOG.error(message, exception); // normal message, expanded by printing the stacktrace
		}
	}
}
