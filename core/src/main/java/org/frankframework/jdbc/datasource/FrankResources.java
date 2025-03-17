/*
   Copyright 2024-2025 WeAreFrank!

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
package org.frankframework.jdbc.datasource;

import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import lombok.Setter;

public class FrankResources {

	private @Setter List<FrankResource> resources;

	public @Nullable FrankResource findResource(@Nonnull String name) {
		if (resources == null) {
			return null; // No matching resources found.
		}

		return resources.stream()
				.filter(e -> name.equals(e.getName()))
				.findFirst()
				.orElse(null);
	}

}
