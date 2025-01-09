/*
   Copyright 2021-2025 WeAreFrank!

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
package org.frankframework.configuration.digester;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;

import org.frankframework.scheduler.JobDefFunctions;
import org.frankframework.scheduler.job.Job;
import org.frankframework.util.EnumUtils;

/**
 * Factory for instantiating Schedules Jobs from the Digester framework.
 * Instantiates the job based on the function specified.
 *
 * @author Niels Meijer
 */
@SuppressWarnings("deprecation")
public class JobFactory extends GenericFactory {

	@Override
	public Object createBean(ApplicationContext applicationContext, Map<String, String> attrs) throws ClassNotFoundException {
		String className = attrs.get("className");
		if(StringUtils.isEmpty(className) || className.equals(Job.class.getCanonicalName())) { // Default empty, filled when using new pre-parsing
			String function = attrs.get("function");
			if(StringUtils.isEmpty(function)) {
				throw new IllegalArgumentException("function may not be empty");
			}

			className = determineClassNameFromFunction(function);
			attrs.put("className", className);
		}
		return super.createBean(applicationContext, attrs);
	}

	private String determineClassNameFromFunction(String functionName) {
		JobDefFunctions function = EnumUtils.parse(JobDefFunctions.class, functionName);
		Class<?> clazz = function.getJobClass();

		return clazz.getCanonicalName();
	}
}
