/*
   Copyright 2013 Nationale-Nederlanden, 2022-2025 WeAreFrank!

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
package org.frankframework.statistics;

import org.springframework.context.ApplicationContext;

/**
 * Interface to be implemented by objects like Pipes or Senders that maintain additional statistics themselves.
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public interface HasStatistics {

	String getName();
	ApplicationContext getApplicationContext(); //Allows the statistic to be grouped by Configuration and Adapter
}
