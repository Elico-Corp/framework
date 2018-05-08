package com.suez.addons.providers;

import com.odoo.core.orm.provider.BaseModelProvider;
import com.suez.addons.models.DeliveryRoute;

/**
 * Created by joseph on 18-5-7.
 */

public class DeliveryRouteSyncProvider extends BaseModelProvider {
    public static final String TAG = DeliveryRouteSyncProvider.class.getSimpleName();

    @Override
    public String authority() {return DeliveryRoute.AUTHORITY;}
}
