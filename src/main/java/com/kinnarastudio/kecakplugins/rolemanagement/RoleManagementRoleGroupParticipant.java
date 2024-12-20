package com.kinnarastudio.kecakplugins.rolemanagement;

import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.DefaultParticipantPlugin;

import java.util.Collection;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * @author aristo
 *
 * Automatically assign participant based on role group
 *
 */
public class RoleManagementRoleGroupParticipant extends DefaultParticipantPlugin {
    @Override
    public String getName() {
        return AppPluginUtil.getMessage("roleManagement.participant", getClassName(), "/messages/RoleManagement");
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
    public Collection<String> getActivityAssignments(Map map) {
        return Utilities.getUsersFromRoleGroup(getPropertyString("roleGroups"));
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
        return AppUtil.readPluginResource(getClassName(), "/properties/RoleManagementRoleGroupParticipant.json", new String[] {PropertyOptionsOptionsBindersWebService.class.getName()},  false, "/messages/RoleManagement");    }
}
