/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.statistics;

import java.util.Date;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;

/**
 * Allows operations on iterations over all statistics keepers.
 * 
 * @author  Gerrit van Brakel
 * @since  
 * @version $Id$
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
