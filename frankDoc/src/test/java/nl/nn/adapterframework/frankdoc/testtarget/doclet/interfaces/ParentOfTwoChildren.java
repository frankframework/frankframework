package nl.nn.adapterframework.frankdoc.testtarget.doclet.interfaces;

public interface ParentOfTwoChildren extends GrandParent1, GrandParent2 {
	@Deprecated
	void annotatedMethod(String value);
}
