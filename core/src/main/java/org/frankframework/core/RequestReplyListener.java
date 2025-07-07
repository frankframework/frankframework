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
package org.frankframework.core;

/**
 * Interface to be added to {@link IListener}s that are used for request / reply
 * scenarios.
 */
public interface RequestReplyListener {

	enum ExceptionHandlingMethod {
		RETHROW, FORMAT_AND_RETURN;
	}

	/**
	 * When an exception happens in the execution of the pipeline, with {@code RETHROW} the
	 * exception is thrown to the caller. With {@code FORMAT_AND_RETURN} the exception is processed
	 * by the {@link Adapter#setErrorMessageFormatter(IErrorMessageFormatter)} and returned as result-message
	 * of the {@link Adapter}.
	 *
	 * <br/>
	 * The default is currently {@code RETHROW} for backwards compatibility but will become {@code FORMAT_AND_RETURN} in a future version.
	 *
	 * @ff.default RETHROW
	 * @since 9.2
	 *
	 * @param method {@code RETHROW} or {@code FORMAT_AND_RETURN}
	 */
	void setOnException(ExceptionHandlingMethod method);
	ExceptionHandlingMethod getOnException();
}
