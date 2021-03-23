/* 
Copyright 2020, 2021 WeAreFrank! 

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

package nl.nn.adapterframework.frankdoc.model;

import java.util.List;

/**
 * Callback interface for {@link FrankElement} to walk all declared as well as all inherited attributes or config children.
 *
 * @author martijn
 *
 * @param <T> {@link FrankAttribute} or {@link ConfigChild}.
 */
public interface CumulativeChildHandler<T extends ElementChild> {
	void handleSelectedChildren(List<T> children, FrankElement owner);
	void handleChildrenOf(FrankElement frankElement);
	void handleCumulativeChildrenOf(FrankElement frankElement);
}
