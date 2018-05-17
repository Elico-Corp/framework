package com.odoo;

import com.odoo.core.orm.ODataRow;
import com.odoo.core.rpc.helper.utils.gson.OdooResult;

import java.util.HashMap;
import java.util.List;

/**
 * Created by joseph on 18-5-2.
 */

public class BaseAbstractListener {
    public void OnSuccessful(HashMap hashMap) {

    }
    public void OnSuccessful(List<ODataRow> listRow) {
    }

    public void OnSuccessful(ODataRow row) {
    }

    public void OnSuccessful(OdooResult result) {
    }

    public void OnSuccessful(Boolean success) {
    }

    public void OnSuccessful(Integer count) {
    }

    public void OnSuccessful(String str) {
    }

    public void OnFail(String str) {

    }

    public void OnFail(int i) {

    }

    public void OnCancelled() {
    }
}
