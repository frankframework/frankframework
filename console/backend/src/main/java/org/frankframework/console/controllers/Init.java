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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
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

	private boolean isMonitoringEnabled() {
		return frankApiService.getProperty("monitoring.enabled", false);
	}

	@GetMapping(value = {"", "/"}, produces = "application/json")
	@PermitAll
	public ResponseEntity<?> getAllResources(HttpServletRequest servletRequest,
											 @RequestParam(value = "allowedRoles", required = false) boolean displayAllowedRoles,
											 @RequestParam(value = "hateoas", defaultValue = "default") String hateoasImpl) {
		List<Object> JSONresources = new ArrayList<>();
		Map<String, Object> HALresources = new HashMap<>();
		Map<String, Object> resources = new HashMap<>(1);
		boolean hateoasSupport = "hal".equalsIgnoreCase(hateoasImpl);

		String requestPath = servletRequest.getRequestURL().toString();
		if (requestPath.endsWith("/")) {
			requestPath = requestPath.substring(0, requestPath.length() - 1);
		}

		Map<RequestMappingInfo, HandlerMethod> handlerMethods = this.handlerMapping.getHandlerMethods();

		for (Map.Entry<RequestMappingInfo, HandlerMethod> mappingHandler : handlerMethods.entrySet()) {
			final RequestMappingInfo mappingInfo = mappingHandler.getKey();
			final HandlerMethod handlerMethod = mappingHandler.getValue();
			String relation = null;

			if (handlerMethod.getBeanType().getName().endsWith("Monitors") && !isMonitoringEnabled()) {
				continue;
			}

			final Method method = handlerMethod.getMethod();
			boolean deprecated = method.getAnnotation(Deprecated.class) != null;

			if (deprecated && !frankApiService.allowDeprecatedEndpoints()) {
				continue;
			}

			PathPattern[] paths = mappingInfo.getPathPatternsCondition().getPatterns().toArray(new PathPattern[0]);
			RequestMethod methodType = mappingInfo.getMethodsCondition().getMethods().toArray(new RequestMethod[0])[0];
			RolesAllowed rolesAllowed = method.getAnnotation(RolesAllowed.class);
			Description description = method.getAnnotation(Description.class);

			String[] allowedRolesList = displayAllowedRoles && rolesAllowed != null ?
					rolesAllowed.value() : null;
			String descriptionText = description != null ? description.value() : null;
			boolean hasRelation = method.isAnnotationPresent(Relation.class);
			String rel = !hateoasSupport && hasRelation ? method.getAnnotation(Relation.class).value() : null;

			for (PathPattern path : paths) {
				Map<String, Object> resource = new HashMap<>(6);
				resource.put("name", method.getName());
				resource.put("href", requestPath + path.getPatternString());
				resource.put("type", methodType.name());
				if (deprecated)
					resource.put("deprecated", deprecated);
				if (allowedRolesList != null)
					resource.put("roles", allowedRolesList);
				if (descriptionText != null)
					resource.put("description", descriptionText);

				if (hateoasSupport) {
					if (hasRelation)
						relation = method.getAnnotation(Relation.class).value();

					if (relation != null) {
						if (HALresources.containsKey(relation)) {
							Object prevRelation = HALresources.get(relation);
							List<Object> tmpList = null;
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
}
