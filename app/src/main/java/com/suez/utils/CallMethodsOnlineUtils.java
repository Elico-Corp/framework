package com.suez.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.odoo.BaseAbstractListener;
import com.odoo.R;
import com.odoo.core.orm.OModel;
import com.odoo.core.rpc.helper.OArguments;
import com.odoo.core.utils.OResource;

import java.util.HashMap;

/**
 * Created by joseph on 18-5-11.
 */

public class CallMethodsOnlineUtils {
    private static final String TAG = CallMethodsOnlineUtils.class.getSimpleName();
    private Context mContext;
    private OModel mModel;
    private String method;
    private OArguments args;
    private HashMap<String, Object> context;
    private HashMap<String, Object> kwargs;
    private BaseAbstractListener listener;

    public CallMethodsOnlineUtils(OModel model, String method, OArguments args, HashMap<String, Object> context, HashMap<String, Object> kwargs) {
        this.mContext = model.getContext();
        this.mModel = model;
        this.method = method;
        this.args = args;
        this.context = context;
        this.kwargs = kwargs;
    }

    public CallMethodsOnlineUtils(OModel model, String method, OArguments args, HashMap<String, Object> context) {
        this(model, method, args, context, null);
    }

    public CallMethodsOnlineUtils(OModel model, String method, OArguments args) {
        this(model, method, args, null);
    }

    public CallMethodsOnlineUtils setListener(BaseAbstractListener listener) {
        this.listener = listener;
        return this;
    }

    public void callMethodOnServer() {
        CallMethodTask task = new CallMethodTask();
        task.execute();
    }

    public class CallMethodTask extends AsyncTask<Void, Void, Object> {
        private ProgressDialog dialog;

        @Override
        protected Object doInBackground(Void... voids) {
            try {
                return mModel.getServerDataHelper().callMethod(method, args, context, kwargs);
            } catch (Exception e) {
                e.printStackTrace();
                LogUtils.e(TAG, e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(mContext);
            dialog.setTitle(R.string.title_please_wait);
            dialog.setMessage(OResource.string(mContext, R.string.title_searching));
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            if (listener != null) {
                listener.OnSuccessful(o);
            }
            dialog.dismiss();
        }
    }
}
