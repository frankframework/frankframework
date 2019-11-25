package nl.nn.adapterframework.scheduler;

import nl.nn.adapterframework.configuration.IbisManager;

import org.apache.commons.lang3.StringUtils;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.impl.JobDetailImpl;

public class IbisJobBuilder {

	private JobKey key;
	private String description;
	private Class<? extends Job> jobClass = ConfiguredJob.class;

	private JobDataMap jobDataMap = new JobDataMap();
	private JobType jobType = JobType.CONFIGURATION;

	public static IbisJobBuilder newJob() {
		return new IbisJobBuilder();
	}

	public JobDetail build() {
		JobDetailImpl job = null;
		switch(jobType) {
			case DATABASE:
				job = new DatabaseJobDetail();
				break;
	
			default:
				job = new JobDetailImpl();
		}

		job.setJobClass(jobClass);
		job.setDescription(description);
		job.setKey(key); 
		job.setDurability(false);
		job.setRequestsRecovery(false);

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

	public IbisJobBuilder setJobDef(JobDef jobDef) {
		jobDataMap.put(ConfiguredJob.JOBDEF_KEY, jobDef);
		return this;
	}

	public static IbisJobBuilder fromJobDef(JobDef jobDef) {
		IbisJobBuilder builder = new IbisJobBuilder();

		builder.setJobDef(jobDef);
		builder.withDescription(jobDef.getDescription());

		if(jobDef instanceof DatabaseJobDef) {
			builder.setJobType(JobType.DATABASE);
		}
		else if(jobDef.getJobDefFunction().isServiceJob()) {
			builder.setJobType(JobType.SERVICE);
		}

		builder.withIdentity(jobDef.getName(), jobDef.getJobGroup());

		return builder;
	}

	public IbisJobBuilder setJobType(JobType jobType) {
		this.jobType = jobType;
		return this;
	}

	public IbisJobBuilder setIbisManager(IbisManager ibisManager) {
		jobDataMap.put(ConfiguredJob.MANAGER_KEY, ibisManager);
		return this;
	}

	public enum JobType {
		CONFIGURATION, SERVICE, DATABASE
	}
}
