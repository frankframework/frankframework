/*
   Copyright 2023 WeAreFrank!

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
package org.frankframework.configuration;

import org.frankframework.core.SharedResource;
import org.frankframework.doc.FrankDocGroup;
import org.frankframework.doc.FrankDocGroupValue;
import org.frankframework.http.HttpSession;

@FrankDocGroup(FrankDocGroupValue.OTHER)
public class SharedResources {

	public void addSharedResource(SharedResource<?> resource) {
		// This method only exists for the FrankDoc
	}

	public void addHttpSession(HttpSession resource) {
		// Hack to directly use the HttpSession element in configurations
	}
}
