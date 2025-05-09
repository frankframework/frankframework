/*
   Copyright 2021-2025 WeAreFrank!

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
package org.frankframework.stream;

import java.io.File;
import java.io.Serial;
import java.nio.charset.Charset;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class FileMessage extends Message {

	@Serial private static final long serialVersionUID = 5219660236736759665L;

	public FileMessage(File file) {
		this(file, null);
	}

	/**
	 * Convenience method which leverages the file attributes to fill the {@link MessageContext}.
	 */
	public FileMessage(@Nonnull File file, @Nullable Charset charset) {
		super(new SerializableFileReference(file.toPath(), false), new MessageContext().withCharset(charset).withModificationTime(file.lastModified())
				.withSize(file.length())
				.withName(file.getName())
				.withLocation(file.getAbsolutePath())
			, file.getClass());
	}
}
