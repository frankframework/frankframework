package nl.nn.adapterframework.doc.objects;

import java.util.ArrayList;

public class AClass {

    private String name;
    private String packageName;
    private ArrayList<AMethod> methods;

    public AClass(String name, String packageName) {
        this.name = name;
        this.packageName = packageName;
        this.methods = new ArrayList<AMethod>();
    }

    public void addMethod(AMethod method) {
        this.methods.add(method);
    }

    public String getName() {
        return this.name;
    }

    public String getPackageName() {
        return packageName;
    }

    public ArrayList<AMethod> getMethods() {
        return this.methods;
    }
}