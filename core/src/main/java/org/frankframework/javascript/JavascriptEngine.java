/*
   Copyright 2019-2024 WeAreFrank!

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
package org.frankframework.javascript;

import org.frankframework.core.ISender;
import org.frankframework.core.PipeLineSession;
import org.frankframework.util.flow.ResultHandler;

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
	 * @param alias An identifier which describes the script(s) that are being executed.
	 */
	void setGlobalAlias(String alias);

	/**
	 * Initialize the runtime for the specified engine
	 */
	void startRuntime() throws JavascriptException;

	/**
	 * Read the functions of a given javascript file
	 *
	 * @param script		String containing the contents of the javascript file in which the function(s) to be executed are specified.
	 */
	void executeScript(String script) throws JavascriptException;

	/**
	 * Executes a javascript function and returns the result of that function
	 *
	 * @param name			The name of the javascript function as given in the javascript file.
	 * @param parameters	An array containing the parameters for the javascript function, given in the adapter configuration
	 * @return				The result of the javascript function is returned.
	 */
	Object executeFunction(String name, Object... parameters) throws JavascriptException;

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
	 * Allows for senders to be called by the Javascript function. Sender needs to be given in the adapter configuration,
	 * a Javascript function can call the sender, using the name of the sender as a function call.
	 *
	 * @param sender		The sender given in the adapter configuration
	 */
	void registerCallback(ISender sender, PipeLineSession session);

	/**
	 * Registers the result and error functions to be handled by the given result handler.
	 * @param resultHandler Object to handle results and errors.
	 */
	void setResultHandler(ResultHandler resultHandler);
}
