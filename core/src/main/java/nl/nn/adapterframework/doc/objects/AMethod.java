/*
   Copyright 2019, 2020 Integration Partners

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
package nl.nn.adapterframework.doc.objects;

import nl.nn.adapterframework.doc.IbisDoc;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;

/**
 * Represents the Method/Attribute object for the IbisDoc application
 *
 * @author Chakir el Moussaoui
 */
public class AMethod {

    private String name;
    private String originalClassName; // The name of the class the method was declared in
    private String description;
    private String defaultValue;
    private int order;
    private boolean deprecated;
    private String referredClassName = "";

    public AMethod(String name) {
        this.name = name;
    }

    /**
     * Get the IbisDocRef values and then the IbisDoc values of the referred method
     *
     * @param packageName - The name full name of the class (with a method attached to it)
     * @param method  - The current method
     */
    public IbisDoc getIbisDocRef(String packageName, Method method) {

        IbisDoc ibisDoc;

        // Get the last element of the full package, to check if it is a class or a method
        String classOrMethod = packageName.substring(packageName.lastIndexOf(".") + 1).trim();
        char[] firstLetter = classOrMethod.toCharArray();

        // Check the first letter of the last element (if lower case => method, else class)
        if (Character.isLowerCase(firstLetter[0])) {

            // Get the full class name
            int lastIndexOf = packageName.lastIndexOf(".");
            String fullClassName = packageName.substring(0, lastIndexOf);

            // Get the reference values of the specified method
            ibisDoc = getRefValues(fullClassName, classOrMethod);
            setReferredClassName(fullClassName.substring(fullClassName.lastIndexOf(".") + 1).trim());
        } else {
            // Get the reference values of this method
            ibisDoc = getRefValues(packageName, method.getName());
            setReferredClassName(classOrMethod);
        }

        return ibisDoc;
    }

    /**
     * Gets the IbisDoc values.
     *
     * @param ibisDocValues - The String[] containing all the ibisDocValues
     * @return The needed ibisDocValues
     */
    public String[] getValues(String[] ibisDocValues) {
        String order;
        int desc;
        int def;

        if (ibisDocValues[0].matches("\\d+")) {
            order = ibisDocValues[0];
            desc = 1;
            def = 2;
        } else {
            order = "999";
            desc = 0;
            def = 1;
        }
        if (ibisDocValues.length > def)
            return new String[] { ibisDocValues[desc], ibisDocValues[def], order };
        else
            return new String[] { ibisDocValues[desc], "", order };
    }

    /**
     * Get the IbisDoc values of the referred method in IbisDocRef
     *
     * @param className - The full name of the class
     * @param methodName - The method name
     * @return the IbisDoc of the method
     */
    public IbisDoc getRefValues(String className, String methodName) {
        IbisDoc ibisDoc = null;
        try {
            Class<?> parentClass = Class.forName(className);
            for (Method parentMethod : parentClass.getDeclaredMethods()) {
                if (parentMethod.getName().equals(methodName)) {

                    // Get the IbisDoc values of that method
                    ibisDoc = AnnotationUtils.findAnnotation(parentMethod, IbisDoc.class);
                    break;
                }
            }

        } catch (ClassNotFoundException e) {
            System.out.println("Could not find [" + className + "]");
            e.printStackTrace();
        }

        return ibisDoc;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOriginalClassName() {
        return originalClassName;
    }

    public void setOriginalClassName(String originalClassName) {
        this.originalClassName = originalClassName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public void setReferredClassName(String referredClassName) {
        this.referredClassName = referredClassName;
    }

    public String getReferredClassName() {
        return this.referredClassName;
    }
}