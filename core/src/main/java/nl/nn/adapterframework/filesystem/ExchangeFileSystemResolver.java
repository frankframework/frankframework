/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.filesystem;

import lombok.Getter;

/**
 * EFS could be used with a static mailAddress attribute or could be used in a dynamic way.
 *
 * It is possible to specify a dynamic mailbox in folder names by using the separator "|".
 * For example: "test@example.com|Sub folder I"
 *
 *
 * This class encapsulates this logic away from EFS.
 *
 * @author Laurens Mäkel.
 */
public class ExchangeFileSystemResolver {
	private final String SEPARATOR = "|";
	private final String SEPARATOR_PATTERN = "\\|";

	private @Getter String mailbox;
	private @Getter String folderName;

	public ExchangeFileSystemResolver(String folderName, String staticMailAddress){
		this.mailbox = getMailboxToUse(folderName, staticMailAddress);
		this.folderName = getFolderNameToUse(folderName);
	}

	private String getFolderNameToUse(String folderName){
		return folderName.contains(SEPARATOR) ? separateFolderName(folderName) : folderName;
	}

	private String getMailboxToUse(String folderName, String staticMailAddress){
		return folderName.contains(SEPARATOR) ? separateMailbox(folderName) : staticMailAddress;
	}

	private String separateFolderName(String concatenatedString){
		return concatenatedString.split(SEPARATOR_PATTERN)[1];
	}

	private String separateMailbox(String concatenatedString){
		return concatenatedString.split(SEPARATOR_PATTERN)[0];
	}

}
