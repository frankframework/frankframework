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
package org.frankframework.console.controllers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;

import lombok.Getter;

import lombok.Setter;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import org.frankframework.console.Description;
import org.frankframework.console.Relation;

@RestController
public class Init {

	private final FrankApiService frankApiService;

	private final RequestMappingHandlerMapping handlerMapping;

	public Init(FrankApiService frankApiService, RequestMappingHandlerMapping handlerMapping) {
		this.frankApiService = frankApiService;
		this.handlerMapping = handlerMapping;
	}

	@Getter
	@Setter
	public static class ParametersModel {
		boolean allowedRoles;
		String hateoas = "default";
	}

	@GetMapping(value = {"", "/"}, produces = "application/json")
	@PermitAll
	public ResponseEntity<?> getAllResources(HttpServletRequest servletRequest,
											 ParametersModel params) {
		List<Object> JSONresources = new ArrayList<>();
		Map<String, Object> HALresources = new HashMap<>();
		Map<String, Object> resources = new HashMap<>(1);
		boolean hateoasSupport = "hal".equalsIgnoreCase(params.hateoas);

		String requestPath = servletRequest.getRequestURL().toString();
		if (requestPath.endsWith("/")) {
			requestPath = requestPath.substring(0, requestPath.length() - 1);
		}

		Map<RequestMappingInfo, HandlerMethod> handlerMethods = this.handlerMapping.getHandlerMethods();

		for (Map.Entry<RequestMappingInfo, HandlerMethod> mappingHandler : handlerMethods.entrySet()) {
			final RequestMappingInfo mappingInfo = mappingHandler.getKey();
			final HandlerMethod handlerMethod = mappingHandler.getValue();

			if (shouldSkip(handlerMethod)) {
				continue;
			}

			String relation = null;
			final Method method = handlerMethod.getMethod();
			boolean deprecated = method.getAnnotation(Deprecated.class) != null;

			if (deprecated && !frankApiService.allowDeprecatedEndpoints()) {
				continue;
			}

			PathPattern[] paths = mappingInfo.getPathPatternsCondition().getPatterns().toArray(new PathPattern[0]);
			RequestMethod methodType = mappingInfo.getMethodsCondition().getMethods().toArray(new RequestMethod[0])[0];
			Description description = method.getAnnotation(Description.class);

			String descriptionText = description != null ? description.value() : null;
			boolean hasRelation = method.isAnnotationPresent(Relation.class);
			String rel = !hateoasSupport && hasRelation ? method.getAnnotation(Relation.class).value() : null;

			for (PathPattern path : paths) {
				Map<String, Object> resource = new HashMap<>(6);
				resource.put("name", method.getName());
				resource.put("href", requestPath + path.getPatternString());
				resource.put("type", methodType.name());
				if (deprecated) {
					resource.put("deprecated", true);
				}
				if (params.allowedRoles) {
					List<String> roles = getAllowedRoles(method);
					if(!roles.isEmpty()) {
						resource.put("roles", roles);
					}
				}
				if (descriptionText != null)
					resource.put("description", descriptionText);

				if (hateoasSupport) {
					if (hasRelation)
						relation = method.getAnnotation(Relation.class).value();

					if (relation != null) {
						if (HALresources.containsKey(relation)) {
							Object prevRelation = HALresources.get(relation);
							List<Object> tmpList;
							if (prevRelation instanceof List)
								tmpList = (List) prevRelation;
							else {
								tmpList = new ArrayList<>();
								tmpList.add(prevRelation);
							}

							tmpList.add(resource);
							HALresources.put(relation, tmpList);
						} else
							HALresources.put(relation, resource);
					}
				} else {
					if (hasRelation) {
						resource.put("rel", rel);
					}
					JSONresources.add(resource);
				}
			}
		}

		if (hateoasSupport) {
			resources.put("_links", HALresources);
		} else {
			resources.put("links", JSONresources);
		}

		return ResponseEntity.status(HttpStatus.OK).body(resources);
	}

	private boolean isMonitoringEnabled() {
		return frankApiService.getProperty("monitoring.enabled", true);
	}

	private boolean shouldSkip(HandlerMethod handlerMethod) {
		String className = handlerMethod.getBeanType().getCanonicalName();

		if(Monitors.class.getCanonicalName().equals(className) && !isMonitoringEnabled()) {
			return true;
		} else if(this.getClass().getCanonicalName().equals(className)) {
			return true;
		}

		return false;
	}

	private List<String> getAllowedRoles(Method method) {
		RolesAllowed test = AnnotationUtils.findAnnotation(method, RolesAllowed.class);
		return test != null ? Arrays.asList(test.value()) : Collections.emptyList();
	}
}
