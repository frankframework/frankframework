/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.credentialprovider.delinea;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response POJO for listing all secrets.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SecretsListDto(
	boolean hasNext,
	boolean hasPrev,
	boolean success,
	int batchCount,
	int currentPage,
	int nextSkip,
	int pageCount,
	int prevSkip,
	int skip,
	int total,
	int take,
	List<CategorizedListSummary> records) {

	// We only need the id since that's used to determine the secret's alias.
	@JsonIgnoreProperties(ignoreUnknown = true)
	record CategorizedListSummary(int id) {}
}
