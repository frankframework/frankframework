/*
   Copyright 2022 WeAreFrank!

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
package org.frankframework.jdbc.migration;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import liquibase.Scope;
import liquibase.changelog.ChangeLogParameters;
import liquibase.exception.ChangeLogParseException;
import liquibase.parser.core.ParsedNode;
import liquibase.parser.core.xml.XMLChangeLogSAXParser;
import liquibase.resource.ResourceAccessor;

import org.frankframework.util.LogUtil;

public class LiquibaseXmlChangeLogParser extends XMLChangeLogSAXParser {
	protected Logger log = LogUtil.getLogger(this);

	@Override
	protected ParsedNode parseToNode(String physicalChangeLogLocation, ChangeLogParameters changeLogParameters, ResourceAccessor resourceAccessor) throws ChangeLogParseException {
		try {
			Map<String, Object> scopeValues = new HashMap<>();
			scopeValues.put(Scope.Attr.resourceAccessor.name(), resourceAccessor);
			String scopeId = Scope.enter(scopeValues);

			try {
				return super.parseToNode(physicalChangeLogLocation, changeLogParameters, resourceAccessor);
			} finally {
				Scope.exit(scopeId);
			}
		} catch (Exception e) {
			throw new ChangeLogParseException(e);
		}
	}

}
