package nl.nn.adapterframework.doc.doclet;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class DocletMain {

    public static void main(String[] args) throws IOException, JSONException {
        getTheJson jsoon = new getTheJson();
        String result = jsoon.getJsoon();

        int i = result.indexOf("{");
        result = result.substring(i);
        JSONObject json = new JSONObject(result.trim());
        System.out.println(json.toString(2));
    }
}
