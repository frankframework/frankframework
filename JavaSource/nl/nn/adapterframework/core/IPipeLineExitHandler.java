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
/*
 * $Log: IPipeLineExitHandler.java,v $
 * Revision 1.4  2012-06-01 10:52:52  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.3  2011/11/30 13:51:55  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2007/05/01 14:08:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of PipeLine-exithandlers
 *
 */
package nl.nn.adapterframework.core;

/**
 * Interface that allows a Pipe to register an exit handler.
 * This handler will be called <i>allways</i> after PipeLine-processing has finished
 * 
 * @author  Gerrit van Brakel
 * @since   4.6.0  
 * @version $Id$
 */
public interface IPipeLineExitHandler extends INamedObject {

	/**
	 * Called to allow registered handler to perform cleanup or commit/rollback.
	 * 
	 * @param correlationId	 correlationId of current session
	 * @param pipeLineResult the result of the PipeLine 
	 * @param session		 the PipeLineSession
	 */
	public void atEndOfPipeLine(String correlationId, PipeLineResult pipeLineResult, IPipeLineSession session) throws PipeRunException;
}
