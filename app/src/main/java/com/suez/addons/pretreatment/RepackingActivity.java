package com.suez.addons.pretreatment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.odoo.R;

/**
 * Created by joseph on 18-5-20.
 */

public class RepackingActivity extends ProcessingActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initToolbar(R.string.label_repacking);
    }

    @Override
    protected void initView() {
        super.initView();
//        repackingLocation.setVisibility(View.VISIBLE);
//        destinationLocation.setVisibility(View.VISIBLE);
//        packagingId.setVisibility(View.VISIBLE);
//        pretreatmentQty.setVisibility(View.VISIBLE);
//        packagingNumber.setVisibility(View.VISIBLE);
//        remainQty.setVisibility(View.VISIBLE);
    }

    @Override
    protected void performProcessing() {
        super.performProcessing();
    }
}
