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

import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import microsoft.exchange.webservices.data.property.complex.FolderId;

public class ExchangeObjectReference {

	private final @Getter String mailbox;
	private final @Getter String objectName;
	private final @Getter String originalReference;
	private @Getter @Setter FolderId baseFolderId;

	public ExchangeObjectReference(String objectName, String staticMailAddress, FolderId defaultBaseFolderId, String separator) {
		if(objectName == null){
			throw new IllegalArgumentException("Cannot create ExchangeObjectReference when folderName is null!");
		}
		this.originalReference = objectName;
		String[] items = StringUtils.split(objectName, separator);
		if (items.length > 1) {
			this.mailbox = items[0];
			this.objectName = items[1];
		} else {
			if(staticMailAddress == null){
				throw new IllegalArgumentException("Cannot create ExchangeObjectReference when staticMailAddress is null " +
					"and objectName ["+objectName+"] does not contain separator ["+separator+"]!");
			}
			this.mailbox = staticMailAddress;
			this.objectName = objectName;
			this.baseFolderId = defaultBaseFolderId;
		}
	}

}
