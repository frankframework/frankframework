/*
 * $Log: StatisticsKeeperIterationHandler.java,v $
 * Revision 1.1  2005-12-28 08:31:33  europe\L190409
 * introduced StatisticsKeeper-iteration
 *
 */
package nl.nn.adapterframework.util;

/**
 * Allows operations on iterations over all statistics keepers.
 * 
 * @author  Gerrit van Brakel
 * @since  
 * @version Id
 */
public interface StatisticsKeeperIterationHandler {

	public Object start();
	public void end(Object data);
	public void handleStatisticsKeeperIteration(Object data, StatisticsKeeper sk);
	public void handleScalarIteration(Object data, String scalarName, long value);
	public Object openGroup(Object parentData, String name, String type);
	public void  closeGroup(Object data);
}
