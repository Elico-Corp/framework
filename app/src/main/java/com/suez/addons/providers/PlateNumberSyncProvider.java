package com.suez.addons.providers;

import com.odoo.core.orm.provider.BaseModelProvider;
import com.suez.addons.models.PlateNumber;

/**
 * Created by joseph on 18-5-7.
 */

public class PlateNumberSyncProvider extends BaseModelProvider {
    public static final String TAG = PlateNumberSyncProvider.class.getSimpleName();

    @Override
    public String authority() {
        return PlateNumber.AUTHORITY;
    }
}
