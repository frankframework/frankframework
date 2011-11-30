/*
 * $Log: StatisticsKeeperLogger.java,v $
 * Revision 1.3  2011-11-30 13:51:48  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2009/12/29 14:25:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved statistics to separate package
 *
 * Revision 1.9  2009/08/26 15:43:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for configurable statisticsHandlers
 *
 * Revision 1.8  2009/06/05 07:37:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for adapter level only statistics
 *
 * Revision 1.7  2009/01/08 16:41:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
package nl.nn.adapterframework.statistics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.FileUtils;

/**
 * Logs statistics-keeper contents to log.
 * 
 * @author  Gerrit van Brakel
 * @since   4.4.3
 * @version Id
 */
public class StatisticsKeeperLogger extends StatisticsKeeperXmlBuilder {

	private String directory=null;
	private int retentionDays=-1;

	public void configure() throws ConfigurationException {
		super.configure();
		AppConstants ac = AppConstants.getInstance();
		if (directory==null)	{
			setDirectory(ac.getResolvedProperty("log.dir"));
		}
		if (retentionDays<0) {	
			setRetentionDays(ac.getInt("statistics.retention",7));
		}
	}


	public void end(Object data) {
		super.end(data);

		if (StringUtils.isNotEmpty(getDirectory())) {
			AppConstants ac = AppConstants.getInstance();		
			String filenamePattern=ac.getResolvedProperty("instance.name")+"-stats_";
			String extension=".log";
			File outfile=FileUtils.getWeeklyRollingFile(directory, filenamePattern, extension, retentionDays);

			FileWriter fw=null;
			try {
				fw = new FileWriter(outfile,true);
				fw.write(getXml(data).toXML());
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
	
	
	public void setRetentionDays(int i) {
		retentionDays = i;
	}
	public int getRetentionDays() {
		return retentionDays;
	}

	public void setDirectory(String string) {
		directory = string;
	}
	public String getDirectory() {
		return directory;
	}

}
