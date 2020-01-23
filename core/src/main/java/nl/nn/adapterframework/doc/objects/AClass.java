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

    public AClass(String name, String packageName, String javadocLink, ArrayList<String> superClasses) {
        this.name = name;
        this.packageName = packageName;
        this.javadocLink = javadocLink;
        this.superClasses = superClasses;
        this.methods = new ArrayList<AMethod>();
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