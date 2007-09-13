/*
 * Created on 11-sep-07
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nl.nn.adapterframework.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingException;

/**
 * View a JNDI context as a Map.
 * 
 * For as much as possible, all Map based operations are mapped
 * onto JNDI operations.
 * 
 * You have to set a Context before using the Map; no default-context
 * is instantiated.
 * 
 * @author m00035f
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class JndiMap implements Map {
    
    private Context context;
    
	/**
	 * 
	 */
	public JndiMap() {
		super();
	}

	/* (non-Javadoc)
	 * @see java.util.Map#size()
	 */
	public int size() {
		throw new UnsupportedOperationException("Cannot determine size of a JNDI Context");
	}

	/* (non-Javadoc)
	 * @see java.util.Map#isEmpty()
	 */
	public boolean isEmpty() {
		return false;
	}

	/* (non-Javadoc)
	 * @see java.util.Map#containsKey(java.lang.Object)
	 */
	public boolean containsKey(Object key) {
		String name = keyToString(key);
        try {
			context.lookup(name);
            return true;
		} catch (NamingException e) {
			e.printStackTrace();
            return false;
		}
	}
	private String keyToString(Object key) {
        if (key == null) {
            return null;
        } else {
            return String.valueOf(key);
        }
	}

	/* (non-Javadoc)
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException("Can not determine if JNDI Context contains a value");
	}

	/* (non-Javadoc)
	 * @see java.util.Map#get(java.lang.Object)
	 */
	public Object get(Object key) {
       String name = keyToString(key);
        try {
            Object o = context.lookup(name);
            return o;
        } catch (NamingException e) {
            e.printStackTrace();
            return null;
        }
	}

	/* (non-Javadoc)
	 * @see java.util.Map#put(java.lang.Object, java.lang.Object)
	 */
	public Object put(Object key, Object value) {
       String name = keyToString(key);
        try {
			Object old = context.lookup(name);
            // Name was found; try rebind
            try {
				context.rebind(name, value);
			} catch (NamingException e) {
				throw new RuntimeException("Rebinding name in JNDI failed", e);
			}
            return old;
		} catch (NamingException e) {
			// Try to bind new name in JNDI context
			e.printStackTrace();
            try {
				context.bind(name, value);
                return null;
			} catch (NamingException e1) {
                throw new RuntimeException("Binding new name in JNDI failed", e1);
			}
		}
	}

	/* (non-Javadoc)
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	public Object remove(Object key) {
        String name = keyToString(key);
        try {
            Object old = context.lookup(name);
            // Name was found; try rebind
            try {
                context.unbind(name);
            } catch (NamingException e) {
                throw new RuntimeException("Unbinding name from JNDI failed", e);
            }
            return old;
        } catch (NamingException e) {
            // Name not found, so nothing to unbind
            e.printStackTrace();
            return null;
        }
	}

	/* (non-Javadoc)
	 * @see java.util.Map#putAll(java.util.Map)
	 */
	public void putAll(Map t) {
		for (Iterator iter = t.entrySet().iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			put(entry.getKey(), entry.getValue());
		}
	}

	/* (non-Javadoc)
	 * @see java.util.Map#clear()
	 */
	public void clear() {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see java.util.Map#keySet()
	 */
	public Set keySet() {
        throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see java.util.Map#values()
	 */
	public Collection values() {
        throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see java.util.Map#entrySet()
	 */
	public Set entrySet() {
        throw new UnsupportedOperationException();
	}

    /**
     * @return
     */
    public Context getContext() {
        return context;
    }

    /**
     * @param context
     */
    public void setContext(Context context) {
        this.context = context;
    }

}
