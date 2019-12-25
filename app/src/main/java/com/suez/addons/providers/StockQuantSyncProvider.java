package com.suez.addons.providers;

import com.odoo.core.orm.provider.BaseModelProvider;
import com.suez.addons.models.StockQuant;

/**
 * Created by joseph on 18-5-22.
 */

public class StockQuantSyncProvider extends BaseModelProvider {
    @Override
    public String authority() {
        return StockQuant.AUTHORITY;
    }
}
