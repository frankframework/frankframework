/**
 * $Log: IReceiverStatistics.java,v $
 * Revision 1.3  2004-03-26 10:42:50  NNVZNL01#L180564
 * added @version tag in javadoc
 *
 */
package nl.nn.adapterframework.core;

import java.util.Iterator;
/**
 * Methods for Receivers to supply statistics to a maintenance clients. Receivers indicate 
 * by implementing this interface that process- and idle statistics may be available for 
 * displaying.
 * 
 * @version Id
 *  
 *  @author Gerrit van Brakel
 */
public interface IReceiverStatistics  {
		public static final String version="$Id: IReceiverStatistics.java,v 1.3 2004-03-26 10:42:50 NNVZNL01#L180564 Exp $";

/**
 * @return an iterator of {@link nl.nn.adapterframework.util.StatisticsKeeper}s describing the durations of time that
 * the receiver has been waiting between messages.
 */
Iterator getIdleStatisticsIterator();
/**
 * @return an iterator of {@link nl.nn.adapterframework.util.StatisticsKeeper}s describing the durations of time that
 * the receiver has been waiting for the adapter to process messages.
 */
Iterator getProcessStatisticsIterator();
}
