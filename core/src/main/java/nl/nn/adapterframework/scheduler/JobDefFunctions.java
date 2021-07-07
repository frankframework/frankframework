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

import nl.nn.adapterframework.doc.DocumentedEnum;

public enum JobDefFunctions implements DocumentedEnum {
	STOP_ADAPTER("StopAdapter"),
	START_ADAPTER("StartAdapter"),
	STOP_RECEIVER("StopReceiver"),
	START_RECEIVER("StartReceiver"),
	SEND_MESSAGE("SendMessage"),
	QUERY("ExecuteQuery"),
	DUMPSTATS("dumpStatistics", true),
	DUMPSTATSFULL("dumpStatisticsFull", true),
	CLEANUPDB("cleanupDatabase", true),
	CLEANUPFS("cleanupFileSystem", true),
	RECOVER_ADAPTERS("recoverAdapters", true),
	CHECK_RELOAD("checkReload", true), 
	LOAD_DATABASE_SCHEDULES("loadDatabaseSchedules", true);

	private boolean servicejob = false;
	private final String label;

	private JobDefFunctions(String label) {
		this(label, false);
	}

	private JobDefFunctions(String label, boolean servicejob) {
		this.label = label;
		this.servicejob = servicejob;
	}

	@Override
	public String getLabel() {
		return label;
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
