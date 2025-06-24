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
 *     ErrorMessageFormatters are configured on {@link Adapter}s to format
 *     exception messages when an exception is thrown in the execution
 *     of a {@link PipeLine}. When no specific error message formatter is
 *     configured, the default implementation {@link org.frankframework.errormessageformatters.ErrorMessageFormatter}
 *     is used. For more control over the layout of the message, configure
 *     a {@link org.frankframework.errormessageformatters.XslErrorMessageFormatter} or
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
