/*
   Copyright 2020-2022, 2024 WeAreFrank!

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
package org.frankframework.core;

import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.FrankDocGroup;
import org.frankframework.doc.FrankDocGroupValue;
import org.frankframework.doc.Optional;

/**
 * Marker interface for Wrappers
 * @author Gerrit van Brakel
 */
@FrankDocGroup(FrankDocGroupValue.PIPE)
@FrankDocGroup(FrankDocGroupValue.WRAPPER)
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.WRAPPER)
public interface IWrapperPipe extends IPipe {

	enum Direction {WRAP, UNWRAP}

	/** The functional name of this pipe, is not required when used as a Wrapper */
	@Override
	@Optional
	void setName(String name);
}
