/*
   Copyright 2021 - 2024 WeAreFrank!

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
package org.frankframework.scheduler.job;

import org.apache.commons.lang3.NotImplementedException;

import lombok.Getter;

import org.frankframework.doc.Mandatory;
import org.frankframework.scheduler.AbstractJobDef;
import org.frankframework.scheduler.JobDefFunctions;

/**
 * Placeholder class to allow legacy configuration notations <code>&lt;job function='SendMessage' /&gt;</code> in the new Frank!Config XSD.
 * <p>
 * The attribute {@literal function} has been removed in favor of explicit JobDefinitions such as: {@literal SendMessageJob},
 * {@literal ExecuteQueryJob} and {@literal ActionJob}.
 * Using the new elements enables the use of auto-completion for the specified type.
 * 
 * @ff.info The job function {@literal IbisActionJob} has been renamed to {@literal ActionJob}.
 * @ff.danger This element in combination with the attribute {@literal function} has been removed.
 *
 * @author Niels Meijer
 */
// Should never be instantiated directly. See {@link JobFactory} and {@link JobDefFunctions} for more information.
@Deprecated(since = "7.7.0")
public class Job extends AbstractJobDef {

	private @Getter JobDefFunctions function;

	@Override
	public void execute() {
		throw new NotImplementedException(); // will be replaced by appropriate executor class in JobFactory, based on function
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(super.toString());
		if(function != null) builder.append(" function [").append(function).append("]");
		return builder.toString();
	}

	@Mandatory
	public void setFunction(JobDefFunctions function) {
		this.function = function;
	}

}
