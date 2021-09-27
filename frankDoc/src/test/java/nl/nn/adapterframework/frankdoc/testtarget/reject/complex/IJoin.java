package nl.nn.adapterframework.frankdoc.testtarget.reject.complex;

/*
 * Descendants of this interface have the attributes inherited from INew1,
 * which are superseded1 and superseded2. The numbers 3 and 4 are inherited
 * from ISuperseded only and they are excluded in descendants.
 */
public interface IJoin extends ISuperseded, INew1 {
	void setSuperseded3(String value);
}
