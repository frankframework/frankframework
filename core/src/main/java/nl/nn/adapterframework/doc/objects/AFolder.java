package nl.nn.adapterframework.doc.objects;

import java.util.ArrayList;

public class AFolder {

    private String name;
    private ArrayList<AClass> classes;

    public AFolder(String name) {
        this.name = name;
        this.classes = new ArrayList<AClass>();
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