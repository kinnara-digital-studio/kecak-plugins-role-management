package com.kinnara.kecakplugins.rolemanagement;

import org.joget.apps.app.dao.AppDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.lib.WorkflowFormBinder;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.directory.model.Group;
import org.joget.directory.model.service.DirectoryManager;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RoleManagementFormLoadBinder extends WorkflowFormBinder {
    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        FormRowSet originalRowSet = super.load(element, primaryKey, formData);

        ApplicationContext appContext = AppUtil.getApplicationContext();
        AppDefinitionDao appDefinitionDao = (AppDefinitionDao) appContext.getBean("appDefinitionDao");
        WorkflowManager wfManager = (WorkflowManager)appContext.getBean("workflowManager");
        WorkflowUserManager wfUserManager = wfManager.getWorkflowUserManager();
        FormDataDao formDataDao = (FormDataDao)appContext.getBean("formDataDao");

        AppDefinition appDef = appDefinitionDao.loadById("roleMgmt");
        Form formMasterAuthObject = Utilities.generateForm(appDef, Utilities.MASTER_AUTH_OBJECT_FORM_DEF_ID);
        Form formMasterRole = Utilities.generateForm(appDef, Utilities.MASTER_ROLE_FORM_DEF_ID);
        Form formMasterRoleGroup = Utilities.generateForm(appDef, Utilities.MASTER_ROLE_GROUP_FORM_DEF_ID);

        String currentUser = wfUserManager.getCurrentUsername();

        DirectoryManager directoryManager = (DirectoryManager)appContext.getBean("directoryManager");
        Collection<Group> groups = directoryManager.getGroupByUsername(currentUser);

        String authObject = getPropertyString("authObject");

        // get Master Auth Object
        FormRow rowMasterAuthObject = formDataDao.load(formMasterAuthObject, authObject);
        if(rowMasterAuthObject == null || !"field".equals(rowMasterAuthObject.getProperty("type"))) {
            LogUtil.warn(getClassName(), "Field Authorization Object [" + authObject + "] not defined");
            return originalRowSet;
        }

        // get Master Role Group
        FormRowSet rowSetMasterRoleGroup = formDataDao.find(formMasterRoleGroup,  " WHERE e.customProperties.users LIKE '%'||?||'%'", new String[] {currentUser}, null, null, null, null);

        String conditionAuthObject;
        if(rowSetMasterRoleGroup != null) {
            conditionAuthObject = " e.customProperties.auth_object LIKE '%"+ rowMasterAuthObject.getId() + "%'";
        } else {
            conditionAuthObject = "";
        }

        // create Master Role filter based on Master Role Group data
        String conditionsRoleId =
                        rowSetMasterRoleGroup
                                .stream()
                                .flatMap(row -> Arrays.stream(row.getProperty("roles").split(";")))
                                .distinct()
                                .collect(Collectors.joining("', '", " id IN ('", "') AND"));

        // get Master Role
        FormRowSet rowSetMasterRole = formDataDao.find(formMasterRole, "WHERE 1 = 1 AND " + conditionsRoleId + conditionAuthObject, null, null, null, null, null);

        Pattern patternAuthObject = Pattern.compile("\\b"+ rowMasterAuthObject.getId() + "\\b");
        int permission = rowSetMasterRole
                .stream()
                .filter(row -> patternAuthObject.matcher(row.getProperty("auth_object")).find())
                .map(r -> r.getProperty("permission"))
                .map(p ->
                        "read".equals(p) ? Utilities.PERMISSION_READ
                        : "write".equals(p) ? Utilities.PERMISSION_WRITE
                        : Utilities.PERMISSION_NONE)
                .reduce((p1, p2) -> p1 | p2)
                .orElse(Utilities.PERMISSION_NONE);

        Arrays.stream(rowMasterAuthObject.getProperty("object_name").split(";"))
                .map(id -> FormUtil.findElement(id, element, formData))
                .filter(Objects::nonNull)
                .forEach(e -> {
                    if((permission & Utilities.PERMISSION_WRITE) != Utilities.PERMISSION_WRITE) {
                        FormUtil.setReadOnlyProperty(e);

                        if(permission == Utilities.PERMISSION_NONE) {
                            originalRowSet
                                    .stream()
//                                    .forEach(row -> row.setProperty(FormUtil.getElementParameterName(e), ""));
                                    .forEach(row -> row.remove(FormUtil.getElementParameterName(e)));
                        }
                    }
                });

        return originalRowSet;
    }

    @Override
    public String getName() {
        return getLabel();
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
    public String getLabel() {
        return "Role Management Form Binder";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/RoleManagementFormLoadBinder.json", new String[] {PropertyOptionsOptionsBindersWebService.class.getName()},  false, "/messages/RoleManagementFormLoadBinder");
    }
}
