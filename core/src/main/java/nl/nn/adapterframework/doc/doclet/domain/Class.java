package nl.nn.adapterframework.doc.doclet.domain;

import org.json.JSONException;
import org.json.JSONWriter;

import java.util.ArrayList;

public class Class implements JsonFormat {

    public String getName() {
        return name;
    }

    private String name;
    private String description;
    private ArrayList<Method> methods;

    public Class(String name, String description, ArrayList<Method> methods) {
        this.name = name;
        this.description = description;
        this.methods = methods;
    }

    public JSONWriter appendToJsonWriter(JSONWriter writer) throws JSONException {
        writer.object();

        writer.key("name").value(name);
        writer.key("description").value(description);
        writer.key("methods").array();
        for (Method method : methods) {
            writer.value(method.appendToJsonWriter(writer));
        }

        writer.endArray().endObject();

        return writer;
    }
}
