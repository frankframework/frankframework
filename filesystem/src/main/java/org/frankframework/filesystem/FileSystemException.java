/*
   Copyright 2019, 2024 Integration Partners

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

import lombok.Getter;

import org.frankframework.core.IbisException;
import org.frankframework.core.PipeForward;

public class FileSystemException extends IbisException {

	/**
	 * Forwards for FileSystemException. Defining this as enum restricts values to known values.
	 * The main reason for not using a simple String however is that this makes it possible for the
	 * compiler to disambiguate constructor overloads.
	 */
	public enum Forward {
		EXCEPTION(PipeForward.EXCEPTION_FORWARD_NAME),
		FILE_NOT_FOUND("fileNotFound"),
		FOLDER_NOT_FOUND("folderNotFound"),
		FILE_ALREADY_EXISTS("fileAlreadyExists"),
		FOLDER_ALREADY_EXISTS("folderAlreadyExists"),
		;

		Forward(String name) {
			this.forwardName = name;
		}

		@Getter
		private final String forwardName;
	}

	@Getter
	private final Forward forward;

	public FileSystemException(String message, Throwable cause) {
		super(message, cause);
		this.forward = getForwardFromCause(cause);
	}

	public FileSystemException(Forward forward, String message, Throwable cause) {
		super(message, cause);
		this.forward = forward;
	}

	public FileSystemException(String message) {
		super(message);
		this.forward = Forward.EXCEPTION;
	}

	public FileSystemException(Forward forward, String message) {
		super(message);
		this.forward = forward;
	}

	public FileSystemException(Throwable cause) {
		super(cause);
		this.forward = getForwardFromCause(cause);
	}

	public FileSystemException(Forward forward, Throwable cause) {
		super(cause);
		this.forward = forward;
	}

	private static Forward getForwardFromCause(Throwable cause) {
		return cause instanceof FileSystemException fse ? fse.getForward() : Forward.EXCEPTION;
	}
}
