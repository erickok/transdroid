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
package org.transdroid.gui.search;

/**
 * A class that contains interface settings relating to searches
 * 
 * @author eric
 *
 */
public final class SearchSettings {

	final private int numberOfResults;
	final private boolean sortBySeeders;
	
	/**
	 * Creates a search site settings instance, providing all result options details
	 * @param numberOfResults The number of results to get from the server for a single query
	 * @param sortBySeeders Whether to sort by number of leechers (otherwise a combined sort is used)
	 */
	public SearchSettings(int numberOfResults, boolean sortBySeeders) {
		this.numberOfResults = numberOfResults;
		this.sortBySeeders = sortBySeeders;
	}
		
	/**
	 * Gives how many search results to show in one screen
	 * @return The number of results to show
	 */
	public int getNumberOfResults() {
		return numberOfResults;
	}
	
	/**
	 * Gives the sort method that should be used
	 * @return True if the results should be sorted by number of seeders/leechers, otherwise it should sort using the torrent site default
	 */
	public boolean getSortBySeeders() {
		return sortBySeeders;
	}
	
}
