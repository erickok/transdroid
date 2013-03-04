package org.transdroid.lite.gui.navigation;

/**
 * Represents some label that is active or available on the server.
 * @author Eric Kok
 */
public class Label implements FilterItem {

	private final String name;

	public Label(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

}
