package org.transdroid.gui;

import java.util.ArrayList;

import org.transdroid.R;
import org.transdroid.daemon.Daemon;
import org.transdroid.daemon.Torrent;
import org.transdroid.daemon.TorrentDetails;
import org.transdroid.daemon.TorrentFile;
import org.transdroid.daemon.TorrentStatus;
import org.transdroid.daemon.util.FileSizeConverter;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TableRow;
import android.widget.TextView;

import com.commonsware.cwac.merge.MergeAdapter;

public class DetailsListAdapter extends MergeAdapter {

	private static final String DECIMAL_FORMATTER = "%.1f";
	
	private DetailsFragment detailsFragment;
	private Torrent torrent;
	private TorrentDetails fineDetails;
	private TorrentFileListAdapter filesAdapter;

	private View detailsfields;
    private TextView name, state, size,  downloaded, uploaded, rate, eta, peers, availability, label, trackers, trackershint, 
    	errors, errorshint;
    private TableRow availabilityRow, labelRow, trackers1Row, trackers2Row, errors1Row, errors2Row;
    private ImageButton resumepause, startstop, remove, setlabel;

    private boolean showingTrackers = false;
    private boolean showingErrors = false;
	
	public DetailsListAdapter(DetailsFragment detailsFragment, Torrent torrent, TorrentDetails fineDetails) {
		this.detailsFragment = detailsFragment;
		this.torrent = torrent;
		this.fineDetails = fineDetails;
		
		// Add the standard details fields form details_header.xml
		detailsfields = detailsFragment.getActivity().getLayoutInflater().inflate(R.layout.part_details_header, null);
		addView(detailsfields);

        name = (TextView) findViewById(R.id.details_name);
        state = (TextView) findViewById(R.id.details_state);
        size = (TextView) findViewById(R.id.details_size);
        downloaded = (TextView) findViewById(R.id.details_downloaded);
        uploaded = (TextView) findViewById(R.id.details_uploaded);
        rate = (TextView) findViewById(R.id.details_rate);
        eta = (TextView) findViewById(R.id.details_eta);
        peers = (TextView) findViewById(R.id.details_peers);
        availability = (TextView) findViewById(R.id.details_availability);
        label = (TextView) findViewById(R.id.details_label);
        trackers = (TextView) findViewById(R.id.details_trackers);
        trackershint = (TextView) findViewById(R.id.details_trackershint);
        trackershint.setOnClickListener(onTrackersExpandClick);
        errors = (TextView) findViewById(R.id.details_errors);
        errorshint = (TextView) findViewById(R.id.details_errorshint);
        errorshint.setOnClickListener(onErrorsExpandClick);
        
        availabilityRow = (TableRow) findViewById(R.id.detailsrow_availability);
        labelRow = (TableRow) findViewById(R.id.detailsrow_label);
        trackers1Row = (TableRow) findViewById(R.id.detailsrow_trackers1);
        trackers2Row = (TableRow) findViewById(R.id.detailsrow_trackers2);
        errors1Row = (TableRow) findViewById(R.id.detailsrow_errors1);
        errors2Row = (TableRow) findViewById(R.id.detailsrow_errors2);
        
        resumepause = (ImageButton) findViewById(R.id.resumepause);
        startstop = (ImageButton) findViewById(R.id.startstop);
        remove = (ImageButton) findViewById(R.id.remove);
        setlabel = (ImageButton) findViewById(R.id.setlabel);
        resumepause.setOnClickListener(onResumePause);
        startstop.setOnClickListener(onStartStop);
        remove.setOnClickListener(onRemove);
        setlabel.setOnClickListener(onSetLabel);
		
		filesAdapter = new TorrentFileListAdapter(detailsFragment, new ArrayList<TorrentFile>());
		addAdapter(filesAdapter);
		
		updateViewsAndButtonStates();
	}
	
	private View findViewById(int id) {
		return detailsfields.findViewById(id);
	}

	public Torrent getTorrent() {
		return this.torrent;
	}
	
	public TorrentFileListAdapter getTorrentFileAdapter() {
		return filesAdapter;
	}

	public void setTorrent(Torrent torrent) {
		this.torrent = torrent;
	}

	public void setTorrentDetails(TorrentDetails fineDetails) {
		this.fineDetails = fineDetails;
	}

