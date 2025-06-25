/*
   Copyright 2013 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.frankframework.doc.FrankDocGroup;
import org.frankframework.doc.FrankDocGroupValue;
import org.frankframework.stream.Message;

/**
 * An {@code errorMessageFormatter} is responsible for returning a message
 * describing the error at hand in a format that the receiver expects.
 *
 * <p>
 *     ErrorMessageFormatters are configured on {@link Adapter}s or {@link org.frankframework.configuration.Configuration}s to format
 *     exception messages when an exception is thrown during the {@link PipeLine} execution process.
 * </p>
 * <p>
 *     The ErrorMessageFormatter is called when a {@link IPipe} throws an exception, and has an {@code exceptionForward}. The
 *     target of the {@code exceptionForward} will receive the message produced by the ErrorMessageFormatter.
 * </p>
 * <p>
 *     The ErrorMessageFormatter is also called when any exception occurs during pipeline execution that is not caught or handled
 *     by an {@code exceptionForward}. The error message is then returned as the pipeline result.
 * </p>
 * <p>
 *     If you want to return a specific error message from a pipeline to signal a (functional) error condition that did not result from
 *     an exception, use an {@link org.frankframework.pipes.ExceptionPipe} to trigger an exception. This exception will then result in the
 *     ErrorMessageFormatter being called.
 *     You can use {@link org.frankframework.parameters.IParameter}s on the {@link org.frankframework.pipes.ExceptionPipe} to pass specific
 *     information to the ErrorMessageFormatter such as error codes, error messages, etc. See the example message layouts in
 *     {@link org.frankframework.errormessageformatters.ErrorMessageFormatter} to see how parameters are available in the XML or JSON error message.
 *     Parameters from the {@link org.frankframework.pipes.ExceptionPipe} are also copied into the {@link PipeLineSession}.
 * </p>
 * <p>
 *     When no specific ErrorMessageFormatter is configured on the Adapter or its Configuration, the default implementation
 *     {@link org.frankframework.errormessageformatters.ErrorMessageFormatter} is used with a default XML format. For more control
 *     over the layout of the message, configure a {@link org.frankframework.errormessageformatters.XslErrorMessageFormatter} or
 *     {@link org.frankframework.errormessageformatters.DataSonnetErrorMessageFormatter}.
 * </p>
 * <p>
 *     If these do not provide enough control over the error message format for your
 *     adapter, you can provide a custom implementation of this interface as custom code in
 *     your configuration.
 * </p>
 *
 * @author Johan Verrips
 */
@FrankDocGroup(FrankDocGroupValue.ERROR_MESSAGE_FORMATTER)
public interface IErrorMessageFormatter {

	@Nonnull Message format(@Nullable String errorMessage, @Nullable Throwable t, @Nullable HasName location, @Nullable Message originalMessage, @Nonnull PipeLineSession session);
}
