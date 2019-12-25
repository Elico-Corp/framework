package com.suez.addons.models;

import android.content.Context;
import android.net.Uri;

import com.odoo.BuildConfig;
import com.odoo.R;
import com.odoo.base.addons.res.ResPartner;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OBoolean;
import com.odoo.core.orm.fields.types.ODateTime;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;
import com.odoo.core.utils.OResource;
import com.odoo.core.utils.notification.ONotificationBuilder;

/**
 * Created by joseph on 18-5-11.
 */

public class StockProductionLot extends OModel {
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".core.provider.content.sync.stock_production_lot";
    private static final String TAG = StockProductionLot.class.getSimpleName();

    OColumn name = new OColumn(getContext(), R.string.column_serial_number, OVarchar.class).setSize(64);
    OColumn product_qty = new OColumn(getContext(), R.string.column_qty, OFloat.class);
    OColumn product_id = new OColumn(getContext(), R.string.column_product, ProductProduct.class, OColumn.RelationType.ManyToOne);
    OColumn delivery_route = new OColumn(getContext(), R.string.column_delivery_route, DeliveryRoute.class, OColumn.RelationType.ManyToOne);
    OColumn delivery_route_line = new OColumn(getContext(), R.string.column_delivery_route_line, DeliveryRouteLine.class, OColumn.RelationType.ManyToOne);
    OColumn customer_id = new OColumn(getContext(), R.string.column_customer_name, ResPartner.class, OColumn.RelationType.ManyToOne);
    OColumn pretreatment_id = new OColumn(getContext(), R.string.column_pretreatment_id, PretreatmentWac.class, OColumn.RelationType.ManyToOne);
    OColumn quant_ids = new OColumn(getContext(), R.string.column_quant_ids, StockQuant.class, OColumn.RelationType.OneToMany);
    OColumn is_finished = new OColumn("Is Finished", OBoolean.class).setDefaultValue(false);
    OColumn truck_in_date = new OColumn(getContext(), R.string.column_truck_in_date, OVarchar.class);
    OColumn wac_processing = new OColumn(getContext(), R.string.column_wac_processing, OVarchar.class);

    public StockProductionLot(Context context, OUser user) {
        super(context, "stock.production.lot", user);
    }

    @Override
    public Uri uri() {
        return buildURI(AUTHORITY);
    }
}
