/*
   Copyright 2018 Nationale-Nederlanden, 2021, 2024 WeAreFrank!

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
package org.frankframework.align;

import org.jspecify.annotations.Nullable;

public interface SubstitutionProvider<V> {

	boolean hasSubstitutionsFor(AlignmentContext context, String childName);
	@Nullable V getSubstitutionsFor(AlignmentContext context, String childName);
	boolean hasOverride(AlignmentContext context);
	V getOverride(AlignmentContext context);
	@Nullable V getDefault(AlignmentContext context);

	boolean isNotEmpty();
}
