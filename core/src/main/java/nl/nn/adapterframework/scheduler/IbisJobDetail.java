/*
   Copyright 2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.scheduler;

import nl.nn.adapterframework.util.Locker;

import org.quartz.impl.JobDetailImpl;

public class IbisJobDetail extends JobDetailImpl {

	private JobType type = JobType.CONFIGURATION;

	public enum JobType {
		CONFIGURATION, DATABASE
	}

	public boolean compareWith(JobDef otherJobDef) {
		JobDef thisJobDef = getJobDef();

		//If the CRON expression is different in both jobs, it's not equal!
		if(!thisJobDef.getCronExpression().equals(otherJobDef.getCronExpression())) {
			return false;
		}
		//If the CRON expression is different in both jobs, it's not equal!
		if(thisJobDef.getInterval() != otherJobDef.getInterval()) {
			return false;
		}

		Locker thisLocker = thisJobDef.getLocker();
		Locker otherLocker = otherJobDef.getLocker();

		//If one is NULL but the other isn't, (locker has been removed or added, it's not equal!
		if((thisLocker == null && otherLocker != null) || (thisLocker != null && otherLocker == null)) {
			return false;
		}

		//If both contain a locker but the key is different, it's not equal!
		if(thisLocker != null && otherLocker != null && !(thisLocker.getObjectId().equals(otherLocker.getObjectId()))) {
			return false;
		}

		//If the message is different in both jobs, it's not equal!
		if(!thisJobDef.getMessage().equals(otherJobDef.getMessage())) {
			return false;
		}

		return true;
	}

	public void setJobType(JobType type) {
		this.type = type;
	}

	public JobType getJobType() {
		return type;
	}

	public JobDef getJobDef() {
		return (JobDef) this.getJobDataMap().get(ConfiguredJob.JOBDEF_KEY);
	}
}
