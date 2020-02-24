package nl.nn.adapterframework.doc.doclet.domain;

import org.json.JSONException;
import org.json.JSONWriter;

public class Method implements JsonFormat {

    private String name;
    private int order;
    private String description;
    private String defaultValue;

    public Method(String name, int order, String description, String defaultValue) {
        this.name = name;
        this.order = order;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    public JSONWriter appendToJsonWriter(JSONWriter writer) throws JSONException {
        writer.object();

        writer.key("name").value(name);
        writer.key("order").value(order);
        writer.key("description").value(description);
        writer.key("defaultValue").value(defaultValue);

        writer.endObject();

        return writer;
    }
}
