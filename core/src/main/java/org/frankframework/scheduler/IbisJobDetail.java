/*
   Copyright 2019 Nationale-Nederlanden, 2021 WeAreFrank!

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
package org.frankframework.scheduler;

import org.apache.commons.lang3.StringUtils;
import org.quartz.impl.JobDetailImpl;

import org.frankframework.scheduler.job.IJob;
import org.frankframework.scheduler.job.SendMessageJob;
import org.frankframework.util.Locker;

public class IbisJobDetail extends JobDetailImpl {

	private JobType type = JobType.CONFIGURATION;

	public enum JobType {
		CONFIGURATION, DATABASE
	}

	public boolean compareWith(IJob otherJobDef) {
		IJob thisJobDef = getJobDef();

		//If the CRON expression is different in both jobs, it's not equal!
		if (!StringUtils.equals(thisJobDef.getCronExpression(), otherJobDef.getCronExpression())) {
			return false;
		}

		//If the Interval expression is different in both jobs, it's not equal!
		if (thisJobDef.getInterval() != otherJobDef.getInterval()) {
			return false;
		}

		Locker thisLocker = thisJobDef.getLocker();
		Locker otherLocker = otherJobDef.getLocker();

		//If one is NULL but the other isn't, (locker has been removed or added, it's not equal!
		if((thisLocker == null && otherLocker != null) || (thisLocker != null && otherLocker == null)) {
			return false;
		}

		//If both contain a locker but the key is different, it's not equal!
		if (thisLocker != null && otherLocker != null && !StringUtils.equals(thisLocker.getObjectId(), otherLocker.getObjectId())) {
			return false;
		}

		//If at this point the message is equal in both jobs, the jobs are equal!
		if(thisJobDef instanceof SendMessageJob job1 && otherJobDef instanceof SendMessageJob job2) {
			String msg1 = job1.getMessage();
			String msg2 = job2.getMessage();
			return StringUtils.equals(msg1, msg2);
		}

		return true;
	}

	public void setJobType(JobType type) {
		this.type = type;
	}

	public JobType getJobType() {
		return type;
	}

	public IJob getJobDef() {
		return (IJob) this.getJobDataMap().get(ConfiguredJob.JOBDEF_KEY);
	}
}
