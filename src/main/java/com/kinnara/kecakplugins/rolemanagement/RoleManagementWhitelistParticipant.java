package com.kinnara.kecakplugins.rolemanagement;

import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.DefaultParticipantPlugin;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.model.service.WorkflowUserManager;

import java.util.*;

/**
 * @author aristo
 *
 * Used at start process participant mapping
 */
public class RoleManagementWhitelistParticipant extends DefaultParticipantPlugin {
    @Override
    public String getName() {
        return AppPluginUtil.getMessage("roleManagement.whitelist", getClassName(), "/messages/RoleManagement");
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
        WorkflowManager wfManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
        WorkflowUserManager wfUserManager = wfManager.getWorkflowUserManager();
        String currentUsername = wfUserManager.getCurrentUsername();
        List<String> assignments = new ArrayList<>();
        if(Utilities.getPermission(currentUsername, getPropertyString("authObject"), "action", false) == Utilities.PERMISSION_WRITE)
            assignments.add(currentUsername);
        return assignments;
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
        return AppUtil.readPluginResource(getClassName(), "/properties/RoleManagementWhitelistParticipant.json", new String[] {PropertyOptionsOptionsBindersWebService.class.getName()},  false, "/messages/RoleManagement");
    }
}
