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
 * $Log: IManagable.java,v $
 * Revision 1.6  2011-11-30 13:51:55  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.2  2011/10/19 14:56:01  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * do not print versions anymore
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.4  2004/03/30 07:29:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.3  2004/03/26 10:42:50  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 */
package nl.nn.adapterframework.core;

import nl.nn.adapterframework.util.RunStateEnum;
/**
 * Models starting and stopping of objects that support such behaviour.
 * 
 * @version $Id$
 * @author Gerrit van Brakel
 * @since 4.0
 */
public interface IManagable extends INamedObject {
/**
 * returns the runstate of the object.
 * Possible values are defined by {@link RunStateEnum}.
 */
RunStateEnum getRunState();
/**
 * Instruct the object that implements <code>IManagable</code> to start working.
 * The method does not wait for completion of the command; at return of this method, 
 * the object might be still in the STARTING-runstate
 */
void startRunning();
/**
 * Instruct the object that implements <code>IManagable</code> to stop working. 
 * The method does not wait for completion of the command; at return of this method, 
 * the object might be still in the STOPPING-runstate
 */
void stopRunning();
}
