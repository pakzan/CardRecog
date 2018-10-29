package com.example.tpz.cardrecog;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.opencv.imgproc.Imgproc.boundingRect;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{

    CameraBridgeViewBase cameraBridgeViewBase;
    Mat mat;
    BaseLoaderCallback baseLoaderCallback;
    ImageView imageView, imageView2;
    TextView textView;
    EditText ipText;
    Button button;

    static String storeSuit = "";
    static String storeRank = "";
    static long lastTime;
    final int updateFreq = 1000;

    static {
        if (!OpenCVLoader.initDebug())
            Log.d("ERROR", "Unable to load OpenCV");
        else
            Log.d("SUCCESS", "OpenCV loaded");
    }

    private void checkPermission(String permission_type){
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                permission_type)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{permission_type},
                0);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        checkPermission(Manifest.permission.CAMERA);
        checkPermission(Manifest.permission.INTERNET);

        imageView = findViewById(R.id.imageView);
        imageView2 = findViewById(R.id.imageView2);
        textView = findViewById(R.id.text1);
        ipText = findViewById(R.id.ipText);
        button = findViewById(R.id.button);

        lastTime = System.currentTimeMillis();

        cameraBridgeViewBase = (JavaCameraView) findViewById(R.id.myCameraView);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);

        baseLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                switch (status){
                    case BaseLoaderCallback.SUCCESS:
                        cameraBridgeViewBase.enableView();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        };
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mat = new Mat(width, height, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mat.release();
    }

    void saveImage(Mat mat) throws IOException {
        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bitmap);

        String file_path = Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/img";
        File dir = new File(file_path);
        if(!dir.exists())
            dir.mkdirs();
        File file = new File(dir, "img" + System.currentTimeMillis() + ".jpg");
        FileOutputStream fOut = new FileOutputStream(file);

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
        fOut.flush();
        fOut.close();
    }

    public MatOfPoint getMaxContour(Mat mat){
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(mat,contours,new Mat(),Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);

        //find largest contour
        double maxArea = 0;
        MatOfPoint max_contour = null;

        Iterator<MatOfPoint> iterator = contours.iterator();
        while (iterator.hasNext()){
            MatOfPoint contour = iterator.next();
            double area = Imgproc.contourArea(contour);
            if(area > maxArea){
                maxArea = area;
                max_contour = contour;
            }
        }
        return max_contour;
    }

    public String getCardType(Mat currMat, String images_path) throws IOException {
        String[] images_name = getAssets().list(images_path);
        int min_diff_pixel = 4000;
        double min_diff_per = 1;
        String min_diff_str = "";
        final Mat[] tmp = new Mat[1];

        for (String image_name :
                images_name) {
            InputStream inputstream = getAssets().open(images_path + "/" + image_name);
            Mat assetMaT = new Mat();
            Utils.bitmapToMat(BitmapFactory.decodeStream(inputstream), assetMaT);
            Imgproc.cvtColor(assetMaT, assetMaT, Imgproc.COLOR_BGRA2GRAY, 1);

            final Mat sizedMat = new Mat();
            //resize Mat to fit assets images
            Size size = new Size(assetMaT.height(), assetMaT.width());
            Imgproc.resize(currMat, sizedMat, size);
            Core.rotate(sizedMat, sizedMat, Core.ROTATE_90_CLOCKWISE);
            //add border to image
//            Core.copyMakeBorder(sizedMat, sizedMat, border, border, border, border, Core.BORDER_REPLICATE);

            //compare image pixel
            Mat diffMat = new Mat();
            Core.absdiff(assetMaT, sizedMat, diffMat);
            int matchPixel = Core.countNonZero(diffMat);

            //compare image contour
            List<MatOfPoint> assetCon = new ArrayList<>();
            List<MatOfPoint> sizedCon = new ArrayList<>();
            Imgproc.findContours(assetMaT,assetCon,new Mat(),Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);
            Imgproc.findContours(sizedMat,sizedCon,new Mat(),Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);
            Double matchPer = Imgproc.matchShapes(getMaxContour(assetMaT), getMaxContour(sizedMat), Imgproc.CV_CONTOURS_MATCH_I3, 0);

            Log.d("total_diff", image_name + "     " + String.valueOf(matchPer));

            //get the most similar image in pixel and shape
            if (matchPixel < min_diff_pixel){
                min_diff_pixel = matchPixel;
//                min_diff_per = matchPer;
                min_diff_str = image_name;
                tmp[0] = sizedMat;
                //debug
                showImage(diffMat, imageView2);
            }
        }
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    checkPermission(Manifest.permission.CAMERA);
                    saveImage(tmp[0]);
                }catch (Exception e){

                }
            }
        });
        return min_diff_str.replace(".jpg", "");
    }

    public void showImage(Mat mat, final ImageView imageView){
//        Core.rotate(mat, mat, Core.ROTATE_90_CLOCKWISE);
        final Bitmap bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bitmap);

        Thread thread = new Thread()
        {
            @Override
            public void run() {
                runOnUiThread(new Runnable() //run on ui thread
                {
                    public void run()
                    {
                        imageView.setImageBitmap(bitmap);
                    }
                });
            }
        };
        thread.start();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mat = inputFrame.rgba();

        //remove glare
