package eu.ttbox.velib.map.osm;

import java.io.InputStream;

import org.osmdroid.ResourceProxy.string;
import org.osmdroid.tileprovider.ExpirableBitmapDrawable;
import org.osmdroid.tileprovider.tilesource.CloudmadeTileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParser;

public class CloudmadeTileSourceVector extends CloudmadeTileSource {

    private static final String TAG = "CloudmadeTileSourceVector";
    private static final Logger logger = LoggerFactory.getLogger(CloudmadeTileSourceVector.class);

    public CloudmadeTileSourceVector(String pName, string pResourceId, int pZoomMinLevel, int pZoomMaxLevel, int pTileSizePixels, String pImageFilenameEnding, String... pBaseUrl) {
        super(pName, pResourceId, pZoomMinLevel, pZoomMaxLevel, pTileSizePixels, pImageFilenameEnding, pBaseUrl);
    }

    @Override
    public Drawable getDrawable(final InputStream aFileInputStream) {
        // try {
        // default implementation will load the file as a bitmap and create
        // a BitmapDrawable from it
        Log.d(TAG, "Get Drawable Vector");
        SVG svg = SVGParser.getSVGFromInputStream(aFileInputStream);
        return svg.createPictureDrawable();
        // final Bitmap bitmap = BitmapFactory.decodeStream(aFileInputStream);
        // if (bitmap != null) {
        // return new ExpirableBitmapDrawable(bitmap);
        // }
        // } catch (final OutOfMemoryError e) {
        // logger.error("OutOfMemoryError loading bitmap");
        // System.gc();
        // throw new LowMemoryException(e);
        // }
        // return null;
    }

}
