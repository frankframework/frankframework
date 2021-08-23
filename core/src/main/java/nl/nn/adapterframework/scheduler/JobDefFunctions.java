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

import nl.nn.adapterframework.doc.DocumentedEnum;
import nl.nn.adapterframework.doc.EnumLabel;

public enum JobDefFunctions implements DocumentedEnum {
	@EnumLabel("StopAdapter") STOP_ADAPTER(),
	@EnumLabel("StartAdapter") START_ADAPTER(),
	@EnumLabel("StopReceiver") STOP_RECEIVER(),
	@EnumLabel("StartReceiver") START_RECEIVER(),
	@EnumLabel("SendMessage") SEND_MESSAGE(),
	@EnumLabel("ExecuteQuery") QUERY(),
	@EnumLabel("dumpStatistics") DUMPSTATS(true),
	@EnumLabel("dumpStatisticsFull") DUMPSTATSFULL(true),
	@EnumLabel("cleanupDatabase") CLEANUPDB(true),
	@EnumLabel("cleanupFileSystem") CLEANUPFS(true),
	@EnumLabel("recoverAdapters") RECOVER_ADAPTERS(true),
	@EnumLabel("checkReload") CHECK_RELOAD(true), 
	@EnumLabel("loadDatabaseSchedules") LOAD_DATABASE_SCHEDULES(true);

	private boolean servicejob = false;

	private JobDefFunctions() {
		this(false);
	}

	private JobDefFunctions(boolean servicejob) {
		this.servicejob = servicejob;
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

	/**
	 * Application related jobs, scheduled by the IBIS it selves for maintenance and performance enhancements
	 */
	public boolean isServiceJob() {
		return servicejob;
	}
}
