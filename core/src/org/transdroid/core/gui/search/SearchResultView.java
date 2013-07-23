package org.transdroid.core.gui.search;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.transdroid.core.R;
import org.transdroid.core.app.search.SearchResult;

import android.content.Context;
import android.text.format.DateUtils;
import android.widget.TextView;
import fr.marvinlabs.widget.CheckableRelativeLayout;

/**
 * View that represents a {@link SearchResult} object from an in-app search
 * @author Eric Kok
 */
@EViewGroup(resName = "list_item_searchresult")
public class SearchResultView extends CheckableRelativeLayout {

	// Views
	@ViewById
	protected TextView nameText, seedersText, leechersText, sizeText, dateText;

	public SearchResultView(Context context) {
		super(context);
	}

	public void bind(SearchResult result) {

		nameText.setText(result.getName());
		sizeText.setText(result.getSize());
		dateText.setText(result.getAddedOn() == null ? "" : DateUtils.getRelativeDateTimeString(getContext(), result
				.getAddedOn().getTime(), DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS,
				DateUtils.FORMAT_ABBREV_MONTH));
		seedersText.setText(getContext().getString(R.string.search_seeders, result.getSeeders()));
		leechersText.setText(getContext().getString(R.string.search_leechers, result.getLeechers()));

	}

}
