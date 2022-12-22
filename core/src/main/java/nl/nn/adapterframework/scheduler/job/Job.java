/*
   Copyright 2021, 2022 WeAreFrank!

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
package nl.nn.adapterframework.scheduler.job;

import org.apache.commons.lang3.NotImplementedException;

import lombok.Getter;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.doc.Mandatory;
import nl.nn.adapterframework.scheduler.JobDef;
import nl.nn.adapterframework.scheduler.JobDefFunctions;

/**
 * Placeholder class to allow old-school <code>&lt;job function='SendMessage' /&gt;</code> in the new Frank!Config XSD.
 *
 * @author Niels Meijer
 */
// Should never be instantiated directly. See {@link JobFactory} and {@link JobDefFunctions} for more information.
public class Job extends JobDef {

	private @Getter JobDefFunctions function;

	@Override
	public void execute() throws JobExecutionException, TimeoutException {
		throw new NotImplementedException(); // will be replaced by appropriate executor class in JobFactory, based on function
	}


	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		if(function != null) builder.append(" function ["+function+"]");
		return builder.toString();
	}

	@Mandatory
	public void setFunction(JobDefFunctions function) {
		this.function = function;
	}

}
