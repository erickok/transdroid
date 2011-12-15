/*
 *	This file is part of Transdroid <http://www.transdroid.org>
 *	
 *	Transdroid is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *	
 *	Transdroid is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *	
 *	You should have received a copy of the GNU General Public License
 *	along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 *	
 */
 package org.transdroid.preferences;

import android.content.Context;
import android.preference.CheckBoxPreference;

/**
 * Wrapper class for CheckboxPreference to expose the onClick() method
 * 
 * @author erickok
 *
 */
public class TransdroidCheckBoxPreference extends CheckBoxPreference {
	public TransdroidCheckBoxPreference(Context context) { super(context); }
	public void click() { onClick(); }
}
