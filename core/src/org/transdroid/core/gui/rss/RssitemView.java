package org.transdroid.core.gui.rss;

import java.util.Date;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.ifies.android.sax.Item;

import android.content.Context;
import android.text.format.DateUtils;
import android.widget.TextView;

/**
 * View that represents some {@link Item} object, which is a single item in some RSS feed.
 * @author Eric Kok
 */
@EViewGroup(resName = "list_item_rssitem")
public class RssitemView extends RssitemStatusLayout {

	// Views
	@ViewById
	protected TextView nameText, dateText;

	public RssitemView(Context context) {
		super(context);
	}

	public void bind(Item rssitem, Date lastViewedItem) {

		nameText.setText(rssitem.getTitle());
		dateText.setText(DateUtils.getRelativeDateTimeString(getContext(), rssitem.getPubdate().getTime(),
				DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_ABBREV_MONTH));
		setIsNew(rssitem.getPubdate().after(lastViewedItem));

	}

}
