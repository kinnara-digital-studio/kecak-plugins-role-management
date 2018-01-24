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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RoleManagementFormLoadBinder extends WorkflowFormBinder {
    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        FormRowSet originalRowSet = super.load(element, primaryKey, formData);

        ApplicationContext appContext = AppUtil.getApplicationContext();
        AppDefinitionDao appDefinitionDao = (AppDefinitionDao) appContext.getBean("appDefinitionDao");
        AppDefinition appDef = appDefinitionDao.loadById("roleMgmt");
        FormDataDao formDataDao = (FormDataDao)appContext.getBean("formDataDao");
        WorkflowManager wfManager = (WorkflowManager)appContext.getBean("workflowManager");
        WorkflowUserManager wfUserManager = wfManager.getWorkflowUserManager();

        String authObject = getPropertyString("authObject");

        int permission = Utilities.getPermission(wfUserManager.getCurrentUsername(), authObject, "field");

        // process the element based on permission
        Consumer<Element> elementConsumer = e -> {
            if (permission != Utilities.PERMISSION_WRITE) {
                // if don't have write access, set as readonly
                FormUtil.setReadOnlyProperty(e);

                if (permission == Utilities.PERMISSION_NONE) {
                    // if don't have any access, remove value
                    originalRowSet
                            .forEach(row -> row.remove(FormUtil.getElementParameterName(e)));
                }
            }
        };

        Form formMasterAuthObject = Utilities.generateForm(appDef, Utilities.MASTER_AUTH_OBJECT_FORM_DEF_ID);
        FormRow rowMasterAuthObject = formDataDao.load(formMasterAuthObject, authObject);
        String propertyObjectName = rowMasterAuthObject.getProperty("object_name");
        if(propertyObjectName != null && !propertyObjectName.isEmpty()) {
            Arrays.stream(propertyObjectName.split(";"))
                    .map(id -> FormUtil.findElement(id, element, formData))
                    .filter(Objects::nonNull)
                    .forEach(elementConsumer);
        } else {
            element.getChildren(formData)
                    .forEach(elementConsumer);
        }

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
        return AppUtil.readPluginResource(getClassName(), "/properties/RoleManagementFormLoadBinder.json", new String[] {PropertyOptionsOptionsBindersWebService.class.getName()},  false, "/messages/RoleManagement");
    }
}
