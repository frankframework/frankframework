/*
 * $Log: StatisticsKeeperLogger.java,v $
 * Revision 1.7  2009-01-08 16:41:31  L190409
 * made statisticsfile weekly rolling
 *
 * Revision 1.6  2008/09/04 12:19:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * log to daily rolling file
 *
 * Revision 1.5  2008/07/24 12:24:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * log statistics as XML
 *
 * Revision 1.4  2008/05/14 09:30:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * simplified methodnames of StatisticsKeeperIterationHandler
 *
 * Revision 1.3  2007/02/12 14:12:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.2  2006/02/09 08:02:46  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * iterate over string scalars too
 *
 * Revision 1.1  2005/12/28 08:31:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduced StatisticsKeeper-iteration
 *
 */
package nl.nn.adapterframework.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import org.apache.log4j.Logger;

/**
 * Logs statistics-keeper contents to log.
 * 
 * @author  Gerrit van Brakel
 * @since   4.4.3
 * @version Id
 */
public class StatisticsKeeperLogger extends StatisticsKeeperXmlBuilder {
	protected Logger log = LogUtil.getLogger("nl.nn.adapterframework.statistics");

	public StatisticsKeeperLogger(Date now, Date mark) {
		super(now,mark);
	}

	public void end(Object data) {
		super.end(data);

		AppConstants ac = AppConstants.getInstance();		
		String directory = ac.getResolvedProperty("log.dir");
		int retentionDays=ac.getInt("statistics.retention",7);
		String filenamePattern=ac.getResolvedProperty("instance.name")+"-stats_";
		String extension=".log";
		File outfile=FileUtils.getWeeklyRollingFile(directory, filenamePattern, extension, retentionDays);

		FileWriter fw=null;
		try {
			fw = new FileWriter(outfile,true);
			fw.write(getXml().toXML());
			fw.write("\n");
		} catch (IOException e) {
			log.error("Could not write statistics to file ["+outfile.getPath()+"]",e);
		} finally {
			if (fw!=null) {
				try {
					fw.close();
				} catch (Exception e) {
					log.error("Could not close statistics file ["+outfile.getPath()+"]",e);
				}
			}
		}
	}
	
	
}
