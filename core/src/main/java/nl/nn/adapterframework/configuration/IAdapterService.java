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
package nl.nn.adapterframework.configuration;

import nl.nn.adapterframework.core.IAdapter;

import java.util.Map;

/**
 * @author Michiel Meeuwissen
 * @since 2.0.59
 */
public interface IAdapterService {

	/**
	 * Retrieve an {@link IAdapter} from the {@link IAdapterService}
	 */
	IAdapter getAdapter(String name);

	/**
	 * Returns all adapters registered with the {@link IAdapterService}
	 */
	Map<String, IAdapter> getAdapters();

	/**
	 * Register an {@link IAdapter} with the {@link IAdapterService}. Adapters 
	 * may only be registered once and {@link IAdapter} names must be unique.
	 * @throws ConfigurationException If an adapter has no name or has already been registered
	 */
	void registerAdapter(IAdapter adapter) throws ConfigurationException;

	/**
	 * Removes an {@link IAdapter} from the {@link IAdapterService}
	 */
	void unRegisterAdapter(IAdapter adapter);
}
