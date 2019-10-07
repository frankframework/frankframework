/*
 * $Log$
 */
package nl.nn.adapterframework.ldap;

public abstract class ObjectCallback<O,K,V> implements Callback<K,V> {

	private O data;
	
	public ObjectCallback(O data) {
		this.data=data;
	}
	
	public O getData() {
		return data;
	}
}
