/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2021 WeAreFrank!

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
package nl.nn.adapterframework.statistics.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jdbc.CachedSideTable;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.JdbcFacade;
import nl.nn.adapterframework.jdbc.SideTable;
import nl.nn.adapterframework.statistics.ScalarMetricBase;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.statistics.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.statistics.jdbc.StatisticsKeeperStore.SessionInfo;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.Misc;

/**
 * StatisticsKeeperIterationHandler that stores all statisticsdata in a database.
 *
 * @author  Gerrit van Brakel
 * @since   4.9.8
 */
public class StatisticsKeeperStore extends JdbcFacade implements StatisticsKeeperIterationHandler<SessionInfo> {

	private SideTable instances=new CachedSideTable("ibisinstance", "instancekey", "name", "seq_ibisinstance");
	private SideTable hosts=    new CachedSideTable("ibishost",     "hostkey",     "name", "seq_ibishost");
	private SideTable statnames=new CachedSideTable("ibisstatname", "statnamekey", "name", "seq_ibisstatname");

	private StatGroupTable groups= new CachedStatGroupTable("ibisgroup", "groupkey", "parentgroup", "instancekey", "name", "type", "seq_ibisgroup");

	private String insertEventQueryInsertClause;
	private String insertEventQueryValuesClause;
	private String insertStatKeeperQuery;
	private String insertNumQuery;
	private String insertTimestampQuery;
	private String selectNextValueQuery;

	private int instanceKey;

	private final boolean trace=false;

	public StatisticsKeeperStore() {
		super();
		createQueries();
	}


	protected class SessionInfo {
		Connection connection;
		int groupKey;
		int eventKey;
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		createQueries();
		String instance=AppConstants.getInstance().getString("instance.name","");
		try (Connection connection = getConnection()) {
			instanceKey=instances.findOrInsert(connection,instance);
		} catch (JdbcException | SQLException e) {
			throw new ConfigurationException("could not find instancekey for instance ["+instance+"]",e);
		}
	}

	private void createQueries() {

		insertEventQueryInsertClause=
		"insert into ibisevent (" +
		"  eventkey" +
		", instancekey" +
		", hostkey" +
		", heapSize" +
		", totalMemory" +
		", timestamp" +
		", intervalstart ";
		insertEventQueryValuesClause=
		") values(?,?,?,?,?,?,?";

		insertStatKeeperQuery=
		"insert into ibisstatinfo (" +
		"  eventkey" +
		", groupkey" +
		", statnamekey" +
		", count" +
		", min" +
		", max" +
		", avg" +
		", stddev" +
		", sum" +
		", sumsq" +
//		", cump50" +
//		", cump90" +
//		", cump95" +
//		", cump98" +
		") values(?,?,?,?,?,?,?,?,?,?)";

		insertNumQuery="INSERT INTO ibisnuminfo (eventkey, groupkey, statnamekey, value) VALUES (?,?,?,?)";
		insertTimestampQuery="INSERT INTO ibisdateinfo (eventkey, groupkey, statnamekey, value) VALUES (?,?,?,?)";
		selectNextValueQuery="SELECT seq_ibisevent.nextval FROM DUAL";
	}

	private void addPeriodIndicator(List nameList, List valueList, Date now, String[][] periods, long allowedLength, String prefix, Date mark) {
		long intervalStart=mark.getTime();
		long intervalEnd=now.getTime();
		if ((intervalEnd-intervalStart)<=allowedLength) {
			Date midterm=new Date((intervalEnd>>1)+(intervalStart>>1));
			for (int i=0; i<periods.length; i++) {
				String[] periodPair=periods[i];
				nameList.add(prefix+periodPair[0]);
				valueList.add(DateUtils.format(midterm,periodPair[1]));
			}
		}
	}


