package com.suez.addons.blending;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;

import com.odoo.R;
import com.suez.SuezActivity;
import com.suez.SuezConstants;
import com.suez.addons.scan.ScanZbarActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by joseph on 18-5-29.
 */

public class MixBlendingMenusActivity extends SuezActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.suez_mix_blending_menus_activity);
        ButterKnife.bind(this);
        initToolbar(R.string.title_suez_mix_blending);
    }

    @OnClick({R.id.btnNewMixBlending, R.id.btnAddMixBlending})
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.btnNewMixBlending:
                intent = new Intent(this, ScanZbarActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.putExtra(SuezConstants.COMMON_KEY, SuezConstants.CREATE_BLENDING_KEY);
                startActivity(intent);
                break;
            case R.id.btnAddMixBlending:
                intent = new Intent(this, ScanZbarActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.putExtra(SuezConstants.COMMON_KEY, SuezConstants.ADD_BLENDING_KEY);
                startActivity(intent);
                break;
        }
    }
}
