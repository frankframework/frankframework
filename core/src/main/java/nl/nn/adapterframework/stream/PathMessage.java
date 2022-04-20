/*
   Copyright 2021, 2022 WeAreFrank!

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
package nl.nn.adapterframework.stream;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import nl.nn.adapterframework.util.ClassUtils;

public class PathMessage extends Message {

	private static final long serialVersionUID = -6810228164430433617L;
	private transient Path path;

	public PathMessage(Path path, Map<String,Object> context) {
		super(() -> Files.newInputStream(path), new MessageContext(context)
				.withModificationTime(path.toFile().lastModified())
				.withSize(path.toFile().length())
				.withName(path.getFileName().toString())
				.withLocation(path.toAbsolutePath().toString())
			, path.getClass());
		this.path = path;
	}

	public PathMessage(Path path) {
		this(path, new MessageContext());
	}

	@Override
	public long size() {
		if (path!=null) {
			try {
				return Files.size(path);
			} catch (IOException e) {
				log.debug("unable to determine size of stream ["+ClassUtils.nameOf(path)+"]", e);
				return -1;
			}
		}
		return super.size();
	}

}
