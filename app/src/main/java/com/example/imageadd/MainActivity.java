package com.example.imageadd;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    Button takePicture, processImage;
    ImageView iv;
    TextView logger;
    String imagefile;
    Bitmap imagebmp;
    Canvas imageCanvas;
    Paint myColor;
    ActivityResultLauncher<Intent> myActivityResultLauncher;
    String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        takePicture = findViewById(R.id.takepicture);
        takePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPicture();
            }
        });
        processImage = findViewById(R.id.process);
        processImage.setEnabled(false);
        processImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                procesor();
            }
        });
        logger = findViewById(R.id.logger);
        iv = findViewById(R.id.imageView);

        //setup the paint object.
        myColor = new Paint();
        myColor.setColor(Color.RED);
        myColor.setStyle(Paint.Style.STROKE);
        myColor.setStrokeWidth(10);
        myColor.setTextSize(myColor.getTextSize() * 10);
        myActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {@Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent data = result.getData();
                        Log.wtf("CAPTURE FILE", "we got a file?");
                        imagebmp = loadAndRotateImage(imagefile);
                        if (imagebmp != null) {
                            imageCanvas = new Canvas(imagebmp);
                            iv.setImageBitmap(imagebmp);
                            processImage.setEnabled(true);
                            logger.setText("Image should have loaded correctly");
                        } else {
                            logger.setText("Image failed to load or was canceled.");
                        }
                    }
                }
            }
        );

        // High-accuracy landmark detection and face classification
        FaceDetectorOptions highAccuracyOpts =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build();

        // Real-time contour detection
        FaceDetectorOptions realTimeOpts =
                new FaceDetectorOptions.Builder()
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .build();

    }

    public void getPicture() {
        //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        //  File mediaFile = new File(storageDir.getPath() +File.separator + "IMG_" + timeStamp+ ".jpg");
        File mediaFile = new File(storageDir.getPath() + File.separator + "IMG_working.jpg");
        Uri photoURI = FileProvider.getUriForFile(this, "imageadd.fileprovider", mediaFile);

        imagefile = mediaFile.getAbsolutePath();
        Log.wtf("File", imagefile);
        // Uri photoURI = getUriForFile(this, "edu.cs4730.piccapture3.fileprovider",mediaFile);
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        myActivityResultLauncher.launch(intent);
    }

    public void procesor() {
        if (imagebmp == null) return;
        InputImage image = InputImage.fromBitmap(imagebmp, 0);
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .build();
        FaceDetector detector = FaceDetection.getClient(options);

        Task<List<Face>> result =
            detector.process(image).addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                @Override
                public void onSuccess(List<Face> faces) {
                    for (Face face : faces) {
                        float rotY = face.getHeadEulerAngleY();  // Head is rotated to the right rotY degrees
                        float rotZ = face.getHeadEulerAngleZ();  // Head is tilted sideways rotZ degrees

                        List<PointF> noseBridge = face.getContour(FaceContour.NOSE_BRIDGE).getPoints();   //red clown nose from point 1 to point 0.5? or work out distance between 0 and 1 and halve it for height
                        List<PointF> noseBottom = face.getContour(FaceContour.NOSE_BOTTOM).getPoints();   //red clown nose width from point 0 to point 2.
                        List<PointF> leftEye = face.getContour(FaceContour.LEFT_EYE).getPoints();   //eyepatch from point 0 to point 8 width, and then height 4 to 12 *2
                        List<PointF> faceContour = face.getContour(FaceContour.FACE).getPoints();   //big beard from point 25 to 11 width, height is y coord of 26 to y coord of 18.

                        float leftx = noseBottom.get(0).x;
                        float topy = noseBridge.get(0).y;
                        topy = topy + (float)(topy*0.05);
                        float rightx = noseBottom.get(2).x;
                        float bottomy = noseBridge.get(1).y;
                        bottomy = bottomy + (float)(bottomy*0.05);
                        Drawable d = getResources().getDrawable(R.drawable.clown, null);
                        Bitmap bitmap = ((BitmapDrawable) d).getBitmap();
                        RectF shape = new RectF(leftx, topy, rightx, bottomy);
                        imageCanvas.drawBitmap(bitmap, null,  shape, myColor);

                        leftx = leftEye.get(0).x;
                        topy = leftEye.get(4).y;
                        rightx = leftEye.get(8).x;
                        bottomy = leftEye.get(12).y;
                        bottomy = bottomy + (float)(bottomy*0.05);
                        d = getResources().getDrawable(R.drawable.pirate_eye_patch, null);
                        bitmap = ((BitmapDrawable) d).getBitmap();
                        shape = new RectF(leftx, topy, rightx, bottomy);
                        imageCanvas.drawBitmap(bitmap, null,  shape, myColor);

                        leftx = faceContour.get(26).x;
                        topy = faceContour.get(26).y;
                        rightx = faceContour.get(10).x;
                        bottomy = faceContour.get(18).y;
                        d = getResources().getDrawable(R.drawable.beard, null);
                        bitmap = ((BitmapDrawable) d).getBitmap();
                        shape = new RectF(leftx, topy, rightx, bottomy);
                        imageCanvas.drawBitmap(bitmap, null,  shape, myColor);

                    }
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    logger.setText((CharSequence) e);
                    // ...
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // Task failed with an exception
                    logger.setText("Processor failed!");
                }
            });


    }


    public Bitmap loadAndRotateImage(String path) {
        int rotate = 0;
        ExifInterface exif;

        Bitmap bitmap = BitmapFactory.decodeFile(path);
        try {
            exif = new ExifInterface(path);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL);
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotate = 270;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotate = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotate = 90;
                break;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(rotate);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
    }

}