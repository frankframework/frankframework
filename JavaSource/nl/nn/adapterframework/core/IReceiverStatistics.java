package nl.nn.adapterframework.core;

import java.util.Iterator;
/**
 * Methods for Receivers to supply statistics to a maintenance clients. Receivers indicate 
 * by implementing this interface that process- and idle statistics may be available for 
 * displaying.
 * 
 * <p>$Id: IReceiverStatistics.java,v 1.2 2004-02-04 10:02:00 a1909356#db2admin Exp $</p>
 *  
 *  @author Gerrit van Brakel
 */
public interface IReceiverStatistics  {
		public static final String version="$Id: IReceiverStatistics.java,v 1.2 2004-02-04 10:02:00 a1909356#db2admin Exp $";

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
