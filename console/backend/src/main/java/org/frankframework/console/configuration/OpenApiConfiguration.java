/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.console.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;

/**
 * <p>
 * Based on the answer from <a href="https://github.com/springdoc/springdoc-openapi/issues/2343#issuecomment-1718995061">springdoc-openapi/issues/2343</a>
 * because we can't rely on EnableAutoConfiguration either.
 * </p>
 */
@Configuration
public class OpenApiConfiguration {

	// TODO Set Title + Version
	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI();
	}

}
