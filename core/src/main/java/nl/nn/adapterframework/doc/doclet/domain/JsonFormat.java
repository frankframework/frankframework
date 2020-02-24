package nl.nn.adapterframework.doc.doclet.domain;

import org.json.JSONException;
import org.json.JSONWriter;

public interface JsonFormat {

    JSONWriter appendToJsonWriter(JSONWriter writer) throws JSONException;
}
