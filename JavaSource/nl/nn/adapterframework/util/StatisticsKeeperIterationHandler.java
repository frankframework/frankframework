/*
 * $Log: StatisticsKeeperIterationHandler.java,v $
 * Revision 1.5  2009-08-26 15:40:07  L190409
 * support for separated adapter-only and detailed statistics
 *
 * Revision 1.4  2009/06/05 07:36:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;

/**
 * Allows operations on iterations over all statistics keepers.
 * 
 * @author  Gerrit van Brakel
 * @since  
 * @version Id
 */
public interface StatisticsKeeperIterationHandler {

	public static final long PERIOD_ALLOWED_LENGTH_HOUR=1100*60*60; // 10% extra
	public static final long PERIOD_ALLOWED_LENGTH_DAY=PERIOD_ALLOWED_LENGTH_HOUR*24;
	public static final long PERIOD_ALLOWED_LENGTH_WEEK=PERIOD_ALLOWED_LENGTH_DAY*7;
	public static final long PERIOD_ALLOWED_LENGTH_MONTH=PERIOD_ALLOWED_LENGTH_DAY*31;
	public static final long PERIOD_ALLOWED_LENGTH_YEAR=PERIOD_ALLOWED_LENGTH_DAY*366;

	public static final String[] PERIOD_FORMAT_HOUR={"hour","HH"};
	public static final String[] PERIOD_FORMAT_DATEHOUR={"datehour","yyyy-MM-dd HH"};
	public static final String[] PERIOD_FORMAT_DAY={"day","dd"};
	public static final String[] PERIOD_FORMAT_DATE={"date","yyyy-MM-dd"};
	public static final String[] PERIOD_FORMAT_WEEKDAY={"weekday","E"};
	public static final String[] PERIOD_FORMAT_WEEK={"week","ww"};
	public static final String[] PERIOD_FORMAT_YEARWEEK={"yearweek","yyyy'W'ww"};
	public static final String[] PERIOD_FORMAT_MONTH={"month","MM"};
	public static final String[] PERIOD_FORMAT_YEARMONTH={"yearmonth","yyyy-MM"};
	public static final String[] PERIOD_FORMAT_YEAR={"year","yyyy"};


	public void configure() throws ConfigurationException;
	public Object start(Date now, Date mainMark, Date detailMark) throws SenderException;
	public void end(Object data) throws SenderException;
	public void handleStatisticsKeeper(Object data, StatisticsKeeper sk) throws SenderException;
	public void handleScalar(Object data, String scalarName, long value) throws SenderException;
	public void handleScalar(Object data, String scalarName, Date value) throws SenderException;
	public Object openGroup(Object parentData, String name, String type) throws SenderException;
	public void  closeGroup(Object data) throws SenderException;
}
