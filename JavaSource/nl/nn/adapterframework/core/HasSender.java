package nl.nn.adapterframework.core;

/**
 * The <code>HasSender</code> is allows objects to declare that they have a Sender.
 * This is used for instance in ShowConfiguration, to show the Senders of receivers
 * that have one
 *
 * @author Gerrit van Brakel
 */
public interface HasSender extends INamedObject {
		public static final String version="$Id: HasSender.java,v 1.1 2004-02-04 08:36:10 a1909356#db2admin Exp $";

	public ISender getSender();
}
