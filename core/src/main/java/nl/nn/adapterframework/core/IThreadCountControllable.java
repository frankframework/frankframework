/*
   Copyright 2013 Nationale-Nederlanden

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

/**
 * Interface to be implemented by classes of which the number of threads can be controlled at runtime.
 * <p>
 * Implementing this class results in receivers that have a number of threads that can be controlled
 * from the console.
 *
 * @author Gerrit van Brakel
 * @since
 */
public interface IThreadCountControllable {

	boolean isThreadCountReadable();
	boolean isThreadCountControllable();

	int getCurrentThreadCount();
	int getMaxThreadCount();

	void increaseThreadCount();
	void decreaseThreadCount();

}
