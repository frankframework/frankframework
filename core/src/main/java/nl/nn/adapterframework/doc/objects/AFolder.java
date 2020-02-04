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

import nl.nn.adapterframework.doc.IbisDocPipe;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeSet;

/**
 * Represents the Folder object for the IbisDoc application
 *
 * @author Chakir el Moussaoui
 */
public class AFolder {

    private String name;
    private ArrayList<AClass> classes;

    public AFolder(String name) {
        this.name = name;
        this.classes = new ArrayList<>();
    }

    /**
     * Add classes to the folder object.
     *
     * @param groups - Contains all information
     * @param folder - The folder object we have to add the classes to
     */
    public void setClasses(Map<String, TreeSet<IbisBean>> groups, AFolder folder) {
        for (IbisBean ibisBean : groups.get(folder.getName())) {
            Map<String, Method> beanProperties = IbisDocPipe.getBeanProperties(ibisBean.getClazz());
            if (!beanProperties.isEmpty()) {
                AClass aClass = new AClass(ibisBean.getName());

                // Get the javadoc link for the class
                String javadocLink = ibisBean.getClazz().getName().replaceAll("\\.", "/");

                // Create a new class and add the methods (attributes) to it, then add it to the folder object
                aClass.setJavadocLink(javadocLink);
                aClass.setPackageName(ibisBean.getClazz().getName());
                aClass.setMethods(beanProperties, aClass);
                folder.addClass(aClass);
            }
        }
    }
    public void addClass(AClass clazz) {
        this.classes.add(clazz);
    }

    public String getName() {
        return name;
    }

    public ArrayList<AClass> getClasses() {
        return this.classes;
    }
}