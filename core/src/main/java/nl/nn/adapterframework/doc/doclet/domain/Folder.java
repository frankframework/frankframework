package nl.nn.adapterframework.doc.doclet.domain;

import org.json.JSONException;
import org.json.JSONWriter;

import java.util.ArrayList;

public class Folder implements JsonFormat {

    private String name;
    private ArrayList<Class> classes;

    public Folder(String name, ArrayList<Class> classes) {
        this.name = name;
        this.classes = classes;
    }

    public JSONWriter appendToJsonWriter(JSONWriter writer) throws JSONException {
        writer.object();

        writer.key("name").value(name);
        writer.key("classes");
        writer.array();
        for (Class clazz : classes) {
            writer.value(clazz.appendToJsonWriter(writer));
        }

        writer.endArray().endObject();

        return writer;
    }
}
