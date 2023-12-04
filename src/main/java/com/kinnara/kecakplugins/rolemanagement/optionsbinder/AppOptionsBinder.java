package com.kinnara.kecakplugins.rolemanagement.optionsbinder;

import org.joget.apps.app.dao.AppDefinitionDao;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.plugin.base.PluginManager;

import java.util.HashSet;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;

public class AppOptionsBinder extends FormBinder implements FormLoadOptionsBinder, FormAjaxOptionsBinder {
    public final static String LABEL = "Role Management Apps Options Binder";
    @Override
    public boolean useAjax() {
        return false;
    }

    @Override
    public FormRowSet loadAjaxOptions(String[] strings) {
        AppDefinitionDao appDefinitionDao = (AppDefinitionDao) AppUtil.getApplicationContext().getBean("appDefinitionDao");
        final Set<String> duplicate = new HashSet<>();
        return appDefinitionDao.findByVersion(null, null, null, null, null, null, null, null)
                .stream()
                .filter(Objects::nonNull)
                .filter(p -> duplicate.add(p.getAppId()))
                .collect(FormRowSet::new, (rs, p) -> {
                    FormRow row = new FormRow();
                    row.put(FormUtil.PROPERTY_VALUE, p.getAppId().trim());
                    row.put(FormUtil.PROPERTY_LABEL, p.getName() + " ("+p.getAppId()+")");
                    rs.add(row);
                }, FormRowSet::addAll);
    }

    @Override
    public FormRowSet load(Element element, String s, FormData formData) {
        setFormData(formData);
        return this.loadAjaxOptions(null);
    }

    @Override
    public String getName() {
        return LABEL;
    }

    @Override
    public String getVersion() {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        ResourceBundle resourceBundle = pluginManager.getPluginMessageBundle(getClassName(), "/messages/BuildNumber");
        String buildNumber = resourceBundle.getString("buildNumber");
        return buildNumber;
    }

    @Override
    public String getDescription() {
        return getClass().getPackage().getImplementationTitle();
    }

    @Override
    public String getLabel() {
        return LABEL;
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return "";
    }
}
