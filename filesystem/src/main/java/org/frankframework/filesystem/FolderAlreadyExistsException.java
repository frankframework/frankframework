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
package org.frankframework.filesystem;

public class FolderAlreadyExistsException extends FileSystemException {

	public FolderAlreadyExistsException(String message, Throwable cause) {
		super(Forward.FOLDER_ALREADY_EXISTS, message, cause);
	}

	public FolderAlreadyExistsException(String message) {
		super(Forward.FOLDER_ALREADY_EXISTS, message);
	}

	public FolderAlreadyExistsException(Throwable cause) {
		super(Forward.FOLDER_ALREADY_EXISTS, cause);
	}
}
