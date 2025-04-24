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

import java.io.IOException;

import org.springdoc.core.configuration.SpringDocDataRestConfiguration;
import org.springdoc.core.configuration.SpringDocHateoasConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

import io.swagger.v3.oas.models.OpenAPI;

/**
 * <p>
 * Based on the answer from <a href="https://github.com/springdoc/springdoc-openapi/issues/2343#issuecomment-1718995061">springdoc-openapi/issues/2343</a>
 * because we can't rely on EnableAutoConfiguration either.
 * </p>
 */
@Configuration
@ComponentScan(
		basePackages = {"org.springdoc"},
		excludeFilters = {@ComponentScan.Filter(
				type = FilterType.CUSTOM,
				classes = {
						OpenApiConfiguration.SpringDocDataRestFilter.class,
						OpenApiConfiguration.SpringDocHateoasFilter.class
				}
		)}
)
public class OpenApiConfiguration {
	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI();
	}

	public static class SpringDocDataRestFilter extends ClassFilter {
		@Override
		protected Class<?> getFilteredClass() {
			return SpringDocDataRestConfiguration.class;
		}
	}

	public static class SpringDocHateoasFilter extends ClassFilter {
		@Override
		protected Class<?> getFilteredClass() {
			return SpringDocHateoasConfiguration.class;
		}
	}

	private abstract static class ClassFilter implements TypeFilter {
		protected abstract Class<?> getFilteredClass();

		@Override
		public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
			String className = metadataReader.getClassMetadata().getClassName();
			String enclosingClassName = metadataReader.getClassMetadata().getEnclosingClassName();
			return
					className.equals(getFilteredClass().getCanonicalName())
							|| (enclosingClassName!=null && enclosingClassName.equals(getFilteredClass().getCanonicalName()));
		}
	}
}
