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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import org.springdoc.core.providers.JavadocProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.frankframework.console.Description;
import org.frankframework.util.Environment;
import org.frankframework.util.StringUtil;

/**
 * <p>
 * Based on the answer from <a href="https://github.com/springdoc/springdoc-openapi/issues/2343#issuecomment-1718995061">springdoc-openapi/issues/2343</a>
 * because we can't rely on EnableAutoConfiguration either.
 * </p>
 */
@Configuration
public class OpenApiConfiguration {

	@Bean
	public OpenAPI openAPI() {
		OpenAPI openApi = new OpenAPI();
		openApi.setInfo(new Info()
				.title("Frank!Framework OpenApi Definition")
				.version(Environment.getModuleVersion("frankframework-console-backend")));

		return openApi;
	}

	@Bean
	public JavadocProvider getJavadocProvider() {
		return new JavadocProvider() {

			// Creates the `tags` in the spec and adds a description to it.
			@Override
			public String getClassJavadoc(Class<?> cl) {
				Description description = cl.getAnnotation(Description.class);

				return description != null ? formatString(description.value()) : null;
			}

			@Override
			public Map<String, String> getRecordClassParamJavadoc(Class<?> cl) {
				return Map.of();
			}

			@Override
			public String getMethodJavadocDescription(Method method) {
				Description description = method.getAnnotation(Description.class);

				return description != null ? formatString(description.value()) : null;
			}

			@Override
			public String getMethodJavadocReturn(Method method) {
				return null;
			}

			// Perhaps we should create a generic error response?
			@Override
			public Map<String, String> getMethodJavadocThrows(Method method) {
				return Map.of();
			}

			@Override
			public String getParamJavadoc(Method method, String name) {
				return null;
			}

			@Override
			public String getFieldJavadoc(Field field) {
				return null;
			}

			// Creates a summary element, which we don't need to use as our descriptions are small enough.
			@Override
			public String getFirstSentence(String text) {
				return null;
			}
		};
	}

	// Upper case first character and end with a `.`.
	private static String formatString(String str) {
		String capitalString = StringUtil.ucFirst(str);
		return capitalString.endsWith(".") ? capitalString : capitalString + ".";
	}
}
