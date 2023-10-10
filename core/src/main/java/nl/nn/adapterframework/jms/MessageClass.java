package nl.nn.adapterframework.jms;

import nl.nn.adapterframework.stream.Message;

/**
 * The JMS {@link javax.jms.Message} class for the outgoing message.
 * Currently supported are TEXT for JMS {@link javax.jms.TextMessage},
 * BYTES for JMS {@link javax.jms.BytesMessage}, or AUTO for auto-determination
 * based on whether the input {@link Message} is binary or character.
 */
public enum MessageClass {
	/**
	 * Automatically determine the type of the outgoing {@link javax.jms.Message} based
	 * on the value of {@link Message#isBinary()}.
	 */
	AUTO,
	/**
	 * Create the outgoing message as {@link javax.jms.TextMessage}.
	 */
	TEXT,
	/**
	 * Create the outgoing message as {@link javax.jms.BytesMessage}.
	 */
	BYTES
}
