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
package nl.nn.adapterframework.scheduler;

import lombok.Getter;
import nl.nn.adapterframework.doc.DocumentedEnum;
import nl.nn.adapterframework.doc.EnumLabel;
import nl.nn.adapterframework.scheduler.job.CheckReloadJob;
import nl.nn.adapterframework.scheduler.job.CleanupDatabaseJob;
import nl.nn.adapterframework.scheduler.job.CleanupFileSystemJob;
import nl.nn.adapterframework.scheduler.job.ExecuteQueryJob;
import nl.nn.adapterframework.scheduler.job.IJob;
import nl.nn.adapterframework.scheduler.job.IbisActionJob;
import nl.nn.adapterframework.scheduler.job.LoadDatabaseSchedulesJob;
import nl.nn.adapterframework.scheduler.job.RecoverAdaptersJob;
import nl.nn.adapterframework.scheduler.job.SendMessageJob;

/**
 * @author Niels Meijer
 */
public enum JobDefFunctions implements DocumentedEnum {
	@EnumLabel("StopAdapter") STOP_ADAPTER(IbisActionJob.class),
	@EnumLabel("StartAdapter") START_ADAPTER(IbisActionJob.class),
	@EnumLabel("StopReceiver") STOP_RECEIVER(IbisActionJob.class),
	@EnumLabel("StartReceiver") START_RECEIVER(IbisActionJob.class),
	@EnumLabel("SendMessage") SEND_MESSAGE(SendMessageJob.class),
	@EnumLabel("ExecuteQuery") QUERY(ExecuteQueryJob.class),
	@EnumLabel("cleanupDatabase") CLEANUPDB(CleanupDatabaseJob.class),
	@EnumLabel("cleanupFileSystem") CLEANUPFS(CleanupFileSystemJob.class),
	@EnumLabel("recoverAdapters") RECOVER_ADAPTERS(RecoverAdaptersJob.class),
	@EnumLabel("checkReload") CHECK_RELOAD(CheckReloadJob.class),
	@EnumLabel("loadDatabaseSchedules") LOAD_DATABASE_SCHEDULES(LoadDatabaseSchedulesJob.class);

	/**
	 * Should never return NULL
	 */
	private @Getter Class<? extends IJob> jobClass = null;

	private JobDefFunctions(Class<? extends IJob> jobClass) {
		this.jobClass = jobClass;
	}

	public boolean isNotEqualToAtLeastOneOf(JobDefFunctions... functions) {
		return !isEqualToAtLeastOneOf(functions);
	}

	public boolean isEqualToAtLeastOneOf(JobDefFunctions... functions) {
		boolean equals = false;
		for (JobDefFunctions func : functions) {
			if(super.equals(func))
				equals = true;
		}
		return equals;
	}
}
