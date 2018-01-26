package com.kinnara.kecakplugins.rolemanagement;

import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.property.model.PropertyEditable;

import java.util.Map;

public class RoleManagementConfiguration extends DefaultApplicationPlugin implements PropertyEditable {
    @Override
    public String getName() {
        return "Role Management Form Binder";
    }

    @Override
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public String getDescription() {
        return "Put this in Plugin Default Properties";
    }

    @Override
    public Object execute(Map map) {
        LogUtil.warn(getClassName(), "Not supposed to be implemented");
        return null;
    }

    @Override
    public String getLabel() {
        return getName();
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/RoleManagementConfiguration.json", new String[] {PropertyOptionsOptionsBindersWebService.class.getName()},  false, "/messages/RoleManagement");
    }
}