	@Override
	public SessionInfo start(Date now, Date mainMark, Date detailMark) throws SenderException {
		List nameList=new LinkedList();
		List valueList=new LinkedList();
		now=new Date();
		SessionInfo sessionInfo = new SessionInfo();
		PreparedStatement stmt=null;
		long freeMem = Runtime.getRuntime().freeMemory();
		long totalMem = Runtime.getRuntime().totalMemory();
		addPeriodIndicator(nameList,valueList,now,new String[][]{PERIOD_FORMAT_HOUR,PERIOD_FORMAT_DATEHOUR},PERIOD_ALLOWED_LENGTH_HOUR,"s",mainMark);
		addPeriodIndicator(nameList,valueList,now,new String[][]{PERIOD_FORMAT_DAY,PERIOD_FORMAT_DATE,PERIOD_FORMAT_WEEKDAY},PERIOD_ALLOWED_LENGTH_DAY,"s",mainMark);
		addPeriodIndicator(nameList,valueList,now,new String[][]{PERIOD_FORMAT_WEEK,PERIOD_FORMAT_YEARWEEK},PERIOD_ALLOWED_LENGTH_WEEK,"s",mainMark);
		addPeriodIndicator(nameList,valueList,now,new String[][]{PERIOD_FORMAT_MONTH,PERIOD_FORMAT_YEARMONTH},PERIOD_ALLOWED_LENGTH_MONTH,"s",mainMark);
		addPeriodIndicator(nameList,valueList,now,new String[][]{PERIOD_FORMAT_YEAR},PERIOD_ALLOWED_LENGTH_YEAR,"s",mainMark);
		try {
			try (Connection connection = getConnection()) {
				sessionInfo.connection=connection;
				String hostname=Misc.getHostname();
				int hostKey=hosts.findOrInsert(connection,hostname);
				sessionInfo.eventKey=JdbcUtil.executeIntQuery(connection,selectNextValueQuery);

				String insertEventQuery=null;
				try {
					String insertClause=insertEventQueryInsertClause;
					String valuesClause=insertEventQueryValuesClause;
					for(Iterator it=nameList.iterator();it.hasNext();) {
						String name=(String)it.next();
						insertClause+=","+name;
						valuesClause+=",?";
					}
					insertEventQuery=insertClause+valuesClause+")";
					if (trace && log.isDebugEnabled()) log.debug("prepare and execute query ["+insertEventQuery+"]");
					stmt = connection.prepareStatement(insertEventQuery);
					int pos=1;
					stmt.setInt(pos++,sessionInfo.eventKey);
					stmt.setInt(pos++,instanceKey);
					stmt.setInt(pos++,hostKey);
					stmt.setLong(pos++,totalMem-freeMem);
					stmt.setLong(pos++,totalMem);
					stmt.setTimestamp(pos++,new Timestamp(now.getTime()));
					stmt.setTimestamp(pos++,new Timestamp(mainMark.getTime()));
					for(Iterator it=valueList.iterator();it.hasNext();) {
						String value=(String)it.next();
						stmt.setString(pos++,value);
					}
					stmt.execute();
				} catch (Exception e) {
					throw new JdbcException("could not execute query ["+insertEventQuery+"]",e);
				} finally {
					if (stmt!=null) {
						try {
							stmt.close();
						} catch (Exception e) {
							throw new JdbcException("could not close statement for query ["+insertEventQuery+"]",e);
						}
					}
				}

				return sessionInfo;
			}
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}

	@Override
	public void end(SessionInfo data) throws SenderException {
		SessionInfo sessionInfo = data;
		try {
			if (sessionInfo!=null && sessionInfo.connection!=null) {
				sessionInfo.connection.close();
			}
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}

	private void applyParam(PreparedStatement stmt, int pos, long value) throws SQLException {
		if (trace && log.isDebugEnabled()) log.debug("pos ["+pos+"] set long param ["+value+"]");
		if (value==Long.MAX_VALUE) {
			stmt.setNull(pos,Types.NUMERIC);
		} else {
			stmt.setLong(pos,value);
		}
	}
	private void applyParam(PreparedStatement stmt, int pos, double value) throws SQLException {
		if (Double.isNaN(value)) {
			if (trace && log.isDebugEnabled()) log.debug("pos ["+pos+"] set double param ["+value+"], setting to NULL");
			stmt.setNull(pos,Types.DOUBLE);
		} else {
			if (trace && log.isDebugEnabled()) log.debug("pos ["+pos+"] set double param ["+value+"]");
			stmt.setDouble(pos,value);
		}
	}

	@Override
	public void handleStatisticsKeeper(SessionInfo data, StatisticsKeeper sk) throws SenderException {
		SessionInfo sessionInfo = data;
		PreparedStatement stmt = null;

		int statnamekey=-1;
		try {
			statnamekey=statnames.findOrInsert(sessionInfo.connection,sk.getName());
			if (trace && log.isDebugEnabled()) log.debug("prepare and execute query ["+insertStatKeeperQuery+"]");
			stmt = sessionInfo.connection.prepareStatement(insertStatKeeperQuery);
			int pos=1;
			long count=sk.getCount();
			applyParam(stmt,pos++,sessionInfo.eventKey);
			applyParam(stmt,pos++,sessionInfo.groupKey);
			applyParam(stmt,pos++,statnamekey);
			applyParam(stmt,pos++,count);
			if (count==0) {
				stmt.setNull(pos++,Types.NUMERIC);
				stmt.setNull(pos++,Types.NUMERIC);
				stmt.setNull(pos++,Types.NUMERIC);
				stmt.setNull(pos++,Types.NUMERIC);
			} else {
				applyParam(stmt,pos++,sk.getMin());
				applyParam(stmt,pos++,sk.getMax());
				applyParam(stmt,pos++,sk.getAvg());
				if (count==1) {
					stmt.setNull(pos++,Types.NUMERIC);
				} else {
					applyParam(stmt,pos++,sk.getStdDev());
				}
			}
			applyParam(stmt,pos++,sk.getTotal());
			applyParam(stmt,pos++,sk.getTotalSquare());
			stmt.execute();
		} catch (Exception e) {
			throw new SenderException("could not execute query ["+insertStatKeeperQuery+"]",e);
		} finally {
			if (stmt!=null) {
				try {
					stmt.close();
				} catch (Exception e) {
					throw new SenderException("could not close statement for query ["+insertStatKeeperQuery+"]",e);
				}
			}
		}
	}

	@Override
	public void handleScalar(SessionInfo data, String scalarName, ScalarMetricBase<?> meter) throws SenderException {
		handleScalar(data, scalarName, meter.getValue());
	}

	@Override
	public void handleScalar(SessionInfo data, String scalarName, long value) throws SenderException {
		SessionInfo sessionInfo = (SessionInfo)data;
		PreparedStatement stmt = null;

		int statnamekey=-1;
		try {
			statnamekey=statnames.findOrInsert(sessionInfo.connection,scalarName);
			if (trace && log.isDebugEnabled()) log.debug("prepare and execute query ["+insertNumQuery+"] params ["+sessionInfo.eventKey+","+ sessionInfo.groupKey +","+ statnamekey+","+ value +"]");
			stmt = sessionInfo.connection.prepareStatement(insertNumQuery);
			stmt.setLong(1,sessionInfo.eventKey);
			stmt.setLong(2,sessionInfo.groupKey);
			stmt.setLong(3,statnamekey);
			stmt.setLong(4,value);
			stmt.execute();
		} catch (Exception e) {
			throw new SenderException("could not execute query ["+insertNumQuery+"] params ["+sessionInfo.eventKey+","+ sessionInfo.groupKey +","+ statnamekey+","+ value +"]",e);
		} finally {
			if (stmt!=null) {
				try {
					stmt.close();
				} catch (Exception e) {
					throw new SenderException("could not close statement for query ["+insertNumQuery+"] params ["+sessionInfo.eventKey+","+ sessionInfo.groupKey +","+ statnamekey+","+ value +"]",e);
				}
			}
		}
	}

	@Override
	public void handleScalar(SessionInfo data, String scalarName, Date value) throws SenderException {
		SessionInfo sessionInfo = (SessionInfo)data;
		PreparedStatement stmt = null;

		int statnamekey=-1;
		try {
			statnamekey=statnames.findOrInsert(sessionInfo.connection,scalarName);
			if (trace && log.isDebugEnabled()) log.debug("prepare and execute query ["+insertTimestampQuery+"] params ["+sessionInfo.eventKey+","+ sessionInfo.groupKey +","+ statnamekey+","+ (value==null?"null":DateUtils.format(value)) +"]");
			stmt = sessionInfo.connection.prepareStatement(insertTimestampQuery);
			stmt.setLong(1,sessionInfo.eventKey);
			stmt.setLong(2,sessionInfo.groupKey);
			stmt.setLong(3,statnamekey);
			if (value==null) {
				stmt.setNull(4,Types.TIMESTAMP);
			} else {
				stmt.setTimestamp(4,new Timestamp(value.getTime()));
			}
			stmt.execute();
		} catch (Exception e) {
			throw new SenderException("could not execute query ["+insertTimestampQuery+"] params ["+sessionInfo.eventKey+","+ sessionInfo.groupKey +","+ statnamekey+","+ (value==null?"null":DateUtils.format(value)) +"]",e);
		} finally {
			if (stmt!=null) {
				try {
					stmt.close();
				} catch (Exception e) {
					throw new SenderException("could not close statement for query ["+insertTimestampQuery+"] params ["+sessionInfo.eventKey+","+ sessionInfo.groupKey +","+ statnamekey+","+ (value==null?"null":DateUtils.format(value)) +"]",e);
				}
			}
		}
	}

	@Override
	public SessionInfo openGroup(SessionInfo parentData, String name, String type) throws SenderException {
		SessionInfo sessionInfo = (SessionInfo)parentData;
		int parentKey=sessionInfo.groupKey;
		int groupKey;
		try {
			groupKey=groups.findOrInsert(sessionInfo.connection,parentKey,instanceKey,name,type);
			SessionInfo groupData=new SessionInfo();
			groupData.connection=sessionInfo.connection;
			groupData.eventKey=sessionInfo.eventKey;
			groupData.groupKey=groupKey;
			return groupData;
		} catch (JdbcException e) {
			throw new SenderException(e);
		}
	}

	@Override
	public void closeGroup(SessionInfo data) throws SenderException {
		// nothing to do
	}

}
