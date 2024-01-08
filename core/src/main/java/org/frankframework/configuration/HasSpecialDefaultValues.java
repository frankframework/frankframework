/*
   Copyright 2013, 2020 Nationale-Nederlanden

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
package org.frankframework.configuration;

import java.util.Map;

/**
 * Interface to be implemented when a class has one or more special default
 * values (i.e. a default value of attribute X depends on the value of attribute
 * Y). When a class implements this interface the getSpecialDefaultValue method
 * will be called for every attribute used in the Ibis configuration when it is
 * being loaded and checked for default values (warning "already has a default
 * value"). At this time the set methods and the configure method aren't called
 * yet but the values specified in the Ibis configuration are known. To make use
 * of these values this interface should be implemented.
 *
 * Please note that the normal get method of the attribute should return an
 * object or when it returns a primitive should not return a null value. I.e.
 * when the return type is boolean but a Boolean null value is returned a
 * NullPointerException (wrapped in an InvocationTargetException) will be thrown
 * and written to the logfile as a warning by the code checking for default
 * values because reflection is being used to call the get method. From the
 * javadoc of java.lang.reflect.InvocationHandler: If the value returned by this
 * method is null and the interface method's return type is primitive, then a
 * NullPointerException will be thrown.
 */
public interface HasSpecialDefaultValues {

	Object getSpecialDefaultValue(String attributeName, Object defaultValue, Map<String, String> attributes);

}
