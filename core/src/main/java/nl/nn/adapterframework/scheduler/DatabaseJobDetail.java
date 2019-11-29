package nl.nn.adapterframework.scheduler;

import nl.nn.adapterframework.util.Locker;

import org.quartz.impl.JobDetailImpl;

public class DatabaseJobDetail extends JobDetailImpl {

	public boolean compareWith(DatabaseJobDef otherJobDef) {
		DatabaseJobDef thisJobDef = getDatabaseJobDef();

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

	public DatabaseJobDef getDatabaseJobDef() {
		return (DatabaseJobDef) this.getJobDataMap().get(ConfiguredJob.JOBDEF_KEY);
	}
}
