/*
   Copyright 2021-2024, 2026 WeAreFrank!

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
package org.frankframework.configuration;

import org.jspecify.annotations.NonNull;
import org.springframework.util.ErrorHandler;

import lombok.extern.log4j.Log4j2;

/**
 * Implementation of Spring error handler that logs errors. To be used for Event handling errors.
 */
@Log4j2
public class SpringEventErrorHandler implements ErrorHandler {

	@Override
	public void handleError(@NonNull Throwable t) {
		log.warn("Error handling event", t);
	}
}
