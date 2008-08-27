/*
 * $Log: HasStatistics.java,v $
 * Revision 1.2  2008-08-27 16:23:24  europe\L190409
 * added reset option to statisticsdump
 *
 * Revision 1.1  2008/05/14 09:29:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of interface HasStatistics
 *
 */
package nl.nn.adapterframework.util;

/**
 * Interface to be implemented by objects like Pipes or Senders that maintain additional statistics themselves.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public interface HasStatistics {
	public void iterateOverStatistics(StatisticsKeeperIterationHandler hski, Object data, boolean reset);
}
