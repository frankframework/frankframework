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

import java.util.ArrayList;

/**
 * Represents the Class object for the IbisDoc application
 *
 * @author Chakir el Moussaoui
 */
public class AClass {

    private String name;
    private String packageName; // Contains the entire class name
    private String javadocLink;
    private ArrayList<String> superClasses;
    private ArrayList<AMethod> methods;

    public AClass(String name, String packageName, String javadocLink) {
        this.name = name;
        this.packageName = packageName;
        this.javadocLink = javadocLink;
        this.superClasses = new ArrayList<>();
        this.methods = new ArrayList<AMethod>();
    }
    /**
     * Get the superclasses of a certain class.
     *
     * @param referredClassName - The class we have to derive the superclasses from
     * @return An ArrayList containing all the superclasses with priority given to
     *         them
     */
    public void addSuperClasses(String referredClassName) {
        if (!referredClassName.isEmpty()) {
            superClasses.add(referredClassName);
        }
        Class<?> clazz = null;
        try {
            clazz = Class.forName(packageName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("["  + packageName + "] is not found");
        }

        while (clazz != null && clazz.getSuperclass() != null) {

            // Assign a string to the array of superclasses
            superClasses.add(clazz.getSuperclass().getSimpleName());
            clazz = clazz.getSuperclass();
        }
    }


    public void addMethod(AMethod method) {
        methods.add(method);
    }

    public String getName() {
        return name;
    }

    public String getPackageName() {
        return packageName;
    }
    
    public String getJavadocLink() {
    	return javadocLink;
    }

    public ArrayList<AMethod> getMethods() {
        return methods;
    }
    
    public ArrayList<String> getSuperClasses() {
    	return superClasses;
    }
}