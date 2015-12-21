package com.example.wangweimin.opencvtest;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by wangweimin on 15/12/14.
 */
public class ChooseModelActivity extends AppCompatActivity {

    @Bind(R.id.model_group)
    RadioGroup mModelGroup;

    @Bind(R.id.mean_blur_btn)
    RadioButton mMeanBlurBtn;

    @Bind(R.id.gaussian_blur_btn)
    RadioButton mGaussianBlurBtn;

    @Bind(R.id.sharpen_kernel_btn)
    RadioButton mSharpenBtn;

    @Bind(R.id.dilation_btn)
    RadioButton mDilationBtn;

    @Bind(R.id.erosion_btn)
    RadioButton mErosionBtn;

    @Bind(R.id.threshold_binary_btn)
    RadioButton mBinaryBtn;

    @Bind(R.id.threshold_trunc_btn)
    RadioButton mTruncBtn;

    @Bind(R.id.threshold_to_zero_btn)
    RadioButton mToZeroBtn;

    @Bind(R.id.adaptive_threshold_gaussian_btn)
    RadioButton mAdaptiveBtn;

    @Bind(R.id.go_btn)
    Button mGoBtn;

    private int actionMode;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        ButterKnife.bind(this);
        context = getApplicationContext();
        init();
    }

    public void init() {
        mModelGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if (i == mMeanBlurBtn.getId()) {
                    actionMode = Constants.MEAN_BLUR;
                } else if (i == mGaussianBlurBtn.getId()) {
                    actionMode = Constants.GAUSSIAN_BLUR;
                } else if (i == mSharpenBtn.getId()) {
                    actionMode = Constants.SHARPEN_KERNEL;
                } else if (i == mDilationBtn.getId()) {
                    actionMode = Constants.DILATION;
                } else if (i == mErosionBtn.getId()) {
                    actionMode = Constants.EROSION;
                } else if (i == mBinaryBtn.getId()) {
                    actionMode = Constants.THRESHOLD_BINARY;
                } else if (i == mTruncBtn.getId()) {
                    actionMode = Constants.THRESHOLD_TRUNC;
                } else if (i == mToZeroBtn.getId()) {
                    actionMode = Constants.THRESHOLD_TO_ZERO;
                } else if (i == mAdaptiveBtn.getId()) {
                    actionMode = Constants.ADAPTIVE_THRESHOLD_GAUSSIAN;
                }
            }
        });


        mGoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, MainActivity.class);
                intent.putExtra("ACTION_MODE", actionMode);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }
}
