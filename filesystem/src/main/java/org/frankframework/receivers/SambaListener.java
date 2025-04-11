/*
   Copyright 2021 WeAreFrank!

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
package org.frankframework.receivers;

import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.doc.Category;

/**
 * This class is an alias for {@link Samba2Listener}, which should be used instead.
 *
 * {@inheritClassDoc}
 */
@Deprecated(since = "9.0")
@ConfigurationWarning("Class SambaListener is an alias for Samba2Listener. Use Samba2Listener instead.")
@Category(Category.Type.ADVANCED)
public class SambaListener extends Samba2Listener {

}
