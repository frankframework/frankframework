package nl.nn.adapterframework.core;

/**
 * The <code>HasPhysicalDestination</code> allows objects to declare that they have a physical destination.
 * This is used for instance in ShowConfiguration, to show the physical destination of receivers
 * that have one.
 * 
 * <p>$Id: HasPhysicalDestination.java,v 1.1 2004-03-23 15:58:02 L190409 Exp $</p>
 *
 * @author Gerrit van Brakel
 */
public interface HasPhysicalDestination extends INamedObject {
	public static final String version="$Id: HasPhysicalDestination.java,v 1.1 2004-03-23 15:58:02 L190409 Exp $";

	public String getPhysicalDestinationName();
}
