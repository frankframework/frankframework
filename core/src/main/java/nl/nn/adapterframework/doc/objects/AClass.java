package nl.nn.adapterframework.doc.objects;

import java.util.ArrayList;

public class AClass {

    private String name;
    private String packageName;
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