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
package org.frankframework.management.web;

import java.util.Map;

import jakarta.annotation.security.RolesAllowed;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IbisstoreSummary extends FrankApiBase {

	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("jdbc")
	@Description("view database dump of the IbisStore table")
	@PostMapping(value = "/jdbc/summary", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getIbisstoreSummary(Map<String, Object> json) {

		String query = null;
		String datasource = null;

		for (Map.Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			if ("datasource".equalsIgnoreCase(key)) {
				datasource = entry.getValue().toString();
			}
			if ("query".equalsIgnoreCase(key)) {
				query = entry.getValue().toString();
			}
		}

		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.IBISSTORE_SUMMARY);
		builder.addHeader(BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, datasource);
		builder.addHeader("query", query);
		return callSyncGateway(builder);
	}

}
