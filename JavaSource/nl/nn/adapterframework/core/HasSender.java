/*
 * $Log: HasSender.java,v $
 * Revision 1.5  2004-03-30 07:29:59  L190409
 * updated javadoc
 *
 * Revision 1.4  2004/03/26 10:42:45  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 */
package nl.nn.adapterframework.core;

/**
 * The <code>HasSender</code> is allows objects to declare that they have a Sender.
 * This is used for instance in ShowConfiguration, to show the Senders of receivers
 * that have one
 * 
 * @version HasSender.java,v 1.3 2004/03/23 16:42:59 L190409 Exp $
 * @author Gerrit van Brakel
 */
public interface HasSender extends INamedObject {
	public static final String version="$Id: HasSender.java,v 1.5 2004-03-30 07:29:59 L190409 Exp $";

	public ISender getSender();
}
