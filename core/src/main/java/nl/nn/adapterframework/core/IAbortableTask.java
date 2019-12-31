/*
   Copyright 2019 Integration Partners

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
package nl.nn.adapterframework.core;

import java.util.concurrent.Callable;

/**
 * Representation of a task that can be executed, and of which the execution can be aborted, 
 * e.g. when a timeout occurs.
 * 
 * @author Gerrit van Brakel
 */
public interface IAbortableTask<V> extends Callable<V> {
	
	/**
	 * calling abort should result in stopping the task running, and cleaning up it's resources.
	 */
	public void abort();

}
