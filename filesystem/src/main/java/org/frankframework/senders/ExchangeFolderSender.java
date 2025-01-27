/*
   Copyright 2019-2024 WeAreFrank!

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
package org.frankframework.senders;

import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.filesystem.AbstractFileSystemSender;

/**
 * Implementation of a {@link AbstractFileSystemSender} that enables to manipulate messages in an Exchange folder.
 */
@Deprecated(forRemoval = true)
@ConfigurationWarning("please use the 'ExchangeFileSystemSender' instead")
public class ExchangeFolderSender extends ExchangeFileSystemSender {
	//NO OP
}
