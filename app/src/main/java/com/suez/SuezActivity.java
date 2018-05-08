package com.suez;

import android.content.Intent;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.Spinner;

import com.odoo.App;
import com.odoo.R;
import com.odoo.core.support.OdooCompatActivity;
import com.odoo.core.support.addons.fragment.IOnSearchViewChangeListener;
import com.odoo.core.utils.OAppBarUtils;
import com.odoo.core.utils.OPreferenceManager;

import java.util.Locale;

/**
 * Created by joseph on 18-5-2.
 */

public class SuezActivity extends OdooCompatActivity {
    public static final String TAG = SuezActivity.class.getSimpleName();
    protected App app;
    protected boolean isNetwork;
    private IOnSearchViewChangeListener mOnSearchViewChangeListener;
    private SearchView mSearchView;
    private SearchView.OnCloseListener closeListener = new SearchView.OnCloseListener() {
        @Override
        public boolean onClose() {
            if (!TextUtils.isEmpty(mSearchView.getQuery())){
                mSearchView.setQuery(null, true);
            }
            return true;
        }
    };
    private SearchView.OnQueryTextListener searchViewQueryListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            String newFilter = !TextUtils.isEmpty(newText) ? newText : null;
            return mOnSearchViewChangeListener.onSearchViewTextChange(newFilter);
        }
    };
    private Boolean mHasActionBarSpinner = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        this.app = (App) this.getApplication();
        this.isNetwork = app.networkState;
        this.changeAppLanguage();
    }

    public void changeAppLanguage(){
        OPreferenceManager preferenceManager = new OPreferenceManager(app);
        String language = preferenceManager.getString("key_language", "zh");
        Configuration config = app.getResources().getConfiguration();
        if (language.equals("zh")){
            Locale.setDefault(Locale.CHINA);
            config.locale = Locale.CHINA;
        } else {
            Locale.setDefault(Locale.ENGLISH);
            config.locale = Locale.ENGLISH;
        }
        app.getResources().updateConfiguration(config, app.getResources().getDisplayMetrics());
    }

    protected void initToolbar(int id){
        OAppBarUtils.setAppBar(this, true);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(id);
        }
    }

    protected void initToolbar(String title){
        OAppBarUtils.setAppBar(this, true);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(title);
        }
    }

    public void setHasSearchView(IOnSearchViewChangeListener listener, Menu menu, int menu_id){
        this.mOnSearchViewChangeListener = listener;
        this.mSearchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(menu_id));
        if (this.mSearchView != null){
            this.mSearchView.setOnCloseListener(closeListener);
            this.mSearchView.setOnQueryTextListener(searchViewQueryListener);
            this.mSearchView.setIconifiedByDefault(true);
        }
    }

    public void setHasActionBarSpinner(Boolean hasActionBarSpinner, ActionBar actionBar){
        if (actionBar != null){
            Spinner spinner = (Spinner) findViewById(R.id.spinner_nav);
            if (hasActionBarSpinner){
                if (spinner != null){
                    spinner.setVisibility(View.VISIBLE);
                }
                actionBar.setDisplayShowTitleEnabled(false);
            } else {
                if (spinner != null){
                    spinner.setVisibility(View.GONE);
                }
                actionBar.setDisplayShowTitleEnabled(true);
            }
            mHasActionBarSpinner = hasActionBarSpinner;
        }
    }

    public Spinner getActionBarSpinner(){
        Spinner spinner = null;
        if (mHasActionBarSpinner){
            spinner = (Spinner) findViewById(R.id.spinner_nav);
            spinner.setAdapter(null);
        }
        return spinner;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        switch (id){
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void startActivity(Intent intent){
        super.startActivity(intent);
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
    }
}
