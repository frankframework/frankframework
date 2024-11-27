/*
   Copyright 2019-2021 WeAreFrank!

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
package org.frankframework.threading;

/**
 * Interface for classes that need to be aware of when child-threads are created and terminated. This is primarily for LadyBug, via the
 * {@code IbisDebuggerAdvice} class which implements this interface.
 *
 * @param <T>
 */
public interface ThreadLifeCycleEventListener<T> {

	T announceChildThread(Object owner, String correlationId);
	void cancelChildThread(T ref);
	<O> O threadCreated(T ref, O request);
	<O> O threadEnded(T ref, O result);
	Throwable threadAborted(T ref, Throwable t);

}
