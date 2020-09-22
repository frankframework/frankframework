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
package nl.nn.adapterframework.configuration;

/**
 * Factory for the JMS Realms instance, which holds all JMS Realm definitions
 * found in the Configuration File.
 * 
 * This bean is a singleton, not a prototype, which is why the GenericFactory
 * can not be used.
 * 
 * @author Tim van der Leeuw
 *
 */
public class JmsRealmsFactory extends AbstractSpringPoweredDigesterFactory {

	@Override
	public String getSuggestedBeanName() {
		return "jmsRealmsFactory";
	}

	@Override
	public boolean isPrototypesOnly() {
		return false;
	}
}
