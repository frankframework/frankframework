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
package org.frankframework.scheduler;

import org.apache.commons.lang3.StringUtils;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;

import org.frankframework.scheduler.IbisJobDetail.JobType;
import org.frankframework.scheduler.job.DatabaseJob;

public class IbisJobBuilder {

	private JobKey key;
	private String description;
	private final Class<? extends Job> jobClass = ConfiguredJob.class;

	private final JobDataMap jobDataMap = new JobDataMap();
	private JobType jobType = JobType.CONFIGURATION;

	public static IbisJobBuilder newJob() {
		return new IbisJobBuilder();
	}

	public JobDetail build() {
		IbisJobDetail job = new IbisJobDetail();

		job.setJobClass(jobClass);
		job.setDescription(description);
		job.setKey(key);
		job.setDurability(false);
		job.setRequestsRecovery(false);
		job.setJobType(jobType);

		if(!jobDataMap.isEmpty())
			job.setJobDataMap(jobDataMap);

		return job;
	}

	public IbisJobBuilder withIdentity(String name, String group) {
		key = new JobKey(name, group);
		return this;
	}

	public IbisJobBuilder withDescription(String jobDescription) {
		if(StringUtils.isNotEmpty(jobDescription))
			this.description = jobDescription;

		return this;
	}

	public IbisJobBuilder setJobDef(AbstractJobDef jobDef) {
		jobDataMap.put(ConfiguredJob.JOBDEF_KEY, jobDef);
		return this;
	}

	public static IbisJobBuilder fromJobDef(AbstractJobDef jobDef) {
		IbisJobBuilder builder = new IbisJobBuilder();

		builder.setJobDef(jobDef);
		builder.withDescription(jobDef.getDescription());

		if(jobDef instanceof DatabaseJob) {
			builder.setJobType(JobType.DATABASE);
		}

		builder.withIdentity(jobDef.getName(), jobDef.getJobGroup());

		return builder;
	}

	public IbisJobBuilder setJobType(JobType jobType) {
		this.jobType = jobType;
		return this;
	}
}
