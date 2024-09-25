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
package org.frankframework.parameters;

import java.io.IOException;

import jakarta.annotation.Nonnull;

import lombok.extern.log4j.Log4j2;

import org.frankframework.stream.Message;

@Log4j2
public class BooleanParameter extends AbstractParameter {

	public BooleanParameter() {
		setType(ParameterType.BOOLEAN);
	}

	@Override
	protected Boolean getValueAsType(@Nonnull Message request, boolean namespaceAware) throws IOException {
		if (request.asObject() instanceof Boolean bool) {
			return bool;
		}

		log.debug("Parameter [{}] converting result [{}] to boolean", this::getName, () -> request);
		return Boolean.parseBoolean(request.asString());
	}
}
