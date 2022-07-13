/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.configuration.digester;

/**
 * Factory for retrieving ConfigurationDigester instance from BeanFactory, for
 * use with the 'include' element in the IBIS Configuration XML.
 *
 * The GenericFactory can not be used because it is not desirable to add an
 * alias to the bean 'configurationDigester' under the name 'proto-include',
 * which would be chosen by the GenericFactory, and because the
 * configurationDigester bean is a singleton instead of a prototype.
 *
 * @author Tim van der Leeuw
 */
public class ConfigurationDigesterFactory extends AbstractSpringPoweredDigesterFactory {

	@Override
	public String getSuggestedBeanName() {
		return "configurationDigester";
	}

	@Override
	public boolean isPrototypesOnly() {
		return false;
	}
}
