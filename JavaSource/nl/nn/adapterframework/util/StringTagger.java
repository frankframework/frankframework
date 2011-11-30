package nl.nn.adapterframework.util;

import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
* Creates an object with tags and fields from a String.
* Its ideal for name-value pairs and name-value pairs with multivalues.
* It also provides support for quoted values, and recognizes values that are 'function' calls with
* their own parameter list (allowing to ignore any tokens within these lists when parsing).
* <br/><br/>
* Example:<br/>
* <pre> <code>
*       StringTagger tag=new StringTagger("cmd=lookup names='Daniel Ockeloen, Rico Jansen'",' ','=',',','\'','(',')');
*       System.out.println("toString:"+tag.toString());
*       System.out.println("cmd:"+tag.Value("cmd"));
*       System.out.println("names: "+tag.Values("names"));
* Result:
* toString:[<cmd=[lookup]><names=[Daniel Ockeloen,  Rico Jansen]>]
* cmd:lookup
* names: [Daniel Ockeloen,  Rico Jansen]
*
*       tag=new StringTagger("cmd=(a,b),c");
*       System.out.println("toString:"+tag.toString());
*       System.out.println("cmd:"+tag.Values("cmd"));
* Result:
* toString:[<cmd=[(a,b), c]>]
* cmd:[(a,b), c]
*
* </code> </pre>
*@version Id
*/
public class StringTagger implements Map {

    /**
     * The name-value pairs where the value is a single string
     */
    private Hashtable tokens;
    /**
     * The name-value pairs where the value is a list of strings
     */
    private Hashtable multitokens;
    /**
     * Token used to separate tags (default a space).
     */
    private char TagStart=' ';
    /**
     * Token used to separate the tag name from its value (default '=').
     */
    private char TagSeperator = '=';
    /**
     * Token used to separate multiple values within a tag (default ',').
     */
    private char FieldSeperator=',';
    /**
     * Token used to indicate quoted values (default '\"').
     */
    private char QuoteChar='\"';
    /**
     * Token used to indicate the start of a function parameter list (default '(').
     */
    private char FunctionOpen='(';
    /**
     * Token used to indicate the end of a function parameter list (default ')').
     */
    private char FunctionClose=')';

    /**
     * The line that was parsed.
     */
    private String startline="";

    /**
     * Creates a StringTag for the given line.
     * Uses default characters for all tokens.
     *
     * @param line : to be tagged line
     */
    public StringTagger(String line) {
        this(line,' ','=',',','"','(',')');
    }
    /**
     * Creates a StringTag for the given line.
     * Uses default characters for the function parameter list tokens.
     * Example : StringTagger("cmd=lookup names='Daniel Ockeloen, Rico Jansen'",' ','=',','\'')
     * @param line : to be tagged line
     * @param TagStart : Seperator for the Tags
     * @param TagSeperator : Seperator inside the Tag (between name and value)
     * @param FieldSeperator : Seperator inside the value
     * @param QuoteChar : Char used if a quoted value
     */
    public StringTagger(String line, char TagStart, char TagSeperator,char FieldSeperator, char QuoteChar) {
        this(line, TagStart, TagSeperator,FieldSeperator, QuoteChar,'(',')');
    }
    /**
     * Creates a StringTag for the given line.
     * Example : StringTagger("cmd=lookup names='Daniel Ockeloen, Rico Jansen'",' ','=',','\'','('.')')
     * @param line : to be tagged line
     * @param TagStart : Seperator for the Tags
     * @param TagSeperator : Seperator inside the Tag (between name and value)
     * @param FieldSeperator : Seperator inside the value
     * @param QuoteChar : Char used if a quoted value
     * @param FunctionOpen char used to open a function parameter list
     * @param FunctionClose char used to close a function parameter list
     */
    public StringTagger(String line, char TagStart, char TagSeperator,char FieldSeperator, char QuoteChar,
                                     char FunctionOpen, char FunctionClose) {
        this.TagStart=TagStart;
        this.startline=line;
        this.TagSeperator=TagSeperator;
        this.FieldSeperator=FieldSeperator;
        this.QuoteChar=QuoteChar;
        this.FunctionOpen=FunctionOpen;
        this.FunctionClose=FunctionClose;
        tokens = new Hashtable();
        multitokens = new Hashtable();
        createTagger(line);
    }
    // Map interface methods

