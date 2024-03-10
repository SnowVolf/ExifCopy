package net.eufy.exifcopy;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.io.InputStream;

public class MainFragment extends Fragment {

    private static final String TAG = "ExifCopy";

    private TextView targetUriText;
    private TextView sourceUriText;

    private Button selectTargetButton;
    private Button selectSourceButton;
    private Button executeButton;
    private CheckBox excludeOrientationCheckbox;

    private ActivityResultLauncher<Intent> startGalleryForTargetIntent;
    private ActivityResultLauncher<Intent> startGalleryForSourceIntent;

    private Uri targetUri = null;
    private Uri sourceUri = null;

    private boolean excludeOrientation = false;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        startGalleryForTargetIntent = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK)
                        {
                            targetUri = result.getData().getData();
                            targetUriText.setText(targetUri.getPath());
                        }
                    }
                });

        startGalleryForSourceIntent = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK)
                        {
                            sourceUri = result.getData().getData();
                            sourceUriText.setText(sourceUri.getPath());
                        }
                    }
                });


        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        selectSourceButton = view.findViewById(R.id.select_source_button);
        selectTargetButton = view.findViewById(R.id.select_target_button);
        executeButton = view.findViewById(R.id.execute_button);
        sourceUriText = view.findViewById(R.id.source_uri_text);
        targetUriText = view.findViewById(R.id.target_uri_text);
        excludeOrientationCheckbox = view.findViewById(R.id.exclude_orientation_checkbox);

        selectTargetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                try {
                    startGalleryForTargetIntent.launch(galleryIntent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        selectSourceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                try {
                    startGalleryForSourceIntent.launch(galleryIntent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        executeButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {
                if (targetUri != null && sourceUri != null)
                {
                    if(CopyExifFromSourceToTarget())
                    {
                        Snackbar.make(view, "Exif tags were successfully copied.", Snackbar.LENGTH_LONG).show();
                    }
                    else
                    {
                        Snackbar.make(view, "Something wrong with CopyExif method. Please check the log messages.", Snackbar.LENGTH_LONG).show();
                    }
                }
                else
                {
                    Snackbar.make(view, "Both source and target image should be selected.", Snackbar.LENGTH_LONG).show();
                }
            }
        });
        excludeOrientation = excludeOrientationCheckbox.isChecked();
        excludeOrientationCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                excludeOrientation = b;
                if(excludeOrientation)
                {
                    Snackbar.make(view, "Orientation tag will not be copied.", Snackbar.LENGTH_SHORT).show();
                }
                else
                {
                    Snackbar.make(view, "Orientation tag will be copied.", Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean CopyExifFromSourceToTarget()
    {
        boolean status = false;
        if (targetUri == null || sourceUri == null) return status;

        // refer to https://stackoverflow.com/questions/65516993/copy-exif-info-from-one-uri-to-the-other-with-the-new-android-scoped-storage
        InputStream ins = null;
        ParcelFileDescriptor outFd = null;
        FragmentActivity context = getActivity();
        try  {
            ins = context.getContentResolver().openInputStream(sourceUri);
            ExifInterface originalExif = new ExifInterface(ins);

            outFd = context.getContentResolver().openFileDescriptor(targetUri, "rw");
            ExifInterface newExif = new ExifInterface(outFd.getFileDescriptor());
            CopyExifAttributes(originalExif, newExif);
            status = true;
        } catch (IOException e) {
            Log.d(TAG, e.getMessage(), e);
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException e) {
                    Log.d(TAG, e.getMessage(), e);
                }
            }
            if (outFd != null) {
                try {
                    outFd.close();
                } catch (IOException e) {
                    Log.d(TAG, e.getMessage(), e);
                }
            }
        }
        return status;
    }

    private void CopyExifAttributes(ExifInterface source, ExifInterface target) throws IOException {
        CopyExifAttribute(source, target, ExifInterface.TAG_APERTURE_VALUE);
        CopyExifAttribute(source, target, ExifInterface.TAG_ARTIST);
        CopyExifAttribute(source, target, ExifInterface.TAG_COPYRIGHT);
        CopyExifAttribute(source, target, ExifInterface.TAG_DATETIME);
        CopyExifAttribute(source, target, ExifInterface.TAG_DATETIME_DIGITIZED);
        CopyExifAttribute(source, target, ExifInterface.TAG_DATETIME_ORIGINAL);
        CopyExifAttribute(source, target, ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION);
        CopyExifAttribute(source, target, ExifInterface.TAG_DIGITAL_ZOOM_RATIO);
        CopyExifAttribute(source, target, ExifInterface.TAG_DNG_VERSION);
        CopyExifAttribute(source, target, ExifInterface.TAG_EXIF_VERSION);
        CopyExifAttribute(source, target, ExifInterface.TAG_EXPOSURE_BIAS_VALUE);
        CopyExifAttribute(source, target, ExifInterface.TAG_EXPOSURE_INDEX);
        CopyExifAttribute(source, target, ExifInterface.TAG_EXPOSURE_MODE);
        CopyExifAttribute(source, target, ExifInterface.TAG_EXPOSURE_PROGRAM);
        CopyExifAttribute(source, target, ExifInterface.TAG_EXPOSURE_TIME);
        CopyExifAttribute(source, target, ExifInterface.TAG_FLASH);
        CopyExifAttribute(source, target, ExifInterface.TAG_FLASH_ENERGY);
        CopyExifAttribute(source, target, ExifInterface.TAG_FOCAL_LENGTH);
        CopyExifAttribute(source, target, ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM);
        CopyExifAttribute(source, target, ExifInterface.TAG_FOCAL_PLANE_RESOLUTION_UNIT);
        CopyExifAttribute(source, target, ExifInterface.TAG_FOCAL_PLANE_X_RESOLUTION);
        CopyExifAttribute(source, target, ExifInterface.TAG_FOCAL_PLANE_Y_RESOLUTION);
        CopyExifAttribute(source, target, ExifInterface.TAG_F_NUMBER);
        CopyExifAttribute(source, target, ExifInterface.TAG_GAIN_CONTROL);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_ALTITUDE);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_ALTITUDE_REF);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_AREA_INFORMATION);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_DATESTAMP);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_DEST_BEARING);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_DEST_BEARING_REF);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_DEST_DISTANCE);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_DEST_DISTANCE_REF);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_DEST_LATITUDE);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_DEST_LATITUDE_REF);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_DEST_LONGITUDE);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_DEST_LONGITUDE_REF);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_DIFFERENTIAL);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_DOP);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_IMG_DIRECTION);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_IMG_DIRECTION_REF);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_LATITUDE);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_LATITUDE_REF);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_LONGITUDE);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_LONGITUDE_REF);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_MAP_DATUM);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_MEASURE_MODE);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_PROCESSING_METHOD);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_SATELLITES);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_SPEED);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_SPEED_REF);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_STATUS);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_TIMESTAMP);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_TRACK);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_TRACK_REF);
        CopyExifAttribute(source, target, ExifInterface.TAG_GPS_VERSION_ID);
        CopyExifAttribute(source, target, ExifInterface.TAG_IMAGE_DESCRIPTION);
        CopyExifAttribute(source, target, ExifInterface.TAG_IMAGE_UNIQUE_ID);
        CopyExifAttribute(source, target, ExifInterface.TAG_ISO_SPEED_RATINGS);
        CopyExifAttribute(source, target, ExifInterface.TAG_LIGHT_SOURCE);
        CopyExifAttribute(source, target, ExifInterface.TAG_MAKE);
        CopyExifAttribute(source, target, ExifInterface.TAG_MAKER_NOTE);
        CopyExifAttribute(source, target, ExifInterface.TAG_MAX_APERTURE_VALUE);
        CopyExifAttribute(source, target, ExifInterface.TAG_METERING_MODE);
        CopyExifAttribute(source, target, ExifInterface.TAG_MODEL);
        CopyExifAttribute(source, target, ExifInterface.TAG_OFFSET_TIME);
        CopyExifAttribute(source, target, ExifInterface.TAG_OFFSET_TIME_DIGITIZED);
        CopyExifAttribute(source, target, ExifInterface.TAG_OFFSET_TIME_ORIGINAL);
        if (!excludeOrientation) CopyExifAttribute(source, target, ExifInterface.TAG_ORIENTATION);
        CopyExifAttribute(source, target, ExifInterface.TAG_SCENE_CAPTURE_TYPE);
        CopyExifAttribute(source, target, ExifInterface.TAG_SCENE_TYPE);
        CopyExifAttribute(source, target, ExifInterface.TAG_SENSING_METHOD);
        CopyExifAttribute(source, target, ExifInterface.TAG_SHUTTER_SPEED_VALUE);
        CopyExifAttribute(source, target, ExifInterface.TAG_SUBJECT_AREA);
        CopyExifAttribute(source, target, ExifInterface.TAG_SUBJECT_DISTANCE);
        CopyExifAttribute(source, target, ExifInterface.TAG_SUBJECT_DISTANCE_RANGE);
        CopyExifAttribute(source, target, ExifInterface.TAG_SUBJECT_LOCATION);
        CopyExifAttribute(source, target, ExifInterface.TAG_SUBSEC_TIME);
        CopyExifAttribute(source, target, ExifInterface.TAG_SUBSEC_TIME_DIGITIZED);
        CopyExifAttribute(source, target, ExifInterface.TAG_SUBSEC_TIME_ORIGINAL);
        CopyExifAttribute(source, target, ExifInterface.TAG_USER_COMMENT);
        CopyExifAttribute(source, target, ExifInterface.TAG_WHITE_BALANCE);
        CopyExifAttribute(source, target, ExifInterface.TAG_WHITE_POINT);
        CopyExifAttribute(source, target, ExifInterface.TAG_Y_CB_CR_COEFFICIENTS);
        CopyExifAttribute(source, target, ExifInterface.TAG_Y_CB_CR_POSITIONING);
        CopyExifAttribute(source, target, ExifInterface.TAG_Y_CB_CR_SUB_SAMPLING);

        target.saveAttributes();
    }

    private void CopyExifAttribute(ExifInterface source, ExifInterface target, String tag)
    {
        String value = source.getAttribute(tag);
        if (value != null)
        {
           target.setAttribute(tag, value);
           Log.d(TAG, tag + ": " + value);
        }
    }
}