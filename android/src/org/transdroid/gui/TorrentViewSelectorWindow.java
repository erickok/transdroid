package org.transdroid.gui;

import java.util.List;

import org.example.qberticus.quickactions.BetterPopupWindow;
import org.transdroid.R;
import org.transdroid.gui.util.ArrayAdapter;

import android.content.Context;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class TorrentViewSelectorWindow extends BetterPopupWindow {

	private final MainViewTypeSelectionListener mainViewTypeSelectionListener;
	private final LabelSelectionListener labelSelectionListener;

	private ViewGroup rootView;
	private ListView labelsListView;
	private LayoutInflater inflater;

	public TorrentViewSelectorWindow(View anchor, MainViewTypeSelectionListener mainViewTypeSelectionListener, LabelSelectionListener labelSelectionListener) {
		super(anchor);
		this.mainViewTypeSelectionListener = mainViewTypeSelectionListener;
		this.labelSelectionListener = labelSelectionListener;
	}

	@Override
	protected void onCreate() {
		
		// Inflate layout
		inflater = (LayoutInflater) this.anchor.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		rootView = (ViewGroup) inflater.inflate(R.layout.part_quickaction, null);
		
		// Setup button events
		((ImageButton)rootView.findViewById(R.id.showall)).setOnClickListener(getOnMainViewTypeClickListener(MainViewType.ShowAll));
		((ImageButton)rootView.findViewById(R.id.showdl)).setOnClickListener(getOnMainViewTypeClickListener(MainViewType.OnlyDownloading));
		((ImageButton)rootView.findViewById(R.id.showup)).setOnClickListener(getOnMainViewTypeClickListener(MainViewType.OnlyUploading));
		((ImageButton)rootView.findViewById(R.id.showactive)).setOnClickListener(getOnMainViewTypeClickListener(MainViewType.OnlyActive));
		((ImageButton)rootView.findViewById(R.id.showinactive)).setOnClickListener(getOnMainViewTypeClickListener(MainViewType.OnlyInactive));
        labelsListView = (ListView) rootView.findViewById(R.id.labelsList);
        labelsListView.setOnItemClickListener(onLabelClickListener);
        
		// set the inflated view as what we want to display
		this.setContentView(rootView);
		
	}
	
	@Override
	public void showLikePopDownMenu() {

		// Place arrow
		int[] location = new int[2];
		anchor.getLocationOnScreen(location);
		Rect anchorRect = new Rect(location[0], location[1], location[0] + anchor.getWidth(), location[1] + anchor.getHeight());
		final ImageView arrow = (ImageView) rootView.findViewById(R.id.arrow_up);
        ViewGroup.MarginLayoutParams param = (ViewGroup.MarginLayoutParams)arrow.getLayoutParams();
        final int arrowWidth = arrow.getMeasuredWidth();
        param.leftMargin = anchorRect.centerX() - arrowWidth / 2;

        super.showLikePopDownMenu();
        
	}

	@SuppressWarnings("unchecked")
	public void updateLabels(List<String> availableLabels) {
		// Update the labels list
		if (labelsListView.getAdapter() == null) {
			labelsListView.setAdapter(new ArrayAdapter<String>(this.anchor.getContext(), availableLabels) {
				@Override
				public View getView(int position, View convertView, ViewGroup parent) {
					// Get the right view, using a ViewHolder
					ViewHolder holder;
					if (convertView == null) {
						convertView = inflater.inflate(R.layout.list_item_label, null);
						holder = new ViewHolder();
						holder.text1 = (TextView) convertView.findViewById(android.R.id.text1);
						convertView.setTag(holder);
					} else {
						holder = (ViewHolder) convertView.getTag();
					}
					
					// Bind the data
					holder.text1.setText(getItem(position));
					return convertView;
				}
			});
		} else {
			((ArrayAdapter<String>)labelsListView.getAdapter()).replace(availableLabels);
		}
		labelsListView.setVisibility(availableLabels.size() > 0? View.VISIBLE: View.GONE);
        labelsListView.setOnItemClickListener(onLabelClickListener);
	}

	protected static class ViewHolder {
		TextView text1;
	}
	
	private OnClickListener getOnMainViewTypeClickListener(final MainViewType type) {
		return new OnClickListener() {
			@Override
			public void onClick(View v) {
				mainViewTypeSelectionListener.onMainViewTypeSelected(type);
				TorrentViewSelectorWindow.this.dismiss();
			}
		};
	}

	private OnItemClickListener onLabelClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			labelSelectionListener.onLabelSelected(position);
			TorrentViewSelectorWindow.this.dismiss();
		}
	};

	public static abstract class MainViewTypeSelectionListener {
		public abstract void onMainViewTypeSelected(MainViewType newType);
	}

	public static abstract class LabelSelectionListener {
		public abstract void onLabelSelected(int labelPosition);
	}

}
