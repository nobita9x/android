package eu.ttbox.geoping.ui.geofence;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import java.util.Locale;

import eu.ttbox.geoping.R;
import eu.ttbox.geoping.domain.GeoFenceProvider;
import eu.ttbox.geoping.domain.PairingProvider;
import eu.ttbox.geoping.domain.model.CircleGeofence;
import eu.ttbox.geoping.domain.pairing.GeoFenceDatabase;
import eu.ttbox.geoping.domain.pairing.GeoFenceHelper;
import eu.ttbox.geoping.domain.pairing.PairingDatabase;
import eu.ttbox.geoping.ui.core.BindingHelper;
import eu.ttbox.geoping.ui.core.validator.Form;
import eu.ttbox.geoping.ui.core.validator.validate.ValidateTextView;
import eu.ttbox.geoping.ui.core.validator.validator.NotEmptyValidator;
import eu.ttbox.geoping.ui.core.validator.validator.TextSizeValidator;

public class GeofenceEditFragment extends SherlockFragment {


    private static final String TAG = "GeofenceEditFragment";


    // Binding
    private EditText nameEditText;
    private TextView latLngEditText;

    // Listener
    private OnGeofenceSelectListener onGeofenceSelectListener;

    //Validator
    private Form formValidator;

    // Instance Data
    private Uri entityUri;
    private CircleGeofence fence;


    // ===========================================================
    // Constructors
    // ===========================================================

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.geofence_edit, container, false);
        // Menu on Fragment
        setHasOptionsMenu(true);

        // Bindings
        this.nameEditText = (EditText) v.findViewById(R.id.geofenceEditName);
        this.latLngEditText = (TextView) v.findViewById(R.id.geofenceEditLatLng);
        // Form
        formValidator = createValidator(getActivity());

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated");
        // Load Data
        loadEntity(getActivity().getIntent());
    }



    private void bindingView(CircleGeofence geofence) {
        //Binding
        this.nameEditText.setText(geofence.name);
        // Lat Lng
        double lat = geofence.getLatitude();
        double lng = geofence.getLongitude();
        String coordString = String.format(Locale.US, "(%.6f, %.6f) +/- %s m", lat, lng, geofence.radiusInMeters);
        this.latLngEditText.setText(coordString);

    }

    // ===========================================================
    // Validator
    // ===========================================================

    public Form createValidator(Context context) {
        Form formValidator = new Form();
        // Name
        ValidateTextView nameTextField = new ValidateTextView(nameEditText)//
                .addValidator(new NotEmptyValidator())
                .addValidator(new TextSizeValidator(null, 10));
        formValidator.addValidates(nameTextField);


        return formValidator;
    }
    // ===========================================================
    // Menu
    // ===========================================================

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.geofence_edit_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_save:
                onSaveClick();
                return true;
            case R.id.menu_delete:
                onDeleteClick();
                return true;
            case R.id.menu_cancel:
                onCancelClick();
                return true;
        }
        return false;
    }
    // ===========================================================
    // Interface
    // ===========================================================


    public interface OnGeofenceSelectListener {

        void onGeofenceSelect(Uri id, CircleGeofence fence);

    }


    public void setOnGeofenceSelectListener(OnGeofenceSelectListener onGeofenceSelectListener) {
        this.onGeofenceSelectListener = onGeofenceSelectListener;
    }

    // ===========================================================
    // Load Data
    // ===========================================================


    private void loadEntity(Intent intent) {
        Uri uri = intent.getData();
        if (uri != null) {
            loadEntity(uri);
        } else {
            prepareInsert();
        }
    }

    public void loadEntity(Uri entityUri) {
        this.entityUri = entityUri;
        ContentResolver cr = getActivity().getContentResolver();
        String selection = null;
        String[] selectionArgs = null;
        Cursor cursor = cr.query(entityUri, GeoFenceDatabase.GeoFenceColumns.ALL_COLS, selection, selectionArgs, null);
        try {
            if (cursor.moveToFirst()) {
                GeoFenceHelper helper = new GeoFenceHelper().initWrapper(cursor);
                CircleGeofence geofence = helper.getEntity(cursor);
                loadEntity(geofence);
            }
        } finally {
            cursor.close();
        }
    }


    private void prepareInsert() {
        this.entityUri = null;
    }


    public void loadEntity(CircleGeofence geofence) {
        // defince
        this.fence = geofence;
        bindingView(geofence);

        // Notify listener
        if (onGeofenceSelectListener != null) {
            onGeofenceSelectListener.onGeofenceSelect(entityUri, fence);
        }

    }



    // ===========================================================
    // Action
    // ===========================================================

    public void onCancelClick() {
        getActivity().setResult(Activity.RESULT_CANCELED);
        getActivity().finish();
    }

    public void onDeleteClick() {
        int deleteCount = getActivity().getContentResolver().delete(entityUri, null, null);
        Log.d(TAG, "Delete %s entity successuf");
        if (deleteCount > 0) {
            getActivity().setResult(Activity.RESULT_OK);
        } else {
            getActivity().setResult(Activity.RESULT_CANCELED);
        }
        getActivity().finish();
    }


    public void onSaveClick() {
        doSaveGeofence();

        // Do Save
        Uri uri = doSaveGeofence();
        if (uri!=null) {
            getActivity().setResult(Activity.RESULT_OK);
            getActivity().finish();
        }
    }

    private Uri doSaveGeofence() {

        // Validate
        if (!formValidator.validate()) {
            return null;
        }
        String name = BindingHelper.getEditTextAsValueTrimToNull(nameEditText);
         // Prepare db insert
        ContentValues values = new ContentValues();
        values.put(GeoFenceDatabase.GeoFenceColumns.COL_NAME, name);

        // Content
        Uri uri;
        ContentResolver cr = getActivity().getContentResolver();
        if (entityUri == null) {
            uri = cr.insert(GeoFenceProvider.Constants.CONTENT_URI, values);
            this.entityUri = uri;
            String entityId = entityUri ==null ? null : entityUri.getLastPathSegment();
            getActivity().setResult(Activity.RESULT_OK);
        } else {
            uri = entityUri;
            int count =cr.update(uri, values, null, null);
            if (count != 1) {
                Log.e(TAG, String.format("Error, %s entities was updates for Expected One", count));
            }
        }
         if (onGeofenceSelectListener != null && entityUri!=null) {
            onGeofenceSelectListener.onGeofenceSelect(uri, fence);
        }
        return entityUri;
    }

    // ===========================================================
    // Other
    // ===========================================================


}