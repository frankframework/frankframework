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
/*
 * $Log: ConfigurationDigesterFactory.java,v $
 * Revision 1.4  2011-11-30 13:51:56  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:48  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2007/10/24 07:13:21  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Rename abstract method 'getBeanName()' to 'getSuggestedBeanName()' since it better reflects the role of the method in the class.
 *
 * Revision 1.1  2007/10/23 09:17:24  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * ConfigurationDigester is an abstract class to be loaded from the Spring Beans Factory, so add a factory for it
 *
 * 
 * Created on 23-okt-07
 *
 */
package nl.nn.adapterframework.configuration;

/**
 * Factory for retrieving ConfigurationDigester instance from BeanFactory,
 * for use with the 'include' element in the IBIS Configuration XML.
 * 
 * The GenericFactory can not be used because it is not desirable to
 * add an alias to the bean 'configurationDigester' under the name
 * 'proto-include', which would be chosen by the GenericFactory, and
 * because the configurationDigester bean is a singleton instead of a
 * prototype.
 * 
 * @author Tim van der Leeuw
 * @version $Id$
 */
public class ConfigurationDigesterFactory
    extends AbstractSpringPoweredDigesterFactory {

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.configuration.AbstractSpringPoweredDigesterFactory#getBeanName()
     */
    public String getSuggestedBeanName() {
        return "configurationDigester";
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.configuration.AbstractSpringPoweredDigesterFactory#isPrototypesOnly()
     */
    public boolean isPrototypesOnly() {
        return false;
    }

}
