package org.transdroid.core.gui.navigation;

import org.transdroid.core.gui.lists.SimpleListItem;

/**
 * Represents some label that is active or available on the server.
 * @author Eric Kok
 */
public class Label implements SimpleListItem {

	private final String name;

	public Label(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

}
