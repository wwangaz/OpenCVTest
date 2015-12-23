package com.example.wangweimin.opencvtest;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    @Bind(R.id.ivImage)
    ImageView imageView;

    @Bind(R.id.ivImageProcessed)
    ImageView imageViewProcessed;

    @Bind(R.id.process_model_tv)
    TextView mProcessModeTV;

    private static final int SELECT_PHOTO = 1;
    private static final int SELECT_MODE = 2;
    private static final int SELECT_ORIGINAL_PIC = 3;
    int actionMode = 0;

    private ProgressDialog progressDialog;
    private Context context;
    Mat src;

    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ButterKnife.bind(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_10, this, mOpenCVCallBack);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.action_load_image) {
            Intent selectPhotoIntent = new Intent(Intent.ACTION_PICK);
            selectPhotoIntent.setType("image/*");
            startActivityForResult(selectPhotoIntent, SELECT_PHOTO);
            return true;
        }

        if (id == R.id.action_select_mode) {
            Intent intent = new Intent(context, ChooseModelActivity.class);
            startActivityForResult(intent, SELECT_MODE);
        }

        if (id == R.id.action_select_original) {
            Intent intent = new Intent(Intent.ACTION_PICK, Uri.parse("content://media/internal/images/media"));
            startActivityForResult(intent, SELECT_ORIGINAL_PIC);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SELECT_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {
                        final Uri imageUri = data.getData();
                        final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                        imageView.setImageBitmap(selectedImage);
                        src = new Mat(selectedImage.getHeight(), selectedImage.getWidth(), CvType.CV_8UC4);
                        Utils.bitmapToMat(selectedImage, src);
                    } catch (FileNotFoundException e) {
                        Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                break;

            case SELECT_ORIGINAL_PIC:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = data.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};

                    Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                    if (cursor != null) {
                        cursor.moveToFirst();
                        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                        String filePath = cursor.getString(columnIndex);
                        cursor.close();

                        //To speed up loading of Images, set SampleSize
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 2;

                        Bitmap temp = BitmapFactory.decodeFile(filePath, options);

                        int orientation = 0;

                        try {
                            ExifInterface imgParams = new ExifInterface(filePath);
                            orientation = imgParams.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        Bitmap originalBitmap = rotateBitmap(temp, orientation);

                        Bitmap currentBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                        Mat currentMat = new Mat(currentBitmap.getHeight(), currentBitmap.getWidth(), CvType.CV_8U);
                        Utils.bitmapToMat(currentBitmap, currentMat);
                        setProcessedImage(currentMat);
                    }
                }

            case SELECT_MODE:
                actionMode = data.getIntExtra("ACTION_MODE", 0);
                if (resultCode == RESULT_OK) {
                    if (src != null) {
                        progressDialog = ProgressDialog.show(context, "处理中请稍候", "");
                        switch (actionMode) {
                            case Constants.MEAN_BLUR:
                                doMeanBlur(src);
                                break;
                            case Constants.GAUSSIAN_BLUR:
                                doGaussianBlur(src);
                                break;
                            case Constants.SHARPEN_KERNEL:
                                doSharpen(src);
                                break;
                            case Constants.DILATION:
                                doDilation(src);
                                break;
                            case Constants.EROSION:
                                doErosion(src);
                                break;
                            case Constants.THRESHOLD_BINARY:
                                doThresholdBinary(src);
                                break;
                            case Constants.THRESHOLD_TRUNC:
                                doThresholdTrunc(src);
                                break;
                            case Constants.THRESHOLD_TO_ZERO:
                                doThresholdToZero(src);
                                break;
                            case Constants.ADAPTIVE_THRESHOLD_GAUSSIAN:
                                doAdaptiveGaussian(src);
                                break;
                            case Constants.DOG:
                                doDOG(src);
                                break;
                            case Constants.CANNY:
                                doCanny(src);
                                break;
                            case Constants.SOBEL:
                                doSobel(src);
                                break;
                            case Constants.HARRIS_CORNER:
                                doHarrisCorner(src);
                                break;
                            case Constants.HOUGH_LINE:
                                doHoughLine(src);
                                break;
                            case Constants.HOUGH_CIRCLE:
                                doHoughCircle(src);
                                break;
                            default:
                                Toast.makeText(context, "请选择要处理的效果", Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                    break;
                }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void doMeanBlur(@NonNull Mat src) {
        mProcessModeTV.setText(getString(R.string.mean_blur));
        Mat dst = new Mat(src.cols(), src.rows(), CvType.CV_8UC4);
        Imgproc.blur(src, dst, new Size(3, 3));
        setProcessedImage(dst);
    }

    private void doGaussianBlur(@NonNull Mat src) {
        mProcessModeTV.setText(getString(R.string.gaussian_blur));
        Mat dst = new Mat(src.cols(), src.rows(), CvType.CV_8UC4);
        Imgproc.GaussianBlur(src, dst, new Size(3, 3), 0);
        setProcessedImage(dst);
    }

    private void doSharpen(@NonNull Mat src) {
        mProcessModeTV.setText(getString(R.string.sharpen_kernel));
        Mat kernel = new Mat(3, 3, CvType.CV_16SC1);
        kernel.put(0, 0, 0, -1, 0, -1, 5, -1, 0, -1, 0);
        Mat dst = new Mat(src.cols(), src.rows(), CvType.CV_8UC4);
        Imgproc.filter2D(src, dst, src.depth(), kernel);
        setProcessedImage(dst);
    }

    private void doDilation(@NonNull Mat src) {
        mProcessModeTV.setText(getString(R.string.dilation));
        Mat kernelDilate = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Mat dst = new Mat(src.cols(), src.rows(), CvType.CV_8UC4);
        Imgproc.dilate(src, dst, kernelDilate);
        setProcessedImage(dst);
    }

    private void doErosion(@NonNull Mat src) {
        mProcessModeTV.setText(getString(R.string.erosion));
        Mat kernelErode = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE, new Size(5, 5));
        Mat dst = new Mat(src.cols(), src.rows(), CvType.CV_8UC4);
        Imgproc.erode(src, dst, kernelErode);
        setProcessedImage(dst);
    }

    private void doThresholdBinary(@NonNull Mat src) {
        mProcessModeTV.setText(getString(R.string.threshold_binary));
        Mat dst = new Mat(src.cols(), src.rows(), CvType.CV_8UC4);
        Imgproc.threshold(src, dst, 100, 255, Imgproc.THRESH_BINARY);
        setProcessedImage(dst);
    }

    private void doThresholdTrunc(@NonNull Mat src) {
        mProcessModeTV.setText(getString(R.string.threshold_binary));
        Mat dst = new Mat(src.cols(), src.rows(), CvType.CV_8UC4);
        Imgproc.threshold(src, dst, 100, 255, Imgproc.THRESH_TRUNC);
        setProcessedImage(dst);
    }

    private void doThresholdToZero(@NonNull Mat src) {
        mProcessModeTV.setText(getString(R.string.threshold_binary));
        Mat dst = new Mat(src.cols(), src.rows(), CvType.CV_8UC4);
        Imgproc.threshold(src, dst, 100, 255, Imgproc.THRESH_TOZERO);
        setProcessedImage(dst);
    }

    private void doAdaptiveGaussian(@NonNull Mat src) {
        mProcessModeTV.setText(getString(R.string.threshold_binary));
        Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY);
        Mat dst = new Mat(src.cols(), src.rows(), CvType.CV_8UC1);
        Imgproc.adaptiveThreshold(src, dst, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 3, 0);
        setProcessedImage(dst);
    }

    private void doDOG(Mat src) {
        Mat grayMat = new Mat();
        Mat temp1 = new Mat();
        Mat temp2 = new Mat();

        Imgproc.cvtColor(src, grayMat, Imgproc.COLOR_BGR2GRAY);

        Imgproc.GaussianBlur(grayMat, temp1, new Size(15, 15), 5);
        Imgproc.GaussianBlur(grayMat, temp2, new Size(21, 21), 5);

        Mat DoG = new Mat();
        Core.absdiff(temp1, temp2, DoG);

        Core.multiply(DoG, new Scalar(100), DoG);

        Imgproc.threshold(DoG, DoG, 50, 255, Imgproc.THRESH_BINARY_INV);

        setProcessedImage(DoG);
    }

    private void doCanny(Mat src) {
        Mat grayMat = new Mat();
        Mat cannyMat = new Mat();

        Imgproc.cvtColor(src, grayMat, Imgproc.COLOR_BGR2GRAY);

        Imgproc.Canny(grayMat, cannyMat, 10, 100);

        setProcessedImage(cannyMat);
    }

    private void doSobel(Mat src) {
        Mat grayMat = new Mat();
        Mat sobel = new Mat();

        Mat grad_x = new Mat();
        Mat abs_grad_x = new Mat();

        Mat grad_y = new Mat();
        Mat abs_grad_y = new Mat();

        Imgproc.cvtColor(src, grayMat, Imgproc.COLOR_BGR2GRAY);

        Imgproc.Sobel(grayMat, grad_x, CvType.CV_16S, 1, 0, 3, 1, 0);
        Imgproc.Sobel(grayMat, grad_y, CvType.CV_16S, 0, 1, 3, 1, 0);

        Core.convertScaleAbs(grad_x, abs_grad_x);
        Core.convertScaleAbs(grad_y, abs_grad_y);

        Core.addWeighted(abs_grad_x, 0.5, abs_grad_y, 0.5, 1, sobel);

        setProcessedImage(sobel);
    }

    private void doHarrisCorner(Mat src) {
        Mat grayMat = new Mat();
        Mat corners = new Mat();

        Imgproc.cvtColor(src, grayMat, Imgproc.COLOR_BGR2GRAY);

        Mat tempMat = new Mat();

        Imgproc.cornerHarris(grayMat, tempMat, 2, 3, 0.04);

        Mat tempMatNorm = new Mat();

        Core.normalize(tempMat, tempMatNorm, 0, 255, Core.NORM_MINMAX);

        Core.convertScaleAbs(tempMatNorm, corners);

        Random r = new Random();

        for (int i = 0; i < corners.cols(); i++) {
            for (int j = 0; j < corners.rows(); j++) {
                double[] values = tempMatNorm.get(j, i);
                if (values[0] > 150) {
                    Imgproc.circle(corners, new Point(j, i), 5, new Scalar(r.nextInt(255)), 2);
                }
            }
        }

        setProcessedImage(corners);
    }

    private void doHoughLine(Mat src) {
        Mat grayMat = new Mat();
        Mat cannyEdges = new Mat();
        Mat lines = new Mat();

        Imgproc.cvtColor(src, grayMat, Imgproc.COLOR_BGR2GRAY);

        Imgproc.Canny(grayMat, cannyEdges, 10, 100);

        Imgproc.HoughLinesP(cannyEdges, lines, 1, Math.PI / 180, 50, 20, 20);

        Mat houghLines = new Mat();
        houghLines.create(lines.rows(), lines.cols(), CvType.CV_8UC1);

        for (int i = 0; i < lines.cols(); i++) {
            double[] points = lines.get(0, i);
            double x1, y1, x2, y2;
            x1 = points[0];
            y1 = points[1];
            x2 = points[3];
            y2 = points[4];

            Point pt1 = new Point(x1, y1);
            Point pt2 = new Point(x2, y2);

            Imgproc.line(houghLines, pt1, pt2, new Scalar(255, 0, 0), 1);
        }

        setProcessedImage(houghLines);
    }

    private void doHoughCircle(Mat src) {
        Mat grayMat = new Mat();
        Mat cannyEdge = new Mat();
        Mat circles = new Mat();

        Imgproc.cvtColor(src, grayMat, Imgproc.COLOR_BGR2GRAY);

        Imgproc.Canny(grayMat, cannyEdge, 10, 100);

        Imgproc.HoughCircles(cannyEdge, circles, Imgproc.CV_HOUGH_GRADIENT, 1, cannyEdge.rows() / 15);

        Mat houghCircles = new Mat();
        houghCircles.create(cannyEdge.rows(), cannyEdge.cols(), CvType.CV_8UC1);

        for (int i = 0; i < circles.cols(); i++) {
            double[] parameters = circles.get(0, i);
            double x, y;
            int r;

            x = parameters[0];
            y = parameters[1];
            r = (int) parameters[2];

            Point center = new Point(x, y);

            Imgproc.circle(houghCircles, center, r, new Scalar(255, 0, 0));
        }

        setProcessedImage(houghCircles);
    }

    private void setProcessedImage(Mat src) {
        Bitmap processedImage = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src, processedImage);
        imageViewProcessed.setImageBitmap(processedImage);
        dismissProgress();
    }

    private Bitmap rotateBitmap(Bitmap src, int orientation) {
        Matrix rotate90 = new Matrix();
        rotate90.postRotate(orientation);
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), rotate90, false);
    }

    private void dismissProgress() {
        if (progressDialog != null)
            progressDialog.dismiss();
    }
}
