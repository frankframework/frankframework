/* 
Copyright 2021 WeAreFrank! 

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
				String descriptionHeader = frankElement.getDescription();
				int idx = frankElement.getDescription().indexOf('.');
				if(idx >= 0) {
					descriptionHeader = frankElement.getDescription().substring(0, idx + 1);
				}
				frankElement.setDescriptionHeader(descriptionHeader);
			}
		}
	}

	private static class DelegateIgnoreJavadoc extends Delegate {
		@Override
		void completeFrankElement(FrankElement frankElement, FrankClass clazz) {
		}		
	}
}
