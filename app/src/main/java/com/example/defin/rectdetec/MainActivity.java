package com.example.defin.rectdetec;

import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;

public class MainActivity extends Activity implements CvCameraViewListener2 {


    Timer timer;
    TextToSpeech tos;
    private static final String TAG = "derp";
    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.rect_activity_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        //Text to speech
        tos = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tos.setLanguage(Locale.GERMAN);
                }
            }
        });


    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }


    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat pic = inputFrame.gray();
        Mat picori = inputFrame.rgba();


        //Apply gaussian blur to remove noise
        Imgproc.GaussianBlur(pic, pic, new Size(11, 11), 0);
        //AdaptiveThreshold
        Imgproc.adaptiveThreshold(pic, pic, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 5, 2);
        //Invert the image
        Core.bitwise_not(pic, pic);
        //Dilate
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_DILATE, new Size(3, 3), new Point(1, 1));
        Imgproc.dilate(pic, pic, kernel);


        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();

        //Find Contours
        Imgproc.findContours(pic, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        //For conversion later on
        MatOfPoint2f approxCurve = new MatOfPoint2f();


        //For each contour found
        for (int i = 0; i < contours.size(); i++) {

            //Convert contours from MatOfPoint to MatOfPoint2f
            MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(i).toArray());
            //Processing on mMOP2f1 which is in type MatOfPoint2f
            double approxDistance = Imgproc.arcLength(contour2f, true) * 0.02;

            if (approxDistance > 5) {

                //Find Polygons
                Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);


                //Convert back to MatOfPoint
                MatOfPoint points = new MatOfPoint(approxCurve.toArray());


                //Rectangle Checks - Points, area, convexity
                if (points.total() == 4 && Math.abs(Imgproc.contourArea(points)) > 1000 && Imgproc.isContourConvex(points)) {


                    // Get bounding rect of contour
                    Rect rect = Imgproc.boundingRect(points);

                    if (Math.abs(rect.height - rect.width) < 100) {
                        // draw enclosing rectangle
                        //TODO Change back to picori

                        Imgproc.rectangle(picori, rect.tl(), rect.br(), new Scalar(255, 0, 0), 1, 8, 0);

                        if (tos.isSpeaking() == false) {
                            tos.speak("Rechteck gefunden", TextToSpeech.QUEUE_FLUSH, null);
                            tos.playSilence(2000, TextToSpeech.QUEUE_ADD, null);
                        }


                    }

                }


            }


        }


        //TODO Change back to picori
        return picori;

    }


}