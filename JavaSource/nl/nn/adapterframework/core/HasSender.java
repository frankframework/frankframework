package nl.nn.adapterframework.core;

/**
 * The <code>HasSender</code> is allows objects to declare that they have a Sender.
 * This is used for instance in ShowConfiguration, to show the Senders of receivers
 * that have one
 * 
 * <p>$Id: HasSender.java,v 1.3 2004-03-23 16:42:59 L190409 Exp $</p>
 *
 * @author Gerrit van Brakel
 */
public interface HasSender extends INamedObject {
	public static final String version="$Id: HasSender.java,v 1.3 2004-03-23 16:42:59 L190409 Exp $";

	public ISender getSender();
}
