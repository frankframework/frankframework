/*
 * $Log: StatisticsKeeperIterationHandler.java,v $
 * Revision 1.4  2009-06-05 07:36:54  L190409
 * allow methods to throw SenderException
 * handle scalar now accepts only long and date values
 *
 * Revision 1.3  2008/05/14 09:30:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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

import java.util.Date;

import nl.nn.adapterframework.core.SenderException;

/**
 * Allows operations on iterations over all statistics keepers.
 * 
 * @author  Gerrit van Brakel
 * @since  
 * @version Id
 */
public interface StatisticsKeeperIterationHandler {

	public Object start() throws SenderException;
	public void end(Object data) throws SenderException;
	public void handleStatisticsKeeper(Object data, StatisticsKeeper sk) throws SenderException;
	public void handleScalar(Object data, String scalarName, long value) throws SenderException;
	public void handleScalar(Object data, String scalarName, Date value) throws SenderException;
	public Object openGroup(Object parentData, String name, String type) throws SenderException;
	public void  closeGroup(Object data) throws SenderException;
}
