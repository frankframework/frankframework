/*
   Copyright 2021-2023 WeAreFrank!

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

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

public class FileMessage extends Message {

	private static final long serialVersionUID = 5219660236736759665L;

	public FileMessage(File file, Map<String,Object> context) {
		super(() -> new FileInputStream(file), new MessageContext(context)
				.withModificationTime(file.lastModified())
				.withSize(file.length())
				.withName(file.getName())
				.withLocation(file.getAbsolutePath())
			, file.getClass());
	}

	public FileMessage(File file, String charset) {
		this(file, new MessageContext(charset));
	}

	public FileMessage(File file) {
		this(file, new MessageContext());
	}
}
