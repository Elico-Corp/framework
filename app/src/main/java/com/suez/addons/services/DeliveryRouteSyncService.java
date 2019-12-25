package com.suez.addons.services;

import android.content.Context;
import android.os.Bundle;

import com.odoo.core.service.OSyncAdapter;
import com.odoo.core.service.OSyncService;
import com.odoo.core.support.OUser;
import com.suez.addons.models.DeliveryRoute;

/**
 * Created by joseph on 18-5-7.
 */

public class DeliveryRouteSyncService extends OSyncService {
    public static final String TAG = DeliveryRouteSyncService.class.getSimpleName();

    @Override
    public OSyncAdapter getSyncAdapter(OSyncService service, Context context) {
        return new OSyncAdapter(context, DeliveryRoute.class, service, true);
    }

    @Override
    public void performDataSync(OSyncAdapter adapter, Bundle extras, OUser user) {
        adapter.syncDataLimit(500);
    }
}
