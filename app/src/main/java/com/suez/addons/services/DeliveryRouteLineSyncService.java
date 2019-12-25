package com.suez.addons.services;

import android.content.Context;
import android.os.Bundle;

import com.odoo.core.service.OSyncAdapter;
import com.odoo.core.service.OSyncService;
import com.odoo.core.support.OUser;
import com.suez.addons.models.DeliveryRouteLine;

/**
 * Created by joseph on 18-5-22.
 */

public class DeliveryRouteLineSyncService extends OSyncService {

    @Override
    public OSyncAdapter getSyncAdapter(OSyncService service, Context context) {
        return new OSyncAdapter(context, DeliveryRouteLine.class, service, true);
    }

    @Override
    public void performDataSync(OSyncAdapter adapter, Bundle extras, OUser user) {
        adapter.syncDataLimit(500);
    }
}
