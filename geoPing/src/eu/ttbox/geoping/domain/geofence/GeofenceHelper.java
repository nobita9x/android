package eu.ttbox.geoping.domain.geofence;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.location.Address;
import android.os.Bundle;
import android.widget.TextView;
import eu.ttbox.geoping.core.AppConstants;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.domain.core.wrapper.BundleWrapper;
import eu.ttbox.geoping.domain.core.wrapper.ContentValuesWrapper;
import eu.ttbox.geoping.domain.core.wrapper.HelperWrapper;
import eu.ttbox.geoping.domain.geofence.GeoFenceDatabase.CircleGeofenceColumns;
import eu.ttbox.geoping.domain.model.CircleGeofence;

public class GeofenceHelper {

	boolean isNotInit = true;

	public int idIdx = -1;
	public int geofenceIdIdx = -1;
	 
	
	public int latitudeE6Idx = -1;
	public int longitudeE6Idx = -1;
	public int radiusIdx = -1;
	
    public int expirationIdx = -1; 
    public int transitionIdx = -1;

	 
	// public int titreIdx = -1;

	public GeofenceHelper initWrapper(Cursor cursor) {
		idIdx = cursor.getColumnIndex(CircleGeofenceColumns.COL_ID); 
		geofenceIdIdx = cursor.getColumnIndex(CircleGeofenceColumns.COL_REQUEST_ID);

		latitudeE6Idx = cursor.getColumnIndex(CircleGeofenceColumns.COL_LATITUDE_E6);
		longitudeE6Idx = cursor.getColumnIndex(CircleGeofenceColumns.COL_LONGITUDE_E6);
		radiusIdx = cursor.getColumnIndex(CircleGeofenceColumns.COL_RADIUS); 
 
		transitionIdx = cursor.getColumnIndex(CircleGeofenceColumns.COL_TRANSITION);
        expirationIdx = cursor.getColumnIndex(CircleGeofenceColumns.COL_EXPIRATION); 

		 

		isNotInit = false;
		return this;
	}

	public CircleGeofence getEntity(Cursor cursor) {
		if (isNotInit) {
			initWrapper(cursor);
		}
		String geofenceId = geofenceIdIdx > -1 ? cursor.getString(geofenceIdIdx) : null;
        int latitudeE6 = cursor.getInt(latitudeE6Idx);
        int longitudeE6 = cursor.getInt(longitudeE6Idx);
        float radius =radiusIdx > -1 ? cursor.getInt(radiusIdx) : -1;
        long expiration = expirationIdx > -1 ? cursor.getLong(expirationIdx) : -1;
        int transition =transitionIdx > -1 ? cursor.getInt(transitionIdx) : -1 ;
		CircleGeofence geoTrack = new CircleGeofence(  geofenceId,
	              latitudeE6,
	              longitudeE6,
	              radius,
	              expiration,
	              transition);
		geoTrack.setId(idIdx > -1 ? cursor.getLong(idIdx) : AppConstants.UNSET_ID);
		 
		return geoTrack;
	}

	private GeofenceHelper setTextWithIdx(TextView view, Cursor cursor, int idx) {
		view.setText(cursor.getString(idx));
		return this;
	}

	public GeofenceHelper setTextId(TextView view, Cursor cursor) {
		return setTextWithIdx(view, cursor, idIdx);
	}

	public String getIdAsString(Cursor cursor) {
		return cursor.getString(idIdx);
	}

	public long getId(Cursor cursor) {
		return cursor.getLong(idIdx);
	}
 

	public static ContentValues getContentValues(CircleGeofence user) {
		ContentValuesWrapper wrapper = (ContentValuesWrapper) getWrapperValues(user, new ContentValuesWrapper(CircleGeofenceColumns.ALL_COLS.length), true);
		ContentValues initialValues = wrapper.getWrappedValue();
		return initialValues;
	}

