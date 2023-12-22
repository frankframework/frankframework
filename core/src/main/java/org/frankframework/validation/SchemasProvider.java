/*
   Copyright 2013, 2015 Nationale-Nederlanden

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
package org.frankframework.validation;

import java.util.List;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;

/*
 * When getSchemasId() returns a value, the schemas loaded at initialisation time
 * will be used to validate, otherwise the run time schemas are loaded and used.
  */
public interface SchemasProvider {

	/*
	 * Id of schemas to load at initialisation time.
	 */
	String getSchemasId() throws ConfigurationException;

	/*
	 * Schemas to load at initialisation time.
	 */
	List<Schema> getSchemas() throws ConfigurationException;

	/*
	 * Id of schemas to load at run time.
	 */
	String getSchemasId(PipeLineSession session) throws PipeRunException;

	/*
	 * Schemas to load at run time.
	 */
	List<Schema> getSchemas(PipeLineSession session) throws PipeRunException;

}