    /**
     * Clears all data
     */
    public void clear() {
        tokens.clear();
        multitokens.clear();
        startline="";
    }
    /**
     * Checks whether a key exits.
     */
    public boolean containsKey (Object ob) {
        return tokens.containsKey(ob);
    }
    /**
     * Checks whether a value exits.
     */
    public boolean containsValue (Object ob) {
        return tokens.containsValue(ob);
    }
    /**
     * Parses the given line, and stores all value-pairs found in the
     * {@link #tokens} and {@link #multitokens} fields.
     * @param line : to be tagged line (why is this a parameter when it can eb retrieved from startline?)
     */
    void createTagger(String line) {
        StringTokenizer tok2=new StringTokenizer(line+TagStart,""+TagSeperator+TagStart,true);
        String part,tag,prevtok,tok;
        boolean isTag,isPart,isQuoted;

        isTag=true;
        isPart=false;
        isQuoted=false;
        prevtok="";
        tag=part="";
//        log.debug("Tagger -> |"+TagStart+"|"+TagSeperator+"|"+QuoteChar+"|");
        while(tok2.hasMoreTokens()) {
            tok=tok2.nextToken();
//            log.debug("tagger tok ("+isTag+","+isPart+","+isQuoted+") |"+tok+"|"+prevtok+"|");
            if (tok.equals(""+TagSeperator)) {
                if (isTag) {
                    tag=prevtok;
                    isTag=false;
                } else {
                    if (!isQuoted) {
                        splitTag(tag+TagSeperator+part);
                        isTag=true;
                        isPart=false;
                        part="";
                    } else {
                        part+=tok;
                    }
                }
            } else if (tok.equals(""+TagStart)) {
                if (isPart) {
                    if (isQuoted) {
                        part+=tok;
                    } else {
                        if (!prevtok.equals(""+TagStart)) {
                            splitTag(tag+TagSeperator+part);
                            isTag=true;
                            isPart=false;
                            part="";
                        }
                    }
                    prevtok=tok;
                }
            } else {
                if (!isTag) isPart=true;
//                log.debug("isTag "+isTag+" "+isPart);
                if (isPart) {
                    if (isQuoted) {
                        // Check end quote
                        if (tok.charAt(tok.length()-1)==QuoteChar) {
                            isQuoted=false;
                        }
                        part+=tok;
                    } else {
                        if (tok.charAt(0)==QuoteChar && !(tok.charAt(tok.length()-1)==QuoteChar)) {
                            isQuoted=true;
                        }
                        part+=tok;
                    }
                }
//                log.debug("isTag "+isTag+" "+isPart+" "+isQuoted);
                prevtok=tok;
            }
        }
    }
    /**
     * Returns a Enumeration of the values as String.
     * The values returned are all single, unsepartated, strings.
     * Use {@link #multiElements} to get a list of multi-values.
     */
    public Enumeration elements() {
        return tokens.elements();
    }
    /**
     *  returns all values
     */
    public Set entrySet() {
        return tokens.entrySet();
    }
    /**
     * Returns whether two objects are the same
     * @param ob the key of the value to retrieve
     */
    public boolean equals(Object ob) {
        return (ob instanceof Map) && (ob.hashCode()==this.hashCode());
    }
    /**
     * Returns the value of a key as an Object.
     * The value returned is a single, unseparated, string.<br>
     * Use {@link #Values} to get a list of multi-values as a <code>Vector</code>.<br>
     * Use {@link #Value} to get the first value as a String
     * @param ob the key of the value to retrieve
     */
    public Object get(Object ob) {
        return tokens.get(ob);
    }
    /**
     *  Hashcode for sorting and comparing
     */
    public int hashCode() {
        return multitokens.hashCode();
    }
    /**
     * Checks whether the tagger is empty
     */
    public boolean isEmpty() {
        return tokens.isEmpty();
    }
    // Custom methods

