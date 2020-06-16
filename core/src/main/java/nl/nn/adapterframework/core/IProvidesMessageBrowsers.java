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
 * Interface that can be implemented by Listeners that provide their own management of
 * messages processed and in error.
 */
public interface IProvidesMessageBrowsers<M> {

	/**
	 * returns a {@link IMessageBrowser browser} of messages that have been processed successfully, and are stored in a 
	 * storage managed by the listener itself (as opposed to a storage configured as a messageLog in the configuration).
	 */
	public IMessageBrowser<M> getMessageLogBrowser();
	
	/**
	 * returns a {@link IMessageBrowser browser} of messages that have been processed in error, and are stored in a 
	 * storage managed by the listener itself (as opposed to a storage configured as a errorStore in the configuration).
	 */
	public IMessageBrowser<M> getErrorStoreBrowser();
	
}
