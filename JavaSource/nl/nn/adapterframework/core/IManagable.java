/*
 * $Log: IManagable.java,v $
 * Revision 1.4  2004-03-30 07:29:54  L190409
 * updated javadoc
 *
 * Revision 1.3  2004/03/26 10:42:50  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 */
package nl.nn.adapterframework.core;

import nl.nn.adapterframework.util.RunStateEnum;
/**
 * Models starting and stopping of objects that support such behaviour.
 * 
 * @version Id
 * @author Gerrit van Brakel
 * @since 4.0
 */
public interface IManagable extends INamedObject {
		public static final String version="$Id: IManagable.java,v 1.4 2004-03-30 07:29:54 L190409 Exp $";

	
/**
 * returns the runstate of the object.
 * Possible values are defined by {@link RunStateEnum}.
 */
RunStateEnum getRunState();
/**
 * Instruct the object that implements <code>IManagable</code> to start working.
 * The method does not wait for completion of the command; at return of this method, 
 * the object might be still in the STARTING-runstate
 */
void startRunning();
/**
 * Instruct the object that implements <code>IManagable</code> to stop working. 
 * The method does not wait for completion of the command; at return of this method, 
 * the object might be still in the STOPPING-runstate
 */
void stopRunning();
}
