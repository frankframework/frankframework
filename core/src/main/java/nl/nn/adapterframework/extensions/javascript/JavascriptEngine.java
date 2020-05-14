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
package nl.nn.adapterframework.extensions.javascript;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;

/** 
 * Javascript engine interface, allows the use of a javascript engine to execute javascript code functions.
 * 
 * @author Jarno Huibers
 * @since 7.4
 *
 * @param <E> Specifies the type of javascript engine
 */

public interface JavascriptEngine<E> {

	/**
	 * Initialize the runtime for the specified engine
	 */
	void startRuntime();

	/**
	 * Read the functions of a given javascript file
	 * 
	 * @param script		String containing the contents of the javascript file in which the function(s) to be executed are specified.
	 */
	void executeScript(String script);

	/**
	 * Executes a javascript function and returns the result of that function
	 * 
	 * @param name		The name of the javascript function as given in the javascript file.
	 * @param parameters		An array containing the parameters for the javascript function, given in the adapter configuration
	 * @return		The result of the javascript function is returned.
	 */
	Object executeFunction(String name, Object... parameters);

	/**
	 * Closes the runtime for the specified engine
	 */
	void closeRuntime();

	/**
	 * Getter for the runtime of the specified engine
	 * 
	 * @return		Returns the runtime instance
	 */
	E getEngine();

	/**
	 * Only used by J2V8, allows for senders to be called by the javascript function. Sender needs to be given in the adapter configuration,
	 * a javascript function can call the sender using the name of the sender as a function call.
	 * 
	 * @param sender		The sender given in the adapter configuration
	 */
	public void registerCallback(ISender sender, IPipeLineSession session);
}