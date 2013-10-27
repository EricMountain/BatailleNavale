package fr.les_enry.bataillenavale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Asks if the user wants to reset the game.
 */
public class ResetDialogFragment extends DialogFragment {
	public interface ResetDialogListener {
		public void onPositiveButton();
	}

	ResetDialogListener resetDialogListener;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			resetDialogListener = (ResetDialogListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " needs to implement ResetDialogFragment.ResetDialogListener");
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(R.string.dialog_reset_game)
				.setPositiveButton(android.R.string.yes,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int id) {
								resetDialogListener.onPositiveButton();
							}
						})
				.setNegativeButton(android.R.string.no,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int id) {
								// Do nowt
							}
						});

		return builder.create();
	}
}
