/*
 * $Log: HasPhysicalDestination.java,v $
 * Revision 1.3  2004-03-30 07:29:54  L190409
 * updated javadoc
 *
 * Revision 1.2  2004/03/26 10:42:50  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 */
package nl.nn.adapterframework.core;

/**
 * The <code>HasPhysicalDestination</code> allows objects to declare that they have a physical destination.
 * This is used for instance in ShowConfiguration, to show the physical destination of receivers
 * that have one.
 * 
 * @version Id
 * @author Gerrit van Brakel
 */
public interface HasPhysicalDestination extends INamedObject {
	public static final String version="$Id: HasPhysicalDestination.java,v 1.3 2004-03-30 07:29:54 L190409 Exp $";

	public String getPhysicalDestinationName();
}
