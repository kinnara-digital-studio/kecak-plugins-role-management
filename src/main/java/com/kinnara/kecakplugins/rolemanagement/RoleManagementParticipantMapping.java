package com.kinnara.kecakplugins.rolemanagement;

import org.joget.apps.app.service.AppUtil;
import org.joget.workflow.model.DefaultParticipantPlugin;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.model.service.WorkflowUserManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class RoleManagementParticipantMapping extends DefaultParticipantPlugin{
    @Override
    public String getName() {
        return "Role Management Participant Mapping";
    }

    @Override
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
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
        if(Utilities.getPermission(currentUsername, getPropertyString("authObject")) == Utilities.PERMISSION_WRITE)
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
        return AppUtil.readPluginResource(getClassName(), "/properties/RoleManagementPermission.json", new String[] {PropertyOptionsOptionsBindersWebService.class.getName()},  false, "/messages/RoleManagementPermission");
    }
}