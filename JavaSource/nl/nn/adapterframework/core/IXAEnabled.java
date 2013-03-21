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
 * $Log: IXAEnabled.java,v $
 * Revision 1.5  2011-11-30 13:51:55  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.2  2011/10/19 14:58:12  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * do not print versions anymore
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.3  2004/03/26 10:42:44  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.2  2004/03/23 17:20:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * one little puntkomma
 *
 * Revision 1.1  2004/03/23 16:52:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * initial version
 *
 */
package nl.nn.adapterframework.core;

/**
 * Indicates a Pipe, Sender or Listener to be capable of supporting XA-transactions. 
 * When isTransacted() returns true, alternative XA enabled versions of resources like
 * connectionfactories should be used by implementing classes.
 * @version $Id$
 * @author Gerrit van Brakel
 * @since  4.1
 */
public interface IXAEnabled {
	
	/**
	 *  indicates implementing object is under transaction control, using XA-transactions
	 */
	public boolean isTransacted();

}
