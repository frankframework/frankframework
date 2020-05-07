/*
   Copyright 2020 WeAreFrank!

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
 * IForwardTarget represents the destination of a PipeForward, so a {@link IPipe} or {@link PipeLineExit}.
 * 
 * @author Gerrit van Brakel
 */
public interface IForwardTarget {
	
	/**
	 * The part of the object that identifies its destination: The {@link IPipe#getName() name} of the Pipe or the {@link PipeLineExit#setPath(String) path} of the PipeLineExit.
	 */
	public String getName();

}
