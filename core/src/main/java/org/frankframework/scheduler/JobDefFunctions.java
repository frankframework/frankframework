/*
   Copyright 2019 Nationale-Nederlanden, 2021 - 2024 WeAreFrank!

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

import lombok.Getter;
import org.frankframework.doc.DocumentedEnum;
import org.frankframework.doc.EnumLabel;
import org.frankframework.scheduler.job.ActionJob;
import org.frankframework.scheduler.job.CheckReloadJob;
import org.frankframework.scheduler.job.CleanupDatabaseJob;
import org.frankframework.scheduler.job.CleanupFileSystemJob;
import org.frankframework.scheduler.job.ExecuteQueryJob;
import org.frankframework.scheduler.job.IJob;
import org.frankframework.scheduler.job.LoadDatabaseSchedulesJob;
import org.frankframework.scheduler.job.RecoverAdaptersJob;
import org.frankframework.scheduler.job.SendMessageJob;

/**
 * @author Niels Meijer
 */
public enum JobDefFunctions implements DocumentedEnum {
	@EnumLabel("StopAdapter") STOP_ADAPTER(ActionJob.class),
	@EnumLabel("StartAdapter") START_ADAPTER(ActionJob.class),
	@EnumLabel("StopReceiver") STOP_RECEIVER(ActionJob.class),
	@EnumLabel("StartReceiver") START_RECEIVER(ActionJob.class),
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
	private final @Getter Class<? extends IJob> jobClass;

	JobDefFunctions(Class<? extends IJob> jobClass) {
		this.jobClass = jobClass;
	}

	public boolean isNotEqualToAtLeastOneOf(JobDefFunctions... functions) {
		return !isEqualToAtLeastOneOf(functions);
	}

	public boolean isEqualToAtLeastOneOf(JobDefFunctions... functions) {
		for (JobDefFunctions func : functions) {
			if(this == func)
				return true;
		}
		return false;
	}
}
