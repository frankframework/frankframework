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
		OpenAPI openApi = new OpenAPI();
		Info info = new Info();
		info.title("Frank!Framework OpenApi Definition").version(Environment.getModuleVersion("frankframework-console-backend"));
		openApi.setInfo(info);

		return openApi;
	}

	@Bean
	public JavadocProvider getJavadocProvider() {
		return new JavadocProvider() {

			@Override
			public String getClassJavadoc(Class<?> cl) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Map<String, String> getRecordClassParamJavadoc(Class<?> cl) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getMethodJavadocDescription(Method method) {
				Description description = method.getAnnotation(Description.class);

				return description != null ? description.value() : null;
			}

			@Override
			public String getMethodJavadocReturn(Method method) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Map<String, String> getMethodJavadocThrows(Method method) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getParamJavadoc(Method method, String name) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getFieldJavadoc(Field field) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getFirstSentence(String text) {
				return text;
			}
		};
	}
}
