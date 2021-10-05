package nl.nn.adapterframework.javadoc.test;

public class FieldOwner {
	public final String finalFieldDirectlyInitialized = "finalFieldDirectlyInitialized the value";
	public final String finalFieldInitializedInConstructor;
	public String nonFinalFieldInitialized = "nonFinalFieldInitialized the value";

	public FieldOwner() {
		finalFieldInitializedInConstructor = "finalFieldInitializedInConstructor the value";
	}
}