//        List<Mat> channels = new LinkedList();
//        Core.split(mat, channels);
//        CLAHE clahe = Imgproc.createCLAHE();
//        clahe.apply(channels.get(0), mat);

        //only show white to grey with some color tolerance
//        Mat filter_combined = new Mat();
//        Mat[] filter = new Mat[3];
//        int partition = 255 / filter.length;
//
//        // Convert to HSV
//        Mat hsvMat = new Mat();
//        Imgproc.cvtColor(mat, hsvMat, Imgproc.COLOR_RGB2HSV, 3);
//
//        //assign first filter to filter_combined
//        Core.inRange(hsvMat, new Scalar(0, 0, 0),
//                new Scalar(partition, partition, partition), filter_combined);
//        for(int i = 1; i < filter.length; i++) {
//            filter[i] = new Mat();
//            Core.inRange(mat, new Scalar(partition * i, partition * i, partition * i),
//                    new Scalar(partition * (i + 1), partition * (i + 1), partition * (i + 1)), filter[i]);
//            Core.bitwise_or(filter_combined, filter[i], filter_combined);
//        }
//
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2GRAY);
//        //completely convert to grey
//        Core.bitwise_xor(mat, filter_combined, mat);
        Imgproc.GaussianBlur(mat, mat, new Size(1,1), 0);

        Imgproc.threshold(mat, mat, 120, 255, Imgproc.THRESH_BINARY);
