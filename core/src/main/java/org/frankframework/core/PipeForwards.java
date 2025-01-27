/*
   Copyright 2022 WeAreFrank!

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
package org.frankframework.core;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

import org.frankframework.doc.FrankDocGroup;
import org.frankframework.doc.FrankDocGroupValue;

/**
 * Optional element in a pipeline. Global forwards that will be added to every pipe, when the forward name has not been explicitly set.
 * For example the <code>&lt;forward name="exception" path="error_exception" /&gt;</code>, which will add the '<code>exception</code>' forward to every pipe in the pipeline.
 */
@FrankDocGroup(FrankDocGroupValue.OTHER)
public class PipeForwards {

	private final @Getter List<PipeForward> forwards = new ArrayList<>();

	/**
	 * Defines what pipe or exit to execute next. When the execution of a pipe is done, the pipe looks up the next pipe or exit to execute.
	 * See {@link PipeForward Forward} for more information.
	 */
	public void addForward(PipeForward forward) {
		forwards.add(forward);
	}

}
