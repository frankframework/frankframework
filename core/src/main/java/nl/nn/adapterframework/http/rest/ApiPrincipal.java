package nl.nn.adapterframework.http.rest;

import java.io.Serializable;
import java.security.Principal;

public class ApiPrincipal implements Principal, Serializable {

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isLoggedIn() {
		// TODO Auto-generated method stub
		return false;
	}

	public int getID() {
		// TODO Auto-generated method stub
		return 5;
	}
}
