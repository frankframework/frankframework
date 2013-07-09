/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
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
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version $Id$
 */
public class JndiMap implements Map {
    
    private Context context;
    
	public JndiMap() {
		super();
	}

	public int size() {
		throw new UnsupportedOperationException("Cannot determine size of a JNDI Context");
	}

	public boolean isEmpty() {
		return false;
	}

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

	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException("Can not determine if JNDI Context contains a value");
	}

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

	public void putAll(Map t) {
		for (Iterator iter = t.entrySet().iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			put(entry.getKey(), entry.getValue());
		}
	}

	public void clear() {
		throw new UnsupportedOperationException();
	}

	public Set keySet() {
        throw new UnsupportedOperationException();
	}

	public Collection values() {
        throw new UnsupportedOperationException();
	}

	public Set entrySet() {
        throw new UnsupportedOperationException();
	}


    public void setContext(Context context) {
        this.context = context;
    }
	public Context getContext() {
		return context;
	}

}