	public static Bundle getBundleValues(CircleGeofence geoTrack) {
		BundleWrapper wrapper = (BundleWrapper) getWrapperValues(geoTrack, new BundleWrapper(CircleGeofenceColumns.ALL_COLS.length), false);
		Bundle bundle = wrapper.getWrappedValue();
		return bundle;
	}

	private static HelperWrapper<?> getWrapperValues(CircleGeofence geoTrack, HelperWrapper<?> initialValues, boolean noHasCheck) {
		if (geoTrack.id > -1) {
			initialValues.putLong(CircleGeofenceColumns.COL_ID, Long.valueOf(geoTrack.id));
		} 
		 
			initialValues.putString(CircleGeofenceColumns.COL_REQUEST_ID, geoTrack.getRequestId());
		 
		// Location 
		initialValues.putLong(CircleGeofenceColumns.COL_EXPIRATION, geoTrack.getExpirationDuration());
		initialValues.putInt(CircleGeofenceColumns.COL_LATITUDE_E6, geoTrack.getLatitudeE6());
		initialValues.putInt(CircleGeofenceColumns.COL_LONGITUDE_E6, geoTrack.getLongitudeE6());
		initialValues.putFloat(CircleGeofenceColumns.COL_RADIUS, geoTrack.getRadius());
		initialValues.putInt(CircleGeofenceColumns.COL_TRANSITION, geoTrack.getTransitionType());
		 
		return initialValues;
	}

	public static CircleGeofence getEntityFromIntent(Intent intent) {
		Bundle initialValues = intent.getBundleExtra(Intents.EXTRA_SMS_PARAMS);
		CircleGeofence geoTrack = getEntityFromBundle(initialValues);
		 
		return geoTrack;
	}

	public static CircleGeofence getEntityFromBundle(Bundle initialValues) {
		if (initialValues == null || initialValues.isEmpty()) {
			return null;
		}
		   CircleGeofence geofence = new CircleGeofence(  );
		   
		if (initialValues.containsKey(CircleGeofenceColumns.COL_REQUEST_ID)) {
		    geofence.setRequestId( initialValues.getString(CircleGeofenceColumns.COL_REQUEST_ID));
		} 
		if (initialValues.containsKey(CircleGeofenceColumns.COL_EXPIRATION)) {
		    geofence.setExpirationDuration(   initialValues.getLong(CircleGeofenceColumns.COL_EXPIRATION));
		}
		// Geo
		if (initialValues.containsKey(Intents.EXTRA_GEO_E6)) {
			int[] geoLatLng = initialValues.getIntArray(Intents.EXTRA_GEO_E6);
			int geoLatLngSize = geoLatLng.length;
			if (geoLatLngSize >= 2) {
			    geofence.setLatitudeE6(   geoLatLng[0]);
			    geofence.setLongitudeE6( geoLatLng[1]);
			}
			if (geoLatLngSize >= 3) {
			    geofence.setRadius(  geoLatLng[3]);
			}
		}
		if (initialValues.containsKey(CircleGeofenceColumns.COL_LATITUDE_E6)) {
		    geofence.setLatitudeE6(  initialValues.getInt(CircleGeofenceColumns.COL_LATITUDE_E6));
		}
		if (initialValues.containsKey(CircleGeofenceColumns.COL_LONGITUDE_E6)) {
		    geofence.setLongitudeE6(initialValues.getInt(CircleGeofenceColumns.COL_LONGITUDE_E6));
		}

		if (initialValues.containsKey(CircleGeofenceColumns.COL_RADIUS)) {
		    geofence.setRadius(  initialValues.getFloat(CircleGeofenceColumns.COL_RADIUS));
		} 
		if (initialValues.containsKey(CircleGeofenceColumns.COL_TRANSITION)) {
		    geofence.setTransitionType( initialValues.getInt(CircleGeofenceColumns.COL_TRANSITION));
		} 
		
		if (initialValues.containsKey(CircleGeofenceColumns.COL_ID)) {
		    geofence.setId(initialValues.getLong(CircleGeofenceColumns.COL_ID));
        }
		return geofence;
	}

	 
}
