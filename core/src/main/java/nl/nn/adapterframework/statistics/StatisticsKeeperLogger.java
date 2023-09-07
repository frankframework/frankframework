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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Logs statistics-keeper contents to log.
 * 
 * @author  Gerrit van Brakel
 * @since   4.4.3
 */
public class StatisticsKeeperLogger extends StatisticsKeeperXmlBuilder {

	private String directory=null;
	private int retentionDays=-1;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		AppConstants ac = AppConstants.getInstance();
		if (directory==null)	{
			setDirectory(ac.getProperty("log.dir"));
		}
		if (retentionDays<0) {
			setRetentionDays(ac.getInt("statistics.retention",7));
		}
	}


	@Override
	public void end(XmlBuilder data) {
		super.end(data);

		if (StringUtils.isNotEmpty(getDirectory())) {
			AppConstants ac = AppConstants.getInstance();
			String filenamePattern=ac.getProperty("instance.name.lc")+"-stats_";
			String extension=".log";
			File outfile=FileUtils.getWeeklyRollingFile(directory, filenamePattern, extension, retentionDays);

			FileWriter fw=null;
			try {
				fw = new FileWriter(outfile,true);
				fw.write(data.toXML());
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