	void updateViewsAndButtonStates() {

		if (name != null) {
			// In case we have a name field (i.e. in tablet layouts) 
			name.setText(torrent.getName());
		}
		
		// Update textviews according to the torrent data
        LocalTorrent local = LocalTorrent.fromTorrent(torrent);
		state.setText(torrent.getStatusCode().toString());
		size.setText(FileSizeConverter.getSize(torrent.getTotalSize()));
		downloaded.setText(FileSizeConverter.getSize(torrent.getDownloadedEver()) + " (" + 
			String.format(DECIMAL_FORMATTER, torrent.getDownloadedPercentage() * 100) + "%)");
		uploaded.setText(FileSizeConverter.getSize(torrent.getUploadedEver()) + " (" + 
			detailsFragment.getString(R.string.status_ratio, local.getRatioString()) + ")");
		rate.setText(local.getProgressSpeedText(detailsFragment.getResources()));
		if (torrent.getStatusCode() == TorrentStatus.Downloading) {
			eta.setText(local.getRemainingTimeString(detailsFragment.getResources(), true));
			availability.setText(String.format(DECIMAL_FORMATTER, torrent.getAvailability() * 100) + "%");
		} else {
			eta.setText("");
			availability.setText("");
		}
		peers.setText(local.getProgressConnectionText(detailsFragment.getResources()));
		label.setText((torrent.getLabelName() == null || torrent.getLabelName().equals(""))? 
			detailsFragment.getString(R.string.labels_unlabeled): torrent.getLabelName());
		if (fineDetails == null || fineDetails.getTrackers() == null) {
			trackers.setText("");
			trackershint.setText("");
		} else {
			trackers.setText(fineDetails.getTrackersText());
			if (showingTrackers) {
				trackershint.setText(detailsFragment.getString(R.string.details_trackers_collapse));
			} else {
				trackershint.setText(detailsFragment.getString(R.string.details_trackers_expand, 
					fineDetails.getTrackers().size() > 0? fineDetails.getTrackers().get(0): ""));
			}
		}
		String errorsText = 
			torrent.getError() != null? torrent.getError() + 
				(fineDetails != null && fineDetails.getErrors() != null && !fineDetails.getErrors().isEmpty()? "\n" + fineDetails.getErrorsText(): ""): 
			(fineDetails != null && fineDetails.getErrors() != null && !fineDetails.getErrors().isEmpty()? fineDetails.getErrorsText(): 
			null);
		if (errorsText == null) {
			errors.setText("");
			errorshint.setText("");
		} else {
			errors.setText(errorsText);
			if (showingErrors) {
				errorshint.setText(detailsFragment.getString(R.string.details_trackers_collapse));
			} else {
				errorshint.setText(detailsFragment.getString(R.string.details_trackers_expand, 
					errorsText.split("\n")[0]));
			}
		}
		
		availabilityRow.setVisibility(Daemon.supportsAvailability(detailsFragment.getActiveDaemonType())? View.VISIBLE: View.GONE);
		labelRow.setVisibility(Daemon.supportsLabels(detailsFragment.getActiveDaemonType())? View.VISIBLE: View.GONE);
		trackers1Row.setVisibility(Daemon.supportsFineDetails(detailsFragment.getActiveDaemonType())? View.VISIBLE: View.GONE);
		trackers2Row.setVisibility(showingTrackers? View.VISIBLE: View.GONE);
		errors1Row.setVisibility(errorsText != null? View.VISIBLE: View.GONE);
		errors2Row.setVisibility(showingErrors? View.VISIBLE: View.GONE);
		
		// Update buttons
		if (torrent.canPause()) {
			resumepause.setImageResource(R.drawable.icon_pause);
		} else {
			resumepause.setImageResource(R.drawable.icon_resume);
		}
		if (Daemon.supportsStoppingStarting(detailsFragment.getActiveDaemonType())) {
			if (torrent.canStop()) {
				startstop.setImageResource(R.drawable.icon_stop);
			} else {
				startstop.setImageResource(R.drawable.icon_start);
			}
		} else {
			startstop.setVisibility(View.GONE);
		}
		setlabel.setVisibility(Daemon.supportsSetLabel(detailsFragment.getActiveDaemonType())? View.VISIBLE: View.GONE);
	}

	private OnClickListener onTrackersExpandClick = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// Show (or hide) the list of full trackers (and adjust the hint text accordingly)
			showingTrackers = !showingTrackers;
			trackers2Row.setVisibility(showingTrackers? View.VISIBLE: View.GONE);
			if (showingTrackers) {
				trackershint.setText(detailsFragment.getString(R.string.details_trackers_collapse));
			} else {
				trackershint.setText(detailsFragment.getString(R.string.details_trackers_expand, 
						fineDetails != null && fineDetails.getTrackers() != null && fineDetails.getTrackers().size() > 0? 
								fineDetails.getTrackers().get(0): ""));
			}
		}
	};

	private OnClickListener onErrorsExpandClick = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// Show (or hide) the list of full trackers (and adjust the hint text accordingly)
			showingErrors = (torrent.getError() != null || (fineDetails != null && 
				fineDetails.getErrors() != null && !fineDetails.getErrors().isEmpty())) && !showingErrors;
			errors2Row.setVisibility(showingErrors? View.VISIBLE: View.GONE);
			if (showingErrors) {
				errorshint.setText(detailsFragment.getString(R.string.details_trackers_collapse));
			} else {
				errorshint.setText(detailsFragment.getString(R.string.details_trackers_expand, 
						fineDetails != null && fineDetails.getErrors() != null && fineDetails.getErrors().size() > 0? 
								fineDetails.getErrors().get(0): ""));
			}
		}
	};
	
	private OnClickListener onResumePause = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (torrent.canPause()) {
				detailsFragment.pauseTorrent();
			} else {
				detailsFragment.resumeTorrent();
			}
		}
	};

	private OnClickListener onStartStop = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (torrent.canStop()) {
				detailsFragment.stopTorrent();
			} else {
				detailsFragment.startTorrent(false);
			}
		}
	};

	private OnClickListener onRemove = new OnClickListener() {
		@Override
		public void onClick(View v) {
			detailsFragment.showDialog(DetailsFragment.DIALOG_ASKREMOVE);
		}
	};

	private OnClickListener onSetLabel = new OnClickListener() {
		@Override
		public void onClick(View v) {
			detailsFragment.showDialog(DetailsFragment.DIALOG_SETLABEL);
		}
	};

}
