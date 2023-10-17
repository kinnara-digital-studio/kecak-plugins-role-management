package com.kinnara.kecakplugins.rolemanagement;

import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.property.model.PropertyEditable;

import java.util.Map;
import java.util.ResourceBundle;

/**
 * @author aristo
 *
 * Common configuration for Role Management Application
 */
public class RoleManagementConfiguration extends DefaultApplicationPlugin implements PropertyEditable {
    @Override
    public String getName() {
        return AppPluginUtil.getMessage("roleManagement.configuration", getClassName(), "/messages/RoleManagement");
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
        return AppPluginUtil.getMessage("roleManagement.configuration.description", getClassName(), "/messages/RoleManagement");
    }

    @Override
    public Object execute(Map map) {
        try {
            throw new Exception("Not supposed to be implemented");
        } catch (Exception e) {
            LogUtil.error(getClassName(), e, e.getMessage());
        }

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
