/*
   Copyright 2020 Integration Partners

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
package nl.nn.adapterframework.doc;

import nl.nn.adapterframework.doc.objects.AClass;
import nl.nn.adapterframework.doc.objects.AFolder;
import nl.nn.adapterframework.doc.objects.AMethod;
import nl.nn.adapterframework.doc.objects.IbisBean;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

/**
 * Handles the data for the IbisDoc application
 *
 * @author Chakir el Moussaoui
 */
public class IbisDocApp {
    private String referredClassName = "";

    /**
     * Get the Json containing all information concerning the IbisDoc.
     *
     * @return A string containing all information of the IbisDoc
     */
    public String getJson() {
        Map<String, TreeSet<IbisBean>> groups = IbisDocPipe.getGroups();
        IbisDocExtractor extractor = new IbisDocExtractor();
        addFolders(groups, extractor);

        extractor.writeToJsonUrl();

        return extractor.getJsonString();
    }

    /**
     * Add folders to the Json.
     *
     * @param groups    - Contains all information
     * @param extractor - Class that converts the folders into objects
     */
    public void addFolders(Map<String, TreeSet<IbisBean>> groups, IbisDocExtractor extractor) {
        AFolder allFolder = new AFolder("All");
        for (String folder : groups.keySet()) {
            AFolder newFolder = new AFolder(folder);
            addClasses(groups, newFolder);
            extractor.addFolder(newFolder);
        }
        extractor.addFolder(allFolder);
    }

    /**
     * Add classes to the folder object.
     *
     * @param groups - Contains all information
     * @param folder - The folder object we have to add the classes to
     */
    public void addClasses(Map<String, TreeSet<IbisBean>> groups, AFolder folder) {
        for (IbisBean ibisBean : groups.get(folder.getName())) {
            Map<String, Method> beanProperties = IbisDocPipe.getBeanProperties(ibisBean.getClazz());
            if (!beanProperties.isEmpty()) {

                // Get the javadoc link for the class
                String javadocLink = ibisBean.getClazz().getName().replaceAll("\\.", "/");

                // Create a new class and add the methods (attributes) to it, then add it to the folder object
                AClass newClass = new AClass(ibisBean.getName(), ibisBean.getClazz().getName(), javadocLink);
                AClass updatedClass = addMethods(beanProperties, newClass);
                folder.addClass(updatedClass);
            }
        }
    }

    /**
     * Add the methods to the class object.
     *
     * @param beanProperties - The properties of a class
     * @param newClass       - The class object we have to add the methods to
     * @return the AClass with the added AMethods
     */
    public AClass addMethods(Map<String, Method> beanProperties, AClass newClass) {
        Iterator<String> iterator = new TreeSet<>(beanProperties.keySet()).iterator();
        referredClassName = "";
        while (iterator.hasNext()) {

            // Get the method
            String property = iterator.next();
            Method method = beanProperties.get(property);

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
                ibisDoc = getIbisDocRef(reference.value()[1], method);
                originalClassName = referredClassName;
            }

            // If there is an IbisDoc for the method, add the method and it's IbisDoc values to the class object
            if (ibisDoc != null) {
                String[] ibisdocValues = ibisDoc.value();
                String[] values = getValues(ibisdocValues);

                // This is done for @IbisDoc use instead of @IbisDocRef
                if (order.isEmpty()) order = values[2];

                // This is done for @IbisDoc use instead of @IbisDocRef
                if (originalClassName.isEmpty()) originalClassName = method.getDeclaringClass().getSimpleName();

                newClass.addMethod(new AMethod(property, originalClassName, values[0], values[1], Integer.parseInt(order), isDeprecated));
            }
        }

        newClass.addSuperClasses(referredClassName);

        return newClass;
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
            referredClassName = fullClassName.substring(fullClassName.lastIndexOf(".") + 1).trim();
        } else {
            // Get the reference values of this method
            ibisDoc = getRefValues(packageName, method.getName());
            referredClassName = classOrMethod;
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

}
