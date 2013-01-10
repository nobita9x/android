package eu.ttbox.geoping.service.slave;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import eu.ttbox.geoping.core.AppConstants;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.domain.GeoTrackerProvider;
import eu.ttbox.geoping.domain.geotrack.GeoTrackDatabase.GeoTrackColumns;
import eu.ttbox.geoping.domain.geotrack.GeoTrackHelper;
import eu.ttbox.geoping.domain.model.GeoTrack;
import eu.ttbox.geoping.service.SmsSenderHelper;
import eu.ttbox.geoping.service.core.WorkerService;
import eu.ttbox.geoping.service.encoder.SmsMessageActionEnum;
import eu.ttbox.geoping.service.encoder.SmsMessageLocEnum;
import eu.ttbox.osm.ui.map.mylocation.sensor.MyLocationListenerProxy;

public class GeoPingSlaveLocationService extends WorkerService implements SharedPreferences.OnSharedPreferenceChangeListener {

	private static final String TAG = "GeoPingSlaveLocationService";

	private static final String LOCK_NAME = "GeoPingSlaveLocationService";

	public static final String ACTION_FIND_LOCALISATION_AND_SEND_SMS_GEOPING = "ACTION_FIND_LOCALISATION_AND_SEND_SMS_GEOPING";

	private final IBinder binder = new LocalBinder();

	// Services
	private TelephonyManager telephonyManager;
	private LocationManager locationManager;
	private MyLocationListenerProxy myLocation;
	private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
	private SharedPreferences appPreferences;

	// Config
	private boolean saveInLocalDb = false;

	// Instance Data
	private List<GeoPingRequest> geoPingRequestList;
	private MultiGeoRequestLocationListener multiGeoRequestListener;

	private int batterLevelInPercent = -1;

	// ===========================================================
	// Lock
	// ===========================================================

	private static volatile PowerManager.WakeLock lockStatic = null;

