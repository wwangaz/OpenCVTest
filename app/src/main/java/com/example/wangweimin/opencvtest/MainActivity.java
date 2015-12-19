package com.example.wangweimin.opencvtest;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
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
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;
import java.io.InputStream;

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
    private static int ACTION_MODE = 0;

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

            case SELECT_MODE:
                ACTION_MODE = data.getIntExtra("ACTION_MODE", 0);
                if (src != null) {
                    switch (ACTION_MODE) {
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
                        default:
                            Toast.makeText(context, "请选择要处理的效果", Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
                break;
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

    private void setProcessedImage(Mat src) {
        Bitmap processedImage = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src, processedImage);
        imageViewProcessed.setImageBitmap(processedImage);

        dismissProgress();
    }

    private void dismissProgress() {
        if (progressDialog != null)
            progressDialog.dismiss();
    }
}
