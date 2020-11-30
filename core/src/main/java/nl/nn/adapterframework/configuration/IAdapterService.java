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

import java.util.Map;

import nl.nn.adapterframework.core.Adapter;

/**
 * @author Michiel Meeuwissen
 * @since 2.0.59
 */
public interface IAdapterService {

	/**
	 * Retrieve an {@link Adapter} from the {@link IAdapterService}
	 */
	Adapter getAdapter(String name);

	/**
	 * Returns all adapters registered with the {@link IAdapterService}
	 */
	Map<String, Adapter> getAdapters();

	/**
	 * Register an {@link Adapter} with the {@link IAdapterService}. Adapters 
	 * may only be registered once and {@link Adapter} names must be unique.
	 * @throws ConfigurationException If an adapter has no name or has already been registered
	 */
	void registerAdapter(Adapter adapter) throws ConfigurationException;

	/**
	 * Removes an {@link Adapter} from the {@link IAdapterService}
	 */
	void unRegisterAdapter(Adapter adapter);
}
