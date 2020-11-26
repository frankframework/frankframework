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

package nl.nn.adapterframework.doc.model;

import java.util.List;

import nl.nn.adapterframework.doc.DocWriterNew;

/**
 * Callback interface for {@link FrankElement} to walk all declared as well as all inherited attributes.
 * {@link FrankElement} also uses this class to walk config children. Walking attributes and walking
 * config children is done in the same way, so the common base class {@link ElementChild} is walked here.
 * Please see {@link DocWriterNew} to understand about declared groups and cumulative groups.
 * {@link FrankElement} walks all {@link ElementChild} of a given kind (attribute or config child)
 * to support building a cumulative group.
 * <p>
 * To do this, {@link FrankElement} calls the methods of this class.
 *
 * @author martijn
 *
 * @param <T> {@link FrankAttribute} or {@link ConfigChild}.
 */
public interface CumulativeChildHandler<T extends ElementChild<?>> {
	/**
	 * Explicitly adds children to the cumulative group.
	 * @param children The children to add
	 * @param owner The FrankElement owning the children.
	 */
	void handleSelectedChildren(List<T> children, FrankElement owner);

	/**
	 * Adds the declared group of the kind given by type paramter <code>T</code> and
	 * the given {@link FrankElement} to the cumulative group being built. 
	 * @param frankElement The {@link FrankElement} of the declared group to be added
	 */
	void handleChildrenOf(FrankElement frankElement);

	/**
	 * Adds the cumulative group of the kind given by type paramter <code>T</code> and
	 * the given {@link FrankElement} to the cumulative group being built.
	 * @param frankElement The {@link FrankElement} of items to be added to the cumulative group being built.
	 */
	void handleCumulativeChildrenOf(FrankElement frankElement);
}
