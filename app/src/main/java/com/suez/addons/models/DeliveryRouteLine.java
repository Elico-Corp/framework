package com.suez.addons.models;

import android.content.Context;
import android.net.Uri;

import com.odoo.BuildConfig;
import com.odoo.R;
import com.odoo.base.addons.res.ResPartner;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.ODate;
import com.odoo.core.orm.fields.types.OFloat;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by joseph on 18-5-14.
 */

public class DeliveryRouteLine extends OModel {
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".core.provider.content.sync.delivery_route_line";
    private static final String TAG = DeliveryRouteLine.class.getSimpleName();

    OColumn name = new OColumn(getContext(), R.string.column_name, OVarchar.class).setSize(64);
    OColumn copy_no = new OColumn(getContext(), R.string.column_copy_no, OVarchar.class);
    OColumn customer_weight = new OColumn(getContext(), R.string.column_customer_weight, OFloat.class);
    OColumn weighing_weight = new OColumn(getContext(), R.string.column_weighing_weight, OFloat.class);
    OColumn total_packaging_quantity = new OColumn(getContext(), R.string.column_total_packaging_quantity, OFloat.class);
    OColumn internal_weight = new OColumn(getContext(), R.string.column_internal_weight, OFloat.class);
    OColumn deduction_weight = new OColumn(getContext(), R.string.column_deduction_weight, OFloat.class);
    OColumn invoice_weight = new OColumn(getContext(), R.string.column_invoice_weight, OFloat.class);
    OColumn wac_number = new OColumn(getContext(), R.string.column_wac_number, OVarchar.class).setSize(64);
    OColumn packaging = new OColumn(getContext(), R.string.column_packaging, OVarchar.class).setSize(256);
    OColumn wac_processing = new OColumn(getContext(), R.string.column_wac_processing, OVarchar.class);
    OColumn address_id = new OColumn(getContext(), R.string.column_partner_id, ResPartner.class, OColumn.RelationType.ManyToOne);
    OColumn route_id = new OColumn(getContext(), R.string.column_delivery_route, DeliveryRoute.class, OColumn.RelationType.ManyToOne);
    OColumn delivery_date = new OColumn(getContext(), R.string.column_delivery_date, ODate.class);
    OColumn pretreatment_id = new OColumn(getContext(), R.string.column_pretreatment_id, PretreatmentWac.class, OColumn.RelationType.ManyToOne);
    OColumn origin = new OColumn(getContext(), R.string.column_origin, OVarchar.class);
    OColumn order_line_weight = new OColumn(getContext(), R.string.column_order_line_weight, OFloat.class);
    OColumn wac_id = new OColumn(getContext(), R.string.column_wac_id, ProductWac.class, OColumn.RelationType.ManyToOne);
    OColumn hw_code = new OColumn(getContext(), R.string.column_hw_code, HwCode.class, OColumn.RelationType.ManyToOne);
    OColumn deviation_reasons_id = new OColumn(getContext(), R.string.column_deviation_reasons, DeliveryDeviationReason.class, OColumn.RelationType.ManyToOne);


    public DeliveryRouteLine(Context context, OUser user) {
        super(context, "delivery.route.line", user);
    }

    @Override
    public Uri uri() {
        return buildURI(AUTHORITY);
    }
}
