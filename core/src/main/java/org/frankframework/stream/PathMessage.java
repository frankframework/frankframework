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

import java.io.Serial;
import java.nio.file.Path;

public class PathMessage extends Message {

	@Serial
	private static final long serialVersionUID = -6810228164430433617L;

	public PathMessage(Path path) {
		this(path, new MessageContext());
	}

	public PathMessage(Path path, MessageContext context) {
		this(path, context, false);
	}

	private PathMessage(Path path, MessageContext context, boolean removeOnClose) {
		super(new SerializableFileReference(path, removeOnClose), new MessageContext(context)
						.withModificationTime(path.toFile().lastModified())
						.withSize(path.toFile().length())
						.withName(path.getFileName().toString())
						.withLocation(path.toAbsolutePath().toString())
				, path.getClass());
	}

	public static PathMessage asTemporaryMessage(Path path) {
		return new PathMessage(path, new MessageContext(), true);
	}

}