	// public static void runIntentInService(Context context, Intent intent) {
	// PowerManager.WakeLock lock = getLock(context);
	// lock.acquire();
	// intent.setClassName(context, GeoPingSlaveService.class.getName());
	// context.startService(intent);
	// }
	private synchronized static PowerManager.WakeLock getLock(Context context) {
		if (lockStatic == null) {
			PowerManager mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOCK_NAME);
			lockStatic.setReferenceCounted(true);
		}
		return (lockStatic);
	}

	// ===========================================================
	// Constructors
	// ===========================================================

	public GeoPingSlaveLocationService() {
		super(TAG);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "#####################################");
		Log.d(TAG, "### GeoPing Location Service Started.");
		Log.d(TAG, "#####################################");
		// service
		this.appPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		this.telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		this.myLocation = new MyLocationListenerProxy(locationManager);
		this.geoPingRequestList = new ArrayList<GeoPingRequest>();
		this.multiGeoRequestListener = new MultiGeoRequestLocationListener(geoPingRequestList);

		loadPrefConfig();
		// register listener
		appPreferences.registerOnSharedPreferenceChangeListener(this);
	}

	private void loadPrefConfig() {
		this.saveInLocalDb = appPreferences.getBoolean(AppConstants.PREFS_LOCAL_SAVE, false);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(AppConstants.PREFS_LOCAL_SAVE)) {
			this.saveInLocalDb = appPreferences.getBoolean(AppConstants.PREFS_LOCAL_SAVE, false);
		}
	}

	@Override
	public void onDestroy() {
		appPreferences.unregisterOnSharedPreferenceChangeListener(this);
		this.myLocation.stopListening();
		geoPingRequestList.clear();
		super.onDestroy();
		Log.d(TAG, "#######################################");
		Log.d(TAG, "### GeoPing Location Service Destroyed.");
		Log.d(TAG, "#######################################");
	}

	// ===========================================================
	// Intent Handler
	// ===========================================================

	public static void runFindLocationAndSendInService(Context context, String phone, Bundle params) {
		// PowerManager.WakeLock lock = getLock(context);
		// lock.acquire();
		Intent intent = new Intent(context, GeoPingSlaveLocationService.class);
		intent.putExtra(Intents.EXTRA_SMS_PHONE, phone);
		intent.putExtra(Intents.EXTRA_SMS_PARAMS, params);
		intent.setAction(ACTION_FIND_LOCALISATION_AND_SEND_SMS_GEOPING);
		context.startService(intent);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String action = intent.getAction();
		if (ACTION_FIND_LOCALISATION_AND_SEND_SMS_GEOPING.equals(action)) {
			// GeoPing Request
			String phone = intent.getStringExtra(Intents.EXTRA_SMS_PHONE);
			Bundle params = intent.getBundleExtra(Intents.EXTRA_SMS_PARAMS);
			registerGeoPingRequest(phone, params);
		}

	}

	// ===========================================================
	// Cell Id
	// ===========================================================

	/**
	 * {link http://www.devx.com/wireless/Article/40524/0/page/2}
	 */
	private int[] getCellId() {
		int[] cellId = new int[0];
		CellLocation cellLoc = telephonyManager.getCellLocation();
		if (cellLoc != null && (cellLoc instanceof GsmCellLocation)) {
			GsmCellLocation gsmLoc = (GsmCellLocation) cellLoc;
			gsmLoc.getPsc();
			// gsm cell id
			int cid = gsmLoc.getCid();
			// gsm location area code
			int lac = gsmLoc.getLac();
			// On a UMTS network, returns the primary scrambling code of the
			// serving cell.
			int psc = gsmLoc.getPsc();
			Log.d(TAG, String.format("Cell Id : %s  / Lac : %s  / Psc : %s", cid, lac, psc));
			if (psc>-1) {
				cellId = new int[3];
				cellId[2] = psc;
			} else {
				cellId = new int[2];
			}
			cellId[0] = cid;
			cellId[1] = lac;
		}
		return cellId;
	}

	private void getNeighboringCellId() {
		List<NeighboringCellInfo> neighCell = null;
		neighCell = telephonyManager.getNeighboringCellInfo();
		for (int i = 0; i < neighCell.size(); i++) {
			NeighboringCellInfo thisCell = neighCell.get(i);
			int cid = thisCell.getCid();
			int rssi = thisCell.getRssi();
			int psc = thisCell.getPsc();
			Log.d(TAG, " " + cid + " - " + rssi + " - " + psc);
		}
	}

	// ===========================================================
	// Geocoding Request
	// ===========================================================

	public boolean registerGeoPingRequest(String phoneNumber, Bundle params) {
		// Acquire Lock
		PowerManager.WakeLock lock = getLock(this.getApplicationContext());
		lock.acquire();
		Log.d(TAG, "*** Lock Acquire: " + LOCK_NAME + " " + lock);
		// Register request
		Location initLastLoc = myLocation.getLastKnownLocation();
		GeoPingRequest request = new GeoPingRequest(phoneNumber, params);
		geoPingRequestList.add(request);
		// TODO Bad for multi request
		boolean locProviderEnabled = myLocation.startListening(multiGeoRequestListener);
		// schedule it for time out
		// TODO
		int timeOutInSeconde = SmsMessageLocEnum.TIME_IN_S.readInt(params, 30);
		executorService.schedule(request, timeOutInSeconde, TimeUnit.SECONDS);

		return locProviderEnabled;
	}

	public void unregisterGeoPingRequest(GeoPingRequest request) {
		boolean isRemove = geoPingRequestList.remove(request);
		if (isRemove) {
			Log.d(TAG, "Remove GeoPing Request in list, do Stop Service");
		} else {
			Log.e(TAG, "Could not remove expected GeoPingRequest. /!\\ Emmergency Stop Service /!\\");
			geoPingRequestList.clear();
		}
		// Release Lock
		PowerManager.WakeLock lock = getLock(this.getApplicationContext());
		if (lock.isHeld()) {
			lock.release();
		}

		Log.d(TAG, "*** Lock Release: " + LOCK_NAME + " " + lock);
		// Stop Service if necessary
		if (geoPingRequestList.isEmpty()) {
			Log.d(TAG, "No GeoPing Request in list, do Stop Service");
			myLocation.stopListening();
			// Stop Service
			stopSelf();
		}
	}

	// ===========================================================
	// Sensor Listener
	// ===========================================================

	/**
	 * Computes the battery level by registering a receiver to the intent
	 * triggered by a battery status/level change. <br/>
	 * {@link http
	 * ://developer.android.com/training/monitoring-device-state/battery
	 * -monitoring.html}
	 */

	private void batteryLevel() {
		BroadcastReceiver batteryLevelReceiver = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				context.unregisterReceiver(this);
				int rawlevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
				int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
				int level = -1;
				if (rawlevel >= 0 && scale > 0) {
					level = (rawlevel * 100) / scale;
				}
				Log.d(TAG, "Battery Level Remaining: " + level + "%");
				batterLevelInPercent = level;
			}
		};

		IntentFilter batteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		registerReceiver(batteryLevelReceiver, batteryLevelFilter);
	}

	public class GeoPingRequest implements Callable<Boolean>, LocationListener {

		private String smsPhoneNumber;
		private Bundle params;

		private int accuracy = -1;
		private boolean isAccuracyCheck = false;

		public GeoPingRequest() {
			super();
		}

		public GeoPingRequest(String phoneNumber, Bundle params) {
			super();
			this.smsPhoneNumber = phoneNumber;
			this.params = params;
			this.accuracy = SmsMessageLocEnum.ACCURACY.readInt(params, -1);
			this.isAccuracyCheck = accuracy > -1;
			// register Listener for Battery Level
			batteryLevel();
		}

		@Override
		public Boolean call() throws Exception {
			Boolean result = Boolean.FALSE;
			try {
				Location lastLocation = myLocation.getLastFix();
				int[] cellId = getCellId();
				// TODO Cell Id
				if (lastLocation != null) {
					sendSmsLocation(smsPhoneNumber, lastLocation);
					result = Boolean.TRUE;
				}
			} finally {
				unregisterGeoPingRequest(GeoPingRequest.this);
			}
			return result;
		}

		@Override
		public void onLocationChanged(Location location) {
			if (isAccuracyCheck && location != null) {
				// TODO check expected accuracy
				int locAcc = (int) location.getAccuracy();
				Log.d(TAG, "onLocationChanged with accuracy= " + locAcc + " : " + location);
			}

		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

	}

	// ===========================================================
	// Sender Sms message
	// ===========================================================

	private void sendSmsLocation(String phone, Location location) {
		GeoTrack geotrack = new GeoTrack(null, location);
		geotrack.batteryLevelInPercent = batterLevelInPercent;
		Bundle params = GeoTrackHelper.getBundleValues(geotrack);
		ContentResolver cr = getContentResolver();
		SmsSenderHelper.sendSms(cr, phone, SmsMessageActionEnum.ACTION_GEO_LOC, params);
		if (saveInLocalDb) {
			geotrack.requesterPersonPhone = phone;
			saveInLocalDb(geotrack);
		}
	}

	private void saveInLocalDb(GeoTrack geotrack) {
		if (geotrack == null) {
			return;
		}
		ContentValues values = GeoTrackHelper.getContentValues(geotrack);
		values.put(GeoTrackColumns.COL_PHONE, AppConstants.KEY_DB_LOCAL);
		getContentResolver().insert(GeoTrackerProvider.Constants.CONTENT_URI, values);
	}

	// ===========================================================
	// Binder
	// ===========================================================

	public class LocalBinder extends Binder {
		public GeoPingSlaveLocationService getService() {
			return GeoPingSlaveLocationService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

}