package com.google.camera;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.VideoCapture;
import androidx.camera.core.VideoCapture.OnVideoSavedCallback;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.view.CameraView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.adapters.LinearLayoutBindingAdapter;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.camera.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static android.os.SystemClock.sleep;
import static com.google.camera.R.drawable.ic_baseline_photo_camera_24;
import static com.google.camera.R.drawable.ic_baseline_videocam_24;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.RECORD_AUDIO"};
    private CameraView mCameraView;
    private ActivityMainBinding binding;
    ImageButton cam_icon;
    static final int REQUEST_VIDEO_CAPTURE = 1;
    Boolean IsVideo=false,IsPhoto=true;
    private final Executor executor = Executors.newSingleThreadExecutor();
    CameraSelector cameraSelector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        if(getSupportActionBar() != null)
        {
            getSupportActionBar().setElevation(0);
        }


        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS,
                    REQUEST_CODE_PERMISSIONS);
        }

    }

    private boolean allPermissionsGranted() {
        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(requestCode == REQUEST_CODE_PERMISSIONS){
            if(allPermissionsGranted()){
                startCamera();
            } else{
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
    }

    private void startCamera() {
        cam_icon =findViewById(R.id.cam_icon);
        mCameraView = findViewById(R.id.view_finder);
        mCameraView.setFlash(ImageCapture.FLASH_MODE_AUTO);
        ImageCapture.Builder builder = new ImageCapture.Builder();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mCameraView.bindToLifecycle((LifecycleOwner) MainActivity.this);
        findViewById(R.id.captbutton).setOnClickListener(new View.OnClickListener() {
            @SuppressLint({"UnsafeExperimentalUsageError", "NewApi", "RestrictedApi"})
            @Override
            public void onClick(View v) {
                if(IsPhoto) {
                    mCameraView.setCaptureMode(CameraView.CaptureMode.IMAGE);
                    SimpleDateFormat mDateFormat = null;
                        mDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);

                    File file1 = null;

                        file1 = new File(getBatchDirectoryName(), mDateFormat.format(new Date()) + ".jpg");

                    ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file1).build();
                    File finalFile = file1;
                    mCameraView.takePicture(outputFileOptions, executor, new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    galleryAddPic(finalFile, 0);
                                }
                            });
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException error) {
                            error.printStackTrace();
                        }
                    }); //image saved callback end
                }else {

                    if(mCameraView.isRecording()){
                        mCameraView.stopRecording();
                        Toast.makeText(MainActivity.this, "is finished", Toast.LENGTH_SHORT).show();
                    }
                    SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
                    File file = new File(getBatchDirectoryName(), mDateFormat.format(new Date()) + ".mp4");
                    VideoCapture.OutputFileOptions outputFileOptions = new VideoCapture.OutputFileOptions.Builder(file).build();
                    mCameraView.setCaptureMode(CameraView.CaptureMode.VIDEO);
                    mCameraView.startRecording(outputFileOptions, executor, new VideoCapture.OnVideoSavedCallback() {
                        @Override
                        public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                            galleryAddPic(file, 1);
                        }

                        @Override
                        public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                            Toast.makeText(MainActivity.this, "there is an error", Toast.LENGTH_SHORT).show();
                            if(cause!=null){
                                cause.printStackTrace();
                                Toast.makeText(MainActivity.this,"Video capture failed",Toast.LENGTH_LONG).show();
                            }
                        }

                    }); //image saved callback end
               }
            }//onclick end

        });



        findViewById(R.id.rotate).setOnClickListener(new View.OnClickListener() {
            @SuppressLint("UnsafeExperimentalUsageError")
            @Override
            public void onClick(View v) {
                if (mCameraView.isRecording()) {
                    return;
                }

                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                if (mCameraView.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT)) {
                    mCameraView.toggleCamera();
                } else {
                    return;
                }
            }
        });
    }

    @SuppressLint("InlinedApi")
    private void galleryAddPic(File originalFile, int mediaType) {

        if (!originalFile.exists()) {
            return;
        }

        int pathSeparator = String.valueOf(originalFile).lastIndexOf('/');
        int extensionSeparator = String.valueOf(originalFile).lastIndexOf('.');
        String filename = pathSeparator >= 0 ? String.valueOf(originalFile).substring(pathSeparator + 1) : String.valueOf(originalFile);
        String extension = extensionSeparator >= 0 ? String.valueOf(originalFile).substring(extensionSeparator + 1) : "";

        String mimeType = extension.length() > 0 ? MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase(Locale.ENGLISH)) : null;

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.TITLE, filename);
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
        values.put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000);

        if (mimeType != null && mimeType.length() > 0)
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);

        Uri externalContentUri;
        if (mediaType == 0)
            externalContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        else if (mediaType == 1)
            externalContentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        else
            externalContentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        // Android 10 restricts our access to the raw filesystem, use MediaStore to save media in that case
        if (android.os.Build.VERSION.SDK_INT >= 29) {
           // Toast.makeText(this, "here we go2", Toast.LENGTH_SHORT).show();
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/Camera");
            values.put(MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis());
            values.put(MediaStore.MediaColumns.IS_PENDING, true);

            Uri uri = getContentResolver().insert(externalContentUri, values);
            if (uri != null) {
                try {
                    if (WriteFileToStream(originalFile, getContentResolver().openOutputStream(uri))) {
                        values.put(MediaStore.MediaColumns.IS_PENDING, false);
                        getContentResolver().update(uri, values, null, null);
                    }
                } catch (Exception e) {
                    getContentResolver().delete(uri, null, null);
                }
            }
            originalFile.delete();
        } else {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(Uri.fromFile(originalFile));
            sendBroadcast(mediaScanIntent);
        }

    }

    private boolean WriteFileToStream(File originalFile, OutputStream openOutputStream) {
        try
        {
            try (InputStream in = new FileInputStream(originalFile)) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0)
                    openOutputStream.write(buf, 0, len);
            } //Log.e( "Unity", "Exception:", e );
        }
        catch( Exception e )
        {
            return false;
        }
        finally
        { try
            {
                openOutputStream.close();
            }
            catch( Exception e )
            {
                //Log.e( "Unity", "Exception:", e );
            }
        }
        return true;
    }

    public String getBatchDirectoryName() {
        String app_folder_path;
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            app_folder_path = getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString();
        } else {
            app_folder_path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera";
        }

        File dir = new File(app_folder_path);
        if (!dir.exists() && !dir.mkdirs()) {
        }
        return app_folder_path;
    }


    public void pressed(View view) {
        ImageButton flsh =  findViewById(R.id.flash);
                if(mCameraView.getFlash()==ImageCapture.FLASH_MODE_OFF){
                    flsh.setImageDrawable(getDrawable(R.drawable.ic_baseline_flash_on_24));
                    mCameraView.setFlash(ImageCapture.FLASH_MODE_ON);

                }else{
                    flsh.setImageDrawable(getDrawable(R.drawable.ic_baseline_flash_off_24));
                    mCameraView.setFlash(ImageCapture.FLASH_MODE_OFF);
                }
    }

    public void onClickButton(View view) {
        if(!IsVideo){
            cam_icon.setImageDrawable(getDrawable(ic_baseline_photo_camera_24));
            IsPhoto = false;
            IsVideo=true;
        }else{
            cam_icon.setImageDrawable(getDrawable(R.drawable.ic_baseline_videocam_24));
            IsPhoto =true;
            IsVideo=false;
        }
    }
}
