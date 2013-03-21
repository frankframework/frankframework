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
 * The PipeLineExit, that represents a terminator of the PipeLine, provides a placeholder
 * for a path (corresponding to a pipeforward) and a state (that is returned to the receiver). 
 * </p>
 * @version $Id$
 * @author Johan Verrips
 */
public class PipeLineExit {
	
	private String path;
	private String state;

	public String getPath() {
		return path;
	}
	public void setPath(String newPath) {
		path = newPath;
	}
	
	public String getState() {
		return state;
	}
	public void setState(String newState) {
		state = newState;
	}
}
