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
package org.frankframework.console.controllers;

import jakarta.annotation.security.PermitAll;

import org.frankframework.util.Environment;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/console")
public class ConsoleDetails {

	@PermitAll
	@GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, String>> getConsoleVersion() {
		Map<String, String> map = new HashMap<>();
		map.put("version", Environment.getModuleVersion("frankframework-console-backend"));
		return ResponseEntity.ok(map);
	}

}
