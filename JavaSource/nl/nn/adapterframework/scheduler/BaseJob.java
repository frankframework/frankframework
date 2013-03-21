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
/*
 * $Log: BaseJob.java,v $
 * Revision 1.7  2011-11-30 13:51:42  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:53  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.5  2008/09/04 13:27:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * restructured job scheduling
 *
 * Revision 1.4  2007/02/12 14:08:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 */
package nl.nn.adapterframework.scheduler;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
/**
 * Base class for jobs.
 * 
 * @author  Johan Verrips
 * @since   4.0
 * @version $Id$
 */
public abstract class BaseJob implements Job {
    protected Logger log=LogUtil.getLogger(this);

	public String getLogPrefix(JobExecutionContext context) {
		String instName = context.getJobDetail().getName();
		return "Job ["+instName+"] ";
	}

	public String getLogPrefix(JobDef jobDef) {
		String instName = jobDef.getName();
		return "Job ["+instName+"] ";
	}
}

