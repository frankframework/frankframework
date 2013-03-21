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
 * $Log: ServiceClient.java,v $
 * Revision 1.6  2011-11-30 13:51:54  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:43  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.4  2011/05/19 14:58:46  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * simplified into one single interace ServiceClient
 *
 */ 
package nl.nn.adapterframework.receivers;

import java.util.Map;

import nl.nn.adapterframework.core.ListenerException;

/**
 * The interface clients (users) of a service may use.
 */
public interface ServiceClient {
	
	public String processRequest(String correlationId, String message, Map requestContext) throws ListenerException;
}
