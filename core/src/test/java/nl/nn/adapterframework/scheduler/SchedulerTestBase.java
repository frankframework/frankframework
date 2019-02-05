package nl.nn.adapterframework.scheduler;

import static org.quartz.JobBuilder.newJob;

import java.text.ParseException;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;

public abstract class SchedulerTestBase {

	static final String JAVALISTENER = "javaListener";
	static final String CORRELATIONID = "correlationId";
	static final String MESSAGE = "message";
	
	JobDataMap createDummyJobDataMap() {
		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put(JAVALISTENER, "javaListener");
		jobDataMap.put(CORRELATIONID, "correlationId");
		jobDataMap.put(MESSAGE, "message");
		
		return jobDataMap;
	}
	
	JobDetail createDummyJob() throws SchedulerException, ParseException {
		return createDummyJob("DummyJob", "DummyGroup");
	}
	
	JobDetail createDummyJob(String jobName, String groupName) throws SchedulerException, ParseException {
		JobDataMap jobDataMap = createDummyJobDataMap();
		
		return newJob(ServiceJob.class)
				.withIdentity(jobName, groupName)
				.usingJobData(jobDataMap)
				.build();
	}
}