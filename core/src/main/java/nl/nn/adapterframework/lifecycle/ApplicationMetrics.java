/*
   Copyright 2020 WeAreFrank!

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
package nl.nn.adapterframework.lifecycle;

import java.util.Date;

import nl.nn.adapterframework.util.DateUtils;

/**
 * Singleton bean that keeps track of a Spring Application's uptime.
 *
 */
public class ApplicationMetrics {

	private final long uptime = System.currentTimeMillis();

	public Date getUptimeDate() {
		return new Date(uptime);
	}

	public String getUptime() {
		return getUptime(DateUtils.FORMAT_GENERICDATETIME);
	}

	public String getUptime(String dateFormat) {
		return DateUtils.format(getUptimeDate(), dateFormat);
	}
}
