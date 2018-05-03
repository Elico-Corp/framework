package com.odoo.core.orm;

import android.content.Context;

import com.odoo.App;
import com.odoo.core.support.OUser;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Enumeration;
import java.util.HashMap;

import dalvik.system.DexFile;

public class ModelRegistryUtils {

    private HashMap<String, Class<? extends OModel>> models = new HashMap<>();

    public void makeReady(Context context) {
        try {
            // Avoid classes cannot be found in android studio caused by instant run
            File instantRunDir = new File(context.getFilesDir(), "instant-run/dex");
            if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.KITKAT_WATCH
                    && instantRunDir.exists()) {
                for (File dexPath : instantRunDir.listFiles()) {
                    DexFile dex = new DexFile(dexPath);
                    for (Enumeration<String> entries = dex.entries(); entries.hasMoreElements
                            (); ) {
                        String element = entries.nextElement();
                        if (element.startsWith(App.class.getPackage().getName())) {
                            Class<? extends OModel> clsName = (Class<? extends OModel>) Class
                                    .forName(element);
                            if (clsName != null && clsName.getSuperclass() != null &&
                                    OModel.class.isAssignableFrom(clsName.getSuperclass())) {
                                String modelName = getModelName(context, clsName);
                                if (modelName != null) {
                                    this.models.put(modelName, clsName);
                                }
                            }
                        }
                    }
                }
            }else {
                DexFile dexFile = new DexFile(context.getPackageCodePath());
                for (Enumeration<String> item = dexFile.entries(); item.hasMoreElements(); ) {
                    String element = item.nextElement();
                    if (element.startsWith(App.class.getPackage().getName())) {
                        Class<? extends OModel> clsName = (Class<? extends OModel>) Class.forName(element);
                        if (clsName != null && clsName.getSuperclass() != null &&
                                OModel.class.isAssignableFrom(clsName.getSuperclass())) {
                            String modelName = getModelName(context, clsName);
                            if (modelName != null) {
                                this.models.put(modelName, clsName);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getModelName(Context context, Class cls) {
        try {
            Constructor constructor = cls.getConstructor(Context.class, OUser.class);
            OModel model = (OModel) constructor.newInstance(context, null);
            return model.getModelName();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Class<? extends OModel> getModel(String modelName) {
        if (models.containsKey(modelName)) {
            return models.get(modelName);
        }
        return null;
    }

    public HashMap<String, Class<? extends OModel>> getModels() {
        return models;
    }
}
