/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.jdbc.datasource;

import java.util.Properties;

public interface IObjectLocator {

	/**
	 * Performs the actual lookup and should return <code>null</code> if it cannot
	 * find the requested resource so the next {@link IObjectLocator} can give it a
	 * shot.
	 */
	public <O> O lookup(String name, Properties environment, Class<O> lookupClass) throws Exception;

}
