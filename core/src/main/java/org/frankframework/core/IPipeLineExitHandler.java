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
package org.frankframework.core;

/**
 * Interface that allows a Pipe to register an exit handler.
 * This handler will be called <i>always</i> after PipeLine-processing has finished
 *
 * @author  Gerrit van Brakel
 * @since   4.6.0
 */
public interface IPipeLineExitHandler extends NameAware, HasName {

	/**
	 * Called to allow registered handler to perform cleanup or commit/rollback.
	 *
	 * @param correlationId  correlationId of current session
	 * @param pipeLineResult the result of the PipeLine
	 * @param session        the PipeLineSession
	 */
	void atEndOfPipeLine(String correlationId, PipeLineResult pipeLineResult, PipeLineSession session) throws PipeRunException;
}
