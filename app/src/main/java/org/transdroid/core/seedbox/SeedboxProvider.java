/* 
 * Copyright 2010-2013 Eric Kok et al.
 * 
 * Transdroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Transdroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.transdroid.core.seedbox;

import org.transdroid.core.gui.settings.KeyBoundPreferencesActivity;

/**
 * Enumeration of all available seedbox types. Every type needs a {@link SeedboxSettings} implementation to access and 
 * modify settings, which includes a {@link KeyBoundPreferencesActivity} to allow a user to configure the settings.
 * @author Eric Kok
 */
public enum SeedboxProvider {

	Dediseedbox {
		@Override
		public SeedboxSettings getSettings() {
			return new DediseedboxSettings();
		}
	},
	Seedstuff {
		@Override
		public SeedboxSettings getSettings() {
			return new SeedstuffSettings();
		}
	},
	XirvikShared {
		@Override
		public SeedboxSettings getSettings() {
			return new XirvikSharedSettings();
		}
	},
	XirvikSemi {
		@Override
		public SeedboxSettings getSettings() {
			return new XirvikSemiSettings();
		}
	},
	XirvikDedi {
		@Override
		public SeedboxSettings getSettings() {
			return new XirvikDediSettings();
		}
	};
	
	public abstract SeedboxSettings getSettings();
	
}
