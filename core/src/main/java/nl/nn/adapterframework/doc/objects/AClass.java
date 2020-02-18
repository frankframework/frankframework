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
import nl.nn.adapterframework.doc.IbisDocRef;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

/**
 * Represents the Class object for the IbisDoc application
 *
 * @author Chakir el Moussaoui
 */
public class AClass {

    private Class clazz;
    private String javadocLink;
    private ArrayList<String> superClasses;
    private ArrayList<AMethod> methods;
    private String referredClassName = "";


    public AClass(Class clazz) {
        this.clazz = clazz;
        this.superClasses = new ArrayList<>();
        this.methods = new ArrayList<>();
    }
    /**
     * Get the superclasses of a certain class.
     *
     * @param referredClassName - The class we have to derive the superclasses from
     */
    public void setSuperclasses(String referredClassName) {
        if (!referredClassName.isEmpty()) {
            superClasses.add(referredClassName);
        }
        Class superClass = this.clazz;
        while (superClass.getSuperclass() != null) {

            // Assign a string to the array of superclasses
            superClasses.add(superClass.getSuperclass().getSimpleName());
            superClass = superClass.getSuperclass();
        }
    }

    /**
     * Add the methods to the class object.
     *
     * @param beanProperties - The properties of a class
     * @param newClass       - The class object we have to add the methods to
     */
    public void setMethods(Map<String, Method> beanProperties, AClass newClass) {
        Iterator<String> iterator = new TreeSet<>(beanProperties.keySet()).iterator();
        setReferredClassName("");
        while (iterator.hasNext()) {

            // Get the method
            String property = iterator.next();
            Method method = beanProperties.get(property);
            AMethod aMethod = new AMethod(property);

            // Get the IbisDocRef
            IbisDocRef reference = AnnotationUtils.findAnnotation(method, IbisDocRef.class);

            // Get the IbisDoc values from the annotations above the method
            IbisDoc ibisDoc = AnnotationUtils.findAnnotation(method, IbisDoc.class);

            // Check for whether the method (attribute) is deprecated
            Deprecated deprecated = AnnotationUtils.findAnnotation(method, Deprecated.class);
            boolean isDeprecated = deprecated != null;

            String order = "";
            String originalClassName = "";

            // If there is an IbisDocRef for the method, get the IbisDoc of the referred method
            if (reference != null) {
                order = reference.value()[0];
                ibisDoc = aMethod.getIbisDocRef(reference.value()[1], method);
                setReferredClassName(aMethod.getReferredClassName());
                originalClassName = aMethod.getReferredClassName();
            }

            // If there is an IbisDoc for the method, add the method and it's IbisDoc values to the class object
            if (ibisDoc != null) {
                String[] ibisdocValues = ibisDoc.value();
                String[] values = aMethod.getValues(ibisdocValues);

                // This is done for @IbisDoc use instead of @IbisDocRef
                if (order.isEmpty()) order = values[2];

                // This is done for @IbisDoc use instead of @IbisDocRef
                if (originalClassName.isEmpty()) originalClassName = method.getDeclaringClass().getSimpleName();

                aMethod.setOriginalClassName(originalClassName);
                aMethod.setDescription(values[0]);
                aMethod.setDefaultValue(values[1]);
                aMethod.setOrder(Integer.parseInt(order));
                aMethod.setDeprecated(isDeprecated);
                aMethod.setReferredClassName(getReferredClassName());

                newClass.addMethod(aMethod);
            }
        }
        newClass.setSuperclasses(getReferredClassName());
    }

    public void addMethod(AMethod method) {
        methods.add(method);
    }

    public Class getClazz() {
        return this.clazz;
    }

    public String getJavadocLink() {
        return javadocLink;
    }

    public void setJavadocLink(String javadocLink) {
        this.javadocLink = javadocLink;
    }

    public ArrayList<String> getSuperClasses() {
        return superClasses;
    }

    public ArrayList<AMethod> getMethods() {
        return methods;
    }

    public String getReferredClassName() {
        return this.referredClassName;
    }

    public void setReferredClassName(String referredClassName) {
        this.referredClassName = referredClassName;
    }
}