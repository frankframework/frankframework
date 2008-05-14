/*
 * $Log: StatisticsKeeperIterationHandler.java,v $
 * Revision 1.3  2008-05-14 09:30:33  europe\L190409
 * simplified methodnames of StatisticsKeeperIterationHandler
 *
 * Revision 1.2  2006/02/09 08:02:46  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * iterate over string scalars too
 *
 * Revision 1.1  2005/12/28 08:31:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
	public void handleStatisticsKeeper(Object data, StatisticsKeeper sk);
	public void handleScalar(Object data, String scalarName, long value);
	public void handleScalar(Object data, String scalarName, String value);
	public Object openGroup(Object parentData, String name, String type);
	public void  closeGroup(Object data);
}
