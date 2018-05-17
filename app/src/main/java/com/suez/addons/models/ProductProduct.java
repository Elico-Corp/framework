package com.suez.addons.models;

import android.content.Context;
import android.net.Uri;

import com.odoo.BuildConfig;
import com.odoo.R;
import com.odoo.core.orm.OModel;
import com.odoo.core.orm.fields.OColumn;
import com.odoo.core.orm.fields.types.OVarchar;
import com.odoo.core.support.OUser;

/**
 * Created by joseph on 18-5-11.
 */

public class ProductProduct extends OModel {
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".core.provider.content.sync.product_product";
    private static final String TAG = ProductProduct.class.getSimpleName();

    OColumn name = new OColumn(getContext(), R.string.column_desc_en, OVarchar.class).setSize(64);
    OColumn name_local = new OColumn(getContext(), R.string.column_desc_local, OVarchar.class).setSize(64);

    public ProductProduct (Context context, OUser user) {
        super(context, "product.product", user);
    }

    @Override
    public Uri uri() {
        return buildURI(AUTHORITY);
    }
}
