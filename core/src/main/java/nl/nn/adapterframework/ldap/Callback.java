/*
 * $Log$
 */
package nl.nn.adapterframework.ldap;

public interface Callback<K,V> {
	
	public void handle(K key, V value);

}
