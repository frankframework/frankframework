package nl.nn.adapterframework.core;

/**
 * The <code>HasSender</code> is allows objects to declare that they have a Sender.
 * This is used for instance in ShowConfiguration, to show the Senders of receivers
 * that have one
 * 
 * <p>$Id: HasSender.java,v 1.2 2004-02-04 10:01:58 a1909356#db2admin Exp $</p>
 *
 * @author Gerrit van Brakel
 */
public interface HasSender extends INamedObject {
		public static final String version="$Id: HasSender.java,v 1.2 2004-02-04 10:01:58 a1909356#db2admin Exp $";

	public ISender getSender();
}
