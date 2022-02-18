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
import org.apache.commons.lang3.StringUtils;

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
	private @Getter String separator = "|";

	private @Getter String mailbox;
	private @Getter String folderName;

	public ExchangeFileSystemResolver(String folderName, String staticMailAddress, String separator){
		if(separator != null){
			this.separator = separator;
		}

		if(folderName == null){
			throw new IllegalArgumentException("Cannot create ExchangeFileSystemResolver when folderName is null!");
		}
		if(staticMailAddress == null && !folderName.contains(getSeparator()) ){
			throw new IllegalArgumentException("Cannot create ExchangeFileSystemResolver when staticMailAddress is null " +
				"and folderName does not contain separator ["+getSeparator()+"]!");
		}

		this.mailbox = getMailboxToUse(folderName, staticMailAddress);
		this.folderName = getFolderNameToUse(folderName);
	}

	private String getFolderNameToUse(String folderName){
		return folderName.contains(getSeparator()) ? separateFolderName(folderName) : folderName;
	}

	private String getMailboxToUse(String folderName, String staticMailAddress){
		return folderName.contains(getSeparator()) ? separateMailbox(folderName) : staticMailAddress;
	}

	private String separateFolderName(String concatenatedString){
		return split(concatenatedString)[1];
	}

	private String separateMailbox(String concatenatedString){
		return split(concatenatedString)[0];
	}

	private String[] split(String concatenatedString){
		return StringUtils.split(concatenatedString, getSeparator());
	}

}
