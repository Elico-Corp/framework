package com.suez.utils;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import com.odoo.BaseAbstractListener;
import com.odoo.R;
import com.odoo.core.orm.ODataRow;
import com.odoo.core.orm.OModel;
import com.odoo.core.rpc.helper.ODomain;
import com.odoo.core.rpc.helper.OdooFields;
import com.odoo.core.utils.OResource;
import com.suez.SuezConstants;

import java.util.List;

/**
 * Created by joseph on 18-5-11.
 */

public class SearchRecordsOnlineUtils {
    private static final String TAG = SearchRecordsOnlineUtils.class.getSimpleName();
    private OModel mModel;
    private OdooFields fields;
    private ODomain domain;
    private int limit = SuezConstants.SEARCH_RECORD_DEFAULT_LIMIT;
    private int offset = 0;
    private String sortby;
    private BaseAbstractListener listener;
    private List<ODataRow> records;
    private boolean showDialog = true;

    public SearchRecordsOnlineUtils(OModel model, OdooFields fields, ODomain domain, int limit, int offset, String sort) {
        this.mModel = model;
        this.fields = fields;
        this.domain = domain;
        this.limit = limit;
        this.offset = offset;
        this.sortby = sort;
    }

    public SearchRecordsOnlineUtils(OModel model, OdooFields fields, ODomain domain, int limit, int offset) {
        this(model, fields, domain, limit, offset, null);
    }

    public SearchRecordsOnlineUtils(OModel model, OdooFields fields, ODomain domain, int limit) {
        this(model, fields, domain, limit, 0);
    }

    public SearchRecordsOnlineUtils(OModel model, OdooFields fields, ODomain domain) {
        this(model, fields, domain, SuezConstants.SEARCH_RECORD_DEFAULT_LIMIT);
    }


    public SearchRecordsOnlineUtils(OModel model, OdooFields fields) {
        this(model, fields, new ODomain());
    }

    public SearchRecordsOnlineUtils(OModel model, ODomain domain) {
        this(model, new OdooFields(model.getColumns()), domain);
    }

    public SearchRecordsOnlineUtils setShowDialog(boolean showDialog) {
        this.showDialog = showDialog;
        return this;
    }

    public SearchRecordsOnlineUtils setListener (BaseAbstractListener listener) {
        this.listener = listener;
        return this;
    }

    public void searchRecordsOnServer() {
        SearchRecordTask task = new SearchRecordTask();
        task.execute();
    }

    private class SearchRecordTask extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog;

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                records = mModel.getServerDataHelper().searchRecords(fields, domain, offset, limit, sortby);
            }
            catch (Exception e) {
                e.printStackTrace();
                LogUtils.e(TAG, e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(mModel.getContext());
            dialog.setTitle(R.string.title_please_wait);
            dialog.setMessage(OResource.string(mModel.getContext(), R.string.title_searching));
            dialog.setCancelable(false);
            if (showDialog) {
                dialog.show();
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (listener != null) {
                listener.OnSuccessful(records);
            }
            dialog.dismiss();
        }
    }
}
