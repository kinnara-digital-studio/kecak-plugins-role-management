package com.kinnarastudio.kecakplugins.rolemanagement;

import org.joget.apps.app.dao.AppDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DatalistPermission;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormPermission;
import org.joget.apps.form.service.FormUtil;
import org.joget.apps.userview.model.Permission;
import org.joget.apps.userview.model.UserviewAccessPermission;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.kecak.apps.userview.model.Platform;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author aristo
 *
 *
 */
public class RoleManagementPermission extends Permission implements FormPermission, UserviewAccessPermission, DatalistPermission {
    @Override
    public boolean isAuthorize() {
        WorkflowManager wfManager = (WorkflowManager)AppUtil.getApplicationContext().getBean("workflowManager");
        WorkflowUserManager wfUserManager = wfManager.getWorkflowUserManager();
        ApplicationContext appContext = AppUtil.getApplicationContext();
        AppDefinitionDao appDefinitionDao = (AppDefinitionDao) appContext.getBean("appDefinitionDao");
        AppDefinition appDef = appDefinitionDao.loadById("roleMgmt");
        FormDataDao formDataDao = (FormDataDao)appContext.getBean("formDataDao");

        String authObject = getPropertyString("authObject");

        final int permission = Utilities.getPermission(wfUserManager.getCurrentUsername(), authObject, "menu", getPlatform() == Platform.MOBILE);
        final FormData formData = getFormData();
        final Element element = getElement();
        final Form currentForm = FormUtil.findRootForm(element) == null && element instanceof Form ? (Form) element : FormUtil.findRootForm(element);

        // process the element based on permission
        final Consumer<Element> elementConsumer = e -> {
            if (permission != Utilities.PERMISSION_WRITE) {
                // if don't have write access, set as readonly
                FormUtil.setReadOnlyProperty(e);

                if (permission == Utilities.PERMISSION_NONE) {
                    // if don't have any access, remove value
                    formData.getRequestParams().remove(FormUtil.getElementParameterName(e));
                }
            }
        };

        final Form formMasterAuthObject = Utilities.generateForm(appDef, Utilities.MASTER_AUTH_OBJECT_FORM_DEF_ID);
        final List<Element> fields = Optional.ofNullable(formMasterAuthObject)
                .map(f -> formDataDao.load(f, authObject))
                .map(r -> r.getProperty("object_name"))
                .map(s -> s.split(";"))
                .map(Arrays::stream)
                .orElse(Stream.empty())
                .map(id -> FormUtil.findElement(id, currentForm, formData, true))
                .collect(Collectors.toList());

        if(fields.isEmpty()) {
            Optional.ofNullable(element)
                    .map(Element::getChildren)
                    .map(Collection::stream)
                    .orElseGet(Stream::empty)
                    .forEach(elementConsumer);
        } else {
            fields.forEach(elementConsumer);
        }

        return Utilities.getPermission(wfUserManager.getCurrentUsername(), getPropertyString("authObject"), "menu", getPlatform() == Platform.MOBILE) != Utilities.PERMISSION_NONE;
    }

    @Override
    public String getName() {
        return AppPluginUtil.getMessage("roleManagement.permission", getClassName(), "/messages/RoleManagement");
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
        return getName();
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        final String[] args = new String[] {
                AppUtil.getCurrentAppDefinition().getAppId(),
                PropertyOptionsOptionsBindersWebService.class.getName()
        };
        return AppUtil.readPluginResource(getClassName(), "/properties/RoleManagementPermission.json", args,  false, "/messages/RoleManagement");
    }
}
