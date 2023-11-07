/*
   Copyright 2020-2022 WeAreFrank!

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

import nl.nn.adapterframework.doc.ElementType;
import nl.nn.adapterframework.doc.ElementType.ElementTypes;
import nl.nn.adapterframework.doc.FrankDocGroup;
import nl.nn.adapterframework.doc.Optional;

/**
 * Marker interface for Wrappers
 * 
 * * @author Gerrit van Brakel
 * 
 */
@FrankDocGroup(order = 50, name = "Wrappers")
@ElementType(ElementTypes.WRAPPER)
public interface IWrapperPipe extends IPipe {

	enum Direction { WRAP, UNWRAP; };

	/** The functional name of this pipe, is not required when used as a Wrapper */
	@Override
	@Optional
	public void setName(String name);
}
