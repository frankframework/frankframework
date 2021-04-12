package nl.nn.adapterframework.frankdoc.model;

import nl.nn.adapterframework.frankdoc.doclet.FrankClass;

/**
 * This class is temporary and it is meant to compare the doclet-produced Frank!Doc with the runtime produced Frank!Doc.
 * When we are not interested in this comparison anymore, we will remove this class. This moment will come
 * when we will gather more information from Javadocs instead of IbisDoc and IbisDocRef annotations.
 * @author martijn
 *
 */
enum JavadocStrategy {
	IGNORE_JAVADOC(new DelegateIgnoreJavadoc()),
	USE_JAVADOC(new DelegateUseJavadoc());

	private final Delegate delegate;

	private JavadocStrategy(Delegate delegate) {
		this.delegate = delegate;
	}

	void completeFrankElement(FrankElement frankElement, FrankClass frankClass) {
		delegate.completeFrankElement(frankElement, frankClass);
	}

	private static abstract class Delegate {
		abstract void completeFrankElement(FrankElement frankElement, FrankClass frankClass);
	}

	private static class DelegateUseJavadoc extends Delegate {
		@Override
		void completeFrankElement(FrankElement frankElement, FrankClass clazz) {
			frankElement.setDescription(clazz.getJavaDoc());
			if(frankElement.getDescription() != null) {
				String[] parts = frankElement.getDescription().split("\n\s*\n");
				frankElement.setDescriptionHeader(parts[0]);
			}
		}
	}

	private static class DelegateIgnoreJavadoc extends Delegate {
		@Override
		void completeFrankElement(FrankElement frankElement, FrankClass clazz) {
		}		
	}
}