    /**
     * Returns a Enumeration of the name keys.
     */
    public Enumeration keys() {
        return tokens.keys();
    }
    /**
     * Returns a Set of the name keys.
     */
    public Set keySet() {
        return tokens.keySet();
    }
    /**
     *  For testing
     */
    public static void main(String args[]) {

        StringTagger tag=new StringTagger("cmd=lookup names='Daniel Ockeloen, Rico Jansen'",' ','=',',','\'','(',')');
        System.out.println("toString:"+tag.toString());
        System.out.println("cmd:"+tag.Value("cmd"));
        System.out.println("names: "+tag.Values("names"));
        tag=new StringTagger("cmd=(a,b),c");
        System.out.println("toString:"+tag.toString());
        System.out.println("cmd:"+tag.Values("cmd"));
        String sx=("name=\"jdbc:odbc:dsn=Boeking\"");
        sx+=" ";
        sx+=("driver =\"sun.jdbc.odbc.JdbcOdbcDriver\"");
        StringTagger x=new StringTagger(sx);
        System.out.println(sx);
        System.out.println(x.toString());
       


    }
    /**
     * Returns a Enumeration of the values as Vectors that contain
     * the seperated values.
     * Use {@link #elements} to get a list of single, unseparated, values.
     */
    public Enumeration multiElements(String token) {
        Vector tmp=(Vector)multitokens.get(token);
        if (tmp!=null) {
            return tmp.elements();
        } else {
            return null;
        }
    }
    /**
     *  sets a value (for the Map interface).
     */
    public Object put(Object key, Object value) {
        Object res=tokens.get(key);
        setValue((String)key,(String)value);
        return res;
    }
    /**
     *  Manually sets a set of values (for the Map interface).
     */
    public void putAll(Map map) {
        throw new UnsupportedOperationException();
    }
    /**
     *  remove a value (for the Map interface).
     */
    public Object remove(Object key) {
        Object res=tokens.get(key);
        tokens.remove(key);
        multitokens.remove(key);
        return res;
    }
    /**
     *  Manually sets a single value.
     */
    public void setValue(String token,String val) {
        Vector newval=new Vector();
        newval.addElement(val);
        tokens.put(token,newval);
        multitokens.put(token,newval);
    }
    /**
     *  Manually sets a multi-value value.
     */
    public void setValues(String token,Vector values) {
        tokens.put(token,values.toString());
        multitokens.put(token,values);
    }
    /**
     *  sets a value (for the Map interface).
     */
    public int size() {
        return tokens.size();
    }
    /**
     * Handles and splits a tag in its component parts, and store the elemements in
     * the {@link #tokens} and {@link #multitokens} fields.
     * @param tag the string containing the tag
     */
    void splitTag(String tag) {
        int    tagPos=tag.indexOf(TagSeperator);
        String name=tag.substring(0,tagPos);
        String result=tag.substring(tagPos+1);
//        log.debug("SplitTag |"+name+"|"+result+"|");

        if (result.length()>1 && result.charAt(0)==QuoteChar && result.charAt(result.length()-1)==QuoteChar) {
            result=result.substring(1,result.length()-1);
        }
        tokens.put(name,result);

        StringTokenizer toks = new StringTokenizer(result,""+FieldSeperator+FunctionOpen+FunctionClose, true);
        // If quoted, strip the " " from beginning and end ?
        Vector Multi = new Vector();
        if(toks.hasMoreTokens()) {
            String tokvalue="";
            int nesting = 0;
            while (toks.hasMoreTokens()) {
                String tok=toks.nextToken();
                if (tok.equals(""+FieldSeperator)) {
                    if (nesting==0) {
                        Multi.addElement(tokvalue.trim());
                        tokvalue="";
                    } else {
                        tokvalue+=tok;
                    }
                } else if (tok.equals(""+FunctionOpen)) {
                    nesting++;
                    tokvalue+=tok;
                } else if (tok.equals(""+FunctionClose)) {
                    nesting--;
                    tokvalue+=tok;
                }
                else {
                    tokvalue+=tok;
                }
            }
            Multi.addElement(tokvalue.trim());
        }
        multitokens.put(name,Multi);
    }
    /**
     * toString
     */
    public String toString() {
        String content="[";
        String key="";
        for (Enumeration e = keys();e.hasMoreElements();) {
            key=(String)e.nextElement();
            content+="<"+key;
            content+="="+Values(key);
            content+=">";
        }
        content+="]";
        return content;
    }
    /**
     * Returns the first value as a <code>String</code>.
     * In case of a single value, it returns that value. In case of multiple values,
     * it returns the
     * Use {@link #get} to get the list of values as a <code>String</code><br>
     * Use {@link #Values} to get a list of multi-values as a <code>Vector</code>.<br>
     * @param token the key of the value to retrieve
     */
    public String Value(String token) {
        String val;
        Vector tmp=(Vector)multitokens.get(token);
        if (tmp!=null && tmp.size()>0) {
            val=(String)tmp.elementAt(0);
            if (val!=null) {
                val = StringUtils.strip(val,"\""); // added stripping daniel
                return val;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
    /**
     *  returns all values
     */
    public Collection values() {
        return tokens.values();
    }
    /**
     * Returns the values as a Vector that contains
     * the separated values.<br>
     * Use {@link #get} to get the list of values as a <code>String</code><br>
     * Use {@link #Value} to get the first value as a String
     * @param token the key of the value to retrieve
     */
    public Vector Values(String token) {
        Vector tmp=(Vector)multitokens.get(token);
        return tmp;
    }
    /**
     * Returns the original parsed line
     * @param token unused
     */
    public String ValuesString(String token) {
        return startline;
    }
}
