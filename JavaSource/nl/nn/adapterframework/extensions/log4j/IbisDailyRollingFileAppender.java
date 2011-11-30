/*
 * $Log: IbisDailyRollingFileAppender.java,v $
 * Revision 1.3  2011-11-30 13:52:05  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2009/10/15 14:26:18  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * first version
 *
 */

package nl.nn.adapterframework.extensions.log4j;

import java.io.File;
import java.util.Date;

import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.WildCardFilter;

import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Extension of DailyRollingFileAppender with the facility to clean up files.
 *
 * @author  Peter Leeuwenburgh
 * @version Id
 */

public class IbisDailyRollingFileAppender extends DailyRollingFileAppender {
	final long millisPerDay = 24 * 60 * 60 * 1000;

	protected int retentionDays = 7;

	private String nextCheck =
		DateUtils.format(new Date(0), DateUtils.shortIsoFormat);

	public void subAppend(LoggingEvent event) {
		super.subAppend(event);

		String n = DateUtils.format(new Date(), DateUtils.shortIsoFormat);
		// cleaning up files is done once a day
		if (n.compareTo(nextCheck) > 0) {
			nextCheck = n;

			long deleteBefore =
				System.currentTimeMillis() - retentionDays * millisPerDay;

			String msgLogFileName = getFile();
			File msgLogFile = new File(msgLogFileName);
			String dirName = msgLogFile.getParent();
			String fileWithoutDirName = msgLogFile.getName();

			WildCardFilter filter =
				new WildCardFilter(fileWithoutDirName + ".*");
			File dir = new File(dirName);
			File[] files = dir.listFiles(filter);

			int count = (files == null ? 0 : files.length);
			for (int i = 0; i < count; i++) {
				File file = files[i];
				if (file.isDirectory()) {
					continue;
				}
				if (file.lastModified() < deleteBefore) {
					LogLog.debug("Deleting file " + file);
					file.delete();
				}
			}
		}
	}

	public void setRetentionDays(int retentionDays) {
		this.retentionDays = retentionDays;
	}

	public int getRetentionDays() {
		return retentionDays;
	}
}