/**
 * $Log: IManagable.java,v $
 * Revision 1.3  2004-03-26 10:42:50  NNVZNL01#L180564
 * added @version tag in javadoc
 *
 */
package nl.nn.adapterframework.core;

import nl.nn.adapterframework.util.RunStateEnum;
/**
 * Models starting and stopping of objects that support such behaviour.
 * 
 * @versin $Id: IManagable.java,v 1.3 2004-03-26 10:42:50 NNVZNL01#L180564 Exp $
 * 
 * @author Gerrit van Brakel
 * @since 4.0
 */
public interface IManagable extends INamedObject {
		public static final String version="$Id: IManagable.java,v 1.3 2004-03-26 10:42:50 NNVZNL01#L180564 Exp $";

	
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
