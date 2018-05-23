package com.suez.addons.models;

import android.content.Context;
import android.net.Uri;

import com.odoo.BuildConfig;
import com.odoo.R;
import com.odoo.base.addons.res.ResPartner;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.annotation.Odoo;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by joseph on 18-5-13.
 */

public class ProductWac extends OModel {
    public static final String TAG = ProductWac.class.getSimpleName();
//    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".core.provider.content.sync.product_wac";

    OColumn wac_code = new OColumn(getContext(), R.string.column_wac_code, OVarchar.class).setSize(64);
    OColumn name = new OColumn(getContext(), R.string.column_name_en, OVarchar.class);
    OColumn name_local = new OColumn(getContext(), R.string.column_name_local, OVarchar.class);
    OColumn wac_version = new OColumn(getContext(), R.string.column_wac_version, OVarchar.class);
    OColumn partner_name_cn = new OColumn(getContext(), R.string.column_partner_name_cn, OVarchar.class);
    OColumn partner_id = new OColumn(getContext(), R.string.column_customer_name, ResPartner.class, OColumn.RelationType.ManyToOne);
    OColumn product_category_id = new OColumn(getContext(), R.string.column_waste_category, ProductCategory.class, OColumn.RelationType.ManyToOne);


    public ProductWac(Context context, OUser user) {
        super(context, "product.wac", user);
    }

//    @Override
//    public Uri uri() {
//        return buildURI(AUTHORITY);
//    }
}