//        Imgproc.adaptiveThreshold(mat, mat, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 15, 40);

        //debug
        showImage(mat, imageView);

        //find largest contour
        MatOfPoint max_contour = getMaxContour(mat);

        //check if max_contour exists
        if(max_contour != null){
            //draw a polynomial around the contour
            double epsilon = 0.02*Imgproc.arcLength(new MatOfPoint2f(max_contour.toArray()),true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(new MatOfPoint2f(max_contour.toArray()),approx,epsilon,true);

            //if found rectangle shape(card), continue stuff
            if(approx.toArray().length == 4) {
                RotatedRect rect = Imgproc.minAreaRect(approx);

                //draw rotated rectangle
                Point points[] = approx.toArray();
//                rect.points(points);
                for (int i = 0; i < 4; ++i) {
                    Imgproc.line(mat, points[i], points[(i + 1) % 4], new Scalar(255, 255, 255));
                }

                //adjust the orientation of the card
                double width01 = Math.sqrt(Math.pow(points[0].x - points[1].x, 2) + Math.pow(points[0].y - points[1].y, 2));
                double height12 = Math.sqrt(Math.pow(points[2].x - points[1].x, 2) + Math.pow(points[2].y - points[1].y, 2));
                double width23 = Math.sqrt(Math.pow(points[2].x - points[3].x, 2) + Math.pow(points[2].y - points[3].y, 2));
                double height30 = Math.sqrt(Math.pow(points[0].x - points[3].x, 2) + Math.pow(points[0].y - points[3].y, 2));

                double total_width, total_height;
                if(Math.abs(height12 - height30) > Math.abs(width01 - width23)){
                    total_width = (width01 + width23) * (Math.max(height12, height30)/Math.min(height12, height30));
                    total_height = 2 * Math.max(height12, height30);
                }else{
                    total_width = 2 * Math.max(width01, width23);
                    total_height = (height12 + height30) * (Math.max(width01, width23)/Math.min(width01, width23));
                }

                //rotate card
                if (total_width > total_height) {
                    Point tmpPoint = points[3];
                    points[3] = points[2];
                    points[2] = points[1];
                    points[1] = points[0];
                    points[0] = tmpPoint;
                }

                //input rectangle real size
                MatOfPoint2f dst = new MatOfPoint2f(
                        new Point(0, 0),
                        new Point(0, 719),
                        new Point(809, 719),
                        new Point(809, 0)
                );
                //get transform perspective of polynomial to rectangle
                Mat warpMat = Imgproc.getPerspectiveTransform(new MatOfPoint2f(points), dst);
                //transform polynomial to rectangle
                Imgproc.warpPerspective(mat, mat, warpMat, mat.size());

                //draw cropped image for better visualization
//                Imgproc.rectangle(mat, new Point(0, 604), new Point(141, 719), new Scalar(255, 255, 255));
//                Imgproc.rectangle(mat, new Point(86, 604), new Point(227, 719), new Scalar(255, 255, 255));

                //crop upper left of the card
                //invert black and white
                Mat cropMat = new Mat(mat, new Rect(0, 604, 227, 115));
                Core.bitwise_not(cropMat, cropMat);

                //get rank and suit from cropped image
                Mat Rank = new Mat(cropMat, new Rect(0, 0, 141, 115));
                Mat Suit = new Mat(cropMat, new Rect(86, 0, 141, 115));


                //crop rank and suit once more to fit the image
                MatOfPoint tempRank = getMaxContour(Rank);

                if (tempRank != null) {
                    //crop rank
                    Rank = new Mat(Rank, boundingRect(tempRank));
                }

                //crop suit
                MatOfPoint tempSuit = getMaxContour(Suit);
                if (tempSuit != null)
                    Suit = new Mat(Suit, boundingRect(tempSuit));

                //match suit
                if (tempRank != null && tempSuit != null) {
                    final String SRank, SSuit;
                    try {
                        SRank = getCardType(Rank, "Rank");
                        SSuit = getCardType(Suit, "Suit");

                        //print result to textview
                        Thread thread = new Thread() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() //run on ui thread
                                {
                                    public void run() {
                                        //if no matched card, output empty string
                                        if(TextUtils.isEmpty(SRank) || TextUtils.isEmpty(SSuit))
                                            textView.setText("");
                                        else {
                                            textView.setText(SRank + " " + SSuit);

                                            //send card info to server
                                            // make sure the detected values are stable for 1 second before update
                                            if (!storeRank.equals(SRank) || !storeSuit.equals(SSuit)) {
                                                storeRank = SRank;
                                                storeSuit = SSuit;
                                                lastTime = System.currentTimeMillis();
                                            }
                                            //send info after values stable for 1 second
                                            if (System.currentTimeMillis() - lastTime > updateFreq) {
                                                storeCardInfo(new CardInfo(SRank, SSuit), ipText.getText().toString());
                                                lastTime = System.currentTimeMillis();
                                            }
                                        }
                                    }
                                });
                            }
                        };
                        thread.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
        return mat;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(cameraBridgeViewBase!=null)
            cameraBridgeViewBase.disableView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!OpenCVLoader.initDebug())
            Toast.makeText(getApplicationContext(),"There is a problem in openCV", Toast.LENGTH_SHORT).show();
        else
            baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(cameraBridgeViewBase!=null)
            cameraBridgeViewBase.disableView();
    }


    private void storeCardInfo(CardInfo cardInfo, String ipAddress){
        ServerRequests serverRequests = new ServerRequests(this);
        serverRequests.storeCardInfoInBackground(cardInfo, ipAddress, new GetCallback() {
            @Override
            public void done() {
            }
        });
    }
}
