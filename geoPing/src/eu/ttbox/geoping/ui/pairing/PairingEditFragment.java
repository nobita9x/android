package eu.ttbox.geoping.ui.pairing;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.AppConstants;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.core.NotifToasts;
import eu.ttbox.geoping.core.PhoneNumberUtils;
import eu.ttbox.geoping.domain.PairingProvider;
import eu.ttbox.geoping.domain.model.PairingAuthorizeTypeEnum;
import eu.ttbox.geoping.domain.pairing.PairingDatabase.PairingColumns;
import eu.ttbox.geoping.domain.pairing.PairingHelper;

public class PairingEditFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "PairingEditFragment";
    
    
    // Constant
    private static final int PAIRING_EDIT_LOADER = R.id.config_id_pairing_edit_loader;

    public static final int PICK_CONTACT = 0;

    // Service
    private SharedPreferences sharedPreferences;

    // Config
    private static final boolean DEFAULT_PREFS_SHOW_GEOPING_NOTIFICATION = false;
    private boolean showNotifDefault = DEFAULT_PREFS_SHOW_GEOPING_NOTIFICATION;

    // Paint
    Paint mPaint = new Paint();

    // Bindings
    private EditText nameEditText;
    private EditText phoneEditText;
    private CheckBox showNotificationCheckBox;
    private TextView authorizeTypeTextView;

    private RadioGroup authorizeTypeRadioGroup;

    private RadioButton authorizeTypeAskRadioButton;
    private RadioButton authorizeTypeNeverRadioButton;
    private RadioButton authorizeTypeAlwaysRadioButton;
 
    
    private Button selectContactClickButton;
    // Instance
    // private String entityId;
    private Uri entityUri;

    // ===========================================================
    // Constructors
    // ===========================================================
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "****************** onCreateView");
        View v = inflater.inflate(R.layout.pairing_edit, container, false);
     // Prefs
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        // Config
        showNotifDefault = sharedPreferences.getBoolean(AppConstants.PREFS_SHOW_GEOPING_NOTIFICATION, DEFAULT_PREFS_SHOW_GEOPING_NOTIFICATION);

        // binding
        nameEditText = (EditText) v.findViewById(R.id.pairing_name);
        phoneEditText = (EditText) v.findViewById(R.id.pairing_phone);
        showNotificationCheckBox = (CheckBox) v.findViewById(R.id.paring_show_notification);
        authorizeTypeTextView = (TextView) v.findViewById(R.id.pairing_authorize_type);

        authorizeTypeRadioGroup = (RadioGroup) v.findViewById(R.id.pairing_authorize_type_radioGroup);
        authorizeTypeAskRadioButton = (RadioButton) v.findViewById(R.id.pairing_authorize_type_radio_ask);
        authorizeTypeNeverRadioButton = (RadioButton) v.findViewById(R.id.pairing_authorize_type_radio_never);
        authorizeTypeAlwaysRadioButton = (RadioButton) v.findViewById(R.id.pairing_authorize_type_radio_always);

        selectContactClickButton = (Button) v.findViewById(R.id.select_contact_button);
        // Radio Auth Listener
        OnClickListener radioAuthListener = new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                onRadioAuthorizeTypeButtonClicked(v);
                
            }
        };
        authorizeTypeAskRadioButton.setOnClickListener(radioAuthListener);
        authorizeTypeNeverRadioButton.setOnClickListener(radioAuthListener);
        authorizeTypeAlwaysRadioButton.setOnClickListener(radioAuthListener);
        
        // default value
        showNotificationCheckBox.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
              onShowNotificationClick(v);
                
            }
        });
        showNotificationCheckBox.setChecked(showNotifDefault);
        
        // default value
        selectContactClickButton.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                onSelectContactClick(v);
                
            }
        });
        // Load Data
//        loadEntity(getArguments());
        
        return v;
    }

    @Override
    public void onDestroy() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }


    // ===========================================================
    // Life Cycle
    // ===========================================================

    

    // ===========================================================
    // Preferences
    // ===========================================================

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(AppConstants.PREFS_SHOW_GEOPING_NOTIFICATION)) {
            showNotifDefault = sharedPreferences.getBoolean(AppConstants.PREFS_SHOW_GEOPING_NOTIFICATION, DEFAULT_PREFS_SHOW_GEOPING_NOTIFICATION);
        }
    }



    // ===========================================================
    // Accessor
    // ===========================================================

    private void loadEntity(Bundle agrs) {
        Log.d(TAG, "****************** loadEntity Bundle " + agrs);
        if (agrs!=null && agrs.containsKey(Intents.EXTRA_PERSON_ID)) {
            String entityId =  agrs.getString(Intents.EXTRA_PERSON_ID);
            loadEntity(entityId);
            Log.d(TAG, "****************** loadEntity with ID " + entityId);
        } else {
            Log.d(TAG, "****************** prepareInsert " + agrs);
            // prepare for insert
            prepareInsert();
        } 
    }
    
    public void prepareInsert() {
        this.entityUri = null;
        showNotificationCheckBox.setChecked(showNotifDefault);
        // Open Selection contact Diallog
        onSelectContactClick(null);
        //Defautl value
        authorizeTypeAskRadioButton.setChecked(true);
    }

    
    public void loadEntity(String entityId) { // String entityId
        this.entityUri = Uri.withAppendedPath(PairingProvider.Constants.CONTENT_URI, entityId); 
        Bundle bundle = new Bundle();
        bundle.putString(Intents.EXTRA_SMS_PHONE, entityId);
        getActivity().getSupportLoaderManager().initLoader(PAIRING_EDIT_LOADER, bundle, pairingLoaderCallback);
    }
    
    

    public void onDeleteClick() {
        int deleteCount = getActivity().getContentResolver().delete(entityUri, null, null);
        Log.d(TAG, "Delete %s entity successuf");
        if (deleteCount > 0) {
            getActivity().setResult(Activity.RESULT_OK);
        }
        getActivity().finish();
    }

    public void onSaveClick() {
        String name = nameEditText.getText().toString();
        String phone = phoneEditText.getText().toString();
        // TODO Select authorizeType
        PairingAuthorizeTypeEnum authType = null;
        if (authorizeTypeAlwaysRadioButton.isChecked()) {
            authType = PairingAuthorizeTypeEnum.AUTHORIZE_ALWAYS;
        } else if (authorizeTypeNeverRadioButton.isChecked()) {
            authType = PairingAuthorizeTypeEnum.AUTHORIZE_NEVER;
        } else {
            authType = PairingAuthorizeTypeEnum.AUTHORIZE_REQUEST;
        }

        // Do Save
        Uri uri =  doSavePairing(name, phone, authType);
        getActivity().setResult(Activity.RESULT_OK);
        getActivity().finish();
    }

    public void onCancelClick() {
        getActivity().setResult(Activity.RESULT_CANCELED);
        getActivity().finish();
    }

    /**
     * {link http://www.higherpass.com/Android/Tutorials/Working-With-Android-
     * Contacts/}
     * 
     * @param v
     */
    public void onSelectContactClick(View v) {
        // String phoneNumber = phoneEditText.getText().toString();
        // Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
        // Uri.encode(phoneNumber));
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        // run
        startActivityForResult(intent, PICK_CONTACT);
    }

    public void onPairingClick(View v) {
        String entityId = entityUri.getLastPathSegment();
        Intent intent = Intents.pairingRequest(getActivity(), phoneEditText.getText().toString(), entityId);
        getActivity().startService(intent);
    }

    public void onShowNotificationClick(View v) {
        if (entityUri != null) {
            boolean isCheck = showNotificationCheckBox.isChecked();
            ContentValues values = new ContentValues();
            values.put(PairingColumns.COL_SHOW_NOTIF, isCheck);
            int count = getActivity().getContentResolver().update(entityUri, values, null, null);
        }
    }


    // ===========================================================
    // Listener
    // ===========================================================


    // ===========================================================
    // Contact Picker
    // ===========================================================

    public void saveContactData(Uri contactData) {
        String selection = null;
        String[] selectionArgs = null;
        Cursor c = getActivity().getContentResolver().query(contactData, new String[] { //
                ContactsContract.CommonDataKinds.Identity.DISPLAY_NAME, // TODO
                                                                        // Check
                                                                        // for
                                                                        // V10
                                                                        // compatibility
                        ContactsContract.CommonDataKinds.Phone.NUMBER, //
                        ContactsContract.CommonDataKinds.Phone.TYPE }, selection, selectionArgs, null);
        try {
            // Read value
            if (c != null && c.moveToFirst()) {
                String name = c.getString(0);
                String phone = c.getString(1);
                int type = c.getInt(2);
                Uri uri = doSavePairing(name, phone, null);
                // showSelectedNumber(type, number);
            }
        } finally {
            c.close();
        }
    }

    public void onRadioAuthorizeTypeButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();
        PairingAuthorizeTypeEnum authType = null;
        // Check which radio button was clicked
        switch (view.getId()) {
        case R.id.pairing_authorize_type_radio_ask:
            if (checked)
                authType = PairingAuthorizeTypeEnum.AUTHORIZE_REQUEST;
            showNotificationCheckBox.setVisibility(View.GONE);
            break;
        case R.id.pairing_authorize_type_radio_always:
            if (checked)
                authType = PairingAuthorizeTypeEnum.AUTHORIZE_ALWAYS;
            showNotificationCheckBox.setVisibility(View.VISIBLE);
            break;
        case R.id.pairing_authorize_type_radio_never:
            if (checked)
                authType = PairingAuthorizeTypeEnum.AUTHORIZE_NEVER;
            showNotificationCheckBox.setVisibility(View.VISIBLE);
            break;
        }
        if (authType != null && entityUri != null) {
            ContentValues values = authType.writeTo(null);
            getActivity().getContentResolver().update(entityUri, values, null, null);
        }
    }

    // ===========================================================
    // Data Model Management
    // ===========================================================

    private String cleanPhone(String phone) {
        String cleanPhone = phone;
        if (cleanPhone != null) {
            cleanPhone = PhoneNumberUtils.normalizeNumber(phone);
        }
        if (cleanPhone != null) {
            cleanPhone = cleanPhone.trim();
            if (cleanPhone.length() < 1) {
                cleanPhone = null;
            }
        }
        return cleanPhone;
    }

    private String trimToNull(String nameDirty) {
        String name = nameDirty;
        if (name != null) {
            name = name.trim();
            if (name.length() < 1) {
                name = null;
            }
        }
        return name;
    }

    private Uri doSavePairing(String nameDirty, String phoneDirty, PairingAuthorizeTypeEnum authorizeType) {
        String phone = cleanPhone(phoneDirty);
        String name = trimToNull(nameDirty);
        setPairing(name, phone);
        if (TextUtils.isEmpty(phone)) {
            NotifToasts.validateMissingPhone(getActivity());
            return null;
        }
        // Prepare db insert
        ContentValues values = new ContentValues();
        values.put(PairingColumns.COL_NAME, name);
        values.put(PairingColumns.COL_PHONE, phone);
        if (authorizeType != null) {
            authorizeType.writeTo(values);
        }
        // Content
        Uri uri;
        if (entityUri == null) {
            uri = getActivity().getContentResolver().insert(PairingProvider.Constants.CONTENT_URI, values);
            this.entityUri = uri;
            getActivity().setResult(Activity.RESULT_OK);
        } else {
            uri = entityUri;
            int count = getActivity().getContentResolver().update(uri, values, null, null);
            if (count != 1) {
                Log.e(TAG, String.format("Error, %s entities was updates for Expected One", count));
            }
        }
        return uri;
    }

    private void setPairing(String name, String phone) {
        nameEditText.setText(name);
        phoneEditText.setText(phone);
    }

    // ===========================================================
    // Activity Result handler
    // ===========================================================

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        switch (reqCode) {
        case (PairingEditFragment.PICK_CONTACT):
            if (resultCode == Activity.RESULT_OK) {
                Uri contactData = data.getData();
                saveContactData(contactData);
//                finish();
            }
        }
    }
    // ===========================================================
    // LoaderManager
    // ===========================================================

    private final LoaderManager.LoaderCallbacks<Cursor> pairingLoaderCallback = new LoaderManager.LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Log.d(TAG, "onCreateLoader");
            String entityId = args.getCharSequence(Intents.EXTRA_SMS_PHONE).toString();
            Uri entityUri =   Uri.withAppendedPath(PairingProvider.Constants.CONTENT_URI, String.valueOf(entityId));
            // Loader
            CursorLoader cursorLoader = new CursorLoader(getActivity(), entityUri, null, null, null, null);
            return cursorLoader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            Log.d(TAG, "onLoadFinished with cursor result count : " + cursor.getCount());
            // Display List
            if (cursor.moveToFirst()) {
                // Data
                PairingHelper helper = new PairingHelper().initWrapper(cursor);
                helper.setTextPairingName(nameEditText, cursor)//
                        .setTextPairingPhone(phoneEditText, cursor)//
                        .setCheckBoxPairingShowNotif(showNotificationCheckBox, cursor);
                // Pairing
                PairingAuthorizeTypeEnum authType = helper.getPairingAuthorizeTypeEnum(cursor);
                switch (authType) {
                case AUTHORIZE_REQUEST:
                    authorizeTypeAskRadioButton.setChecked(true);
                    break;
                case AUTHORIZE_NEVER:
                    authorizeTypeNeverRadioButton.setChecked(true);
                    break;
                case AUTHORIZE_ALWAYS:
                    authorizeTypeAlwaysRadioButton.setChecked(true);
                    break;

                default:
                    break;
                }
                // Notif
                if (PairingAuthorizeTypeEnum.AUTHORIZE_REQUEST.equals(authType)) {
                    showNotificationCheckBox.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            setPairing(null, null);
        }

    };

}