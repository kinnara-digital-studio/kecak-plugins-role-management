package com.kinnara.kecakplugins.rolemanagement;

import org.joget.apps.app.dao.AppDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormPermission;
import org.joget.apps.form.service.FormUtil;
import org.joget.apps.userview.model.UserviewPermission;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author aristo
 *
 *
 */
public class RoleManagementPermission extends UserviewPermission implements FormPermission {
    @Override
    public boolean isAuthorize() {
        WorkflowManager wfManager = (WorkflowManager)AppUtil.getApplicationContext().getBean("workflowManager");
        WorkflowUserManager wfUserManager = wfManager.getWorkflowUserManager();
        ApplicationContext appContext = AppUtil.getApplicationContext();
        AppDefinitionDao appDefinitionDao = (AppDefinitionDao) appContext.getBean("appDefinitionDao");
        AppDefinition appDef = appDefinitionDao.loadById("roleMgmt");
        FormDataDao formDataDao = (FormDataDao)appContext.getBean("formDataDao");

        String authObject = getPropertyString("authObject");

        final int permission = Utilities.getPermission(wfUserManager.getCurrentUsername(), authObject, "menu", Utilities.isMobile(getFormData()));
        final FormData formData = getFormData();
        final Element element = getElement();
        final Form currentForm = FormUtil.findRootForm(element) == null && element instanceof Form ? (Form) element : FormUtil.findRootForm(element);

        // process the element based on permission
        Consumer<Element> elementConsumer = e -> {
            if (permission != Utilities.PERMISSION_WRITE) {
                // if don't have write access, set as readonly
                FormUtil.setReadOnlyProperty(e);

                if (permission == Utilities.PERMISSION_NONE) {
                    // if don't have any access, remove value
                    formData.getRequestParams().remove(FormUtil.getElementParameterName(e));
                }
            }
        };

        Form formMasterAuthObject = Utilities.generateForm(appDef, Utilities.MASTER_AUTH_OBJECT_FORM_DEF_ID);
        List<Element> fields = Optional.ofNullable(formMasterAuthObject)
                .map(f -> formDataDao.load(f, authObject))
                .map(r -> r.getProperty("object_name"))
                .map(s -> s.split(";"))
                .map(Arrays::stream)
                .orElse(Stream.empty())
                .map(id -> FormUtil.findElement(id, currentForm, formData, true))
                .collect(Collectors.toList());

        if(fields.isEmpty()) {
            Optional.ofNullable(element).map(Element::getChildren).map(Collection::stream).orElse(Stream.empty()).forEach(elementConsumer);
        } else {
            fields.forEach(elementConsumer);
        }

        return Utilities.getPermission(wfUserManager.getCurrentUsername(), getPropertyString("authObject"), "menu", Utilities.isMobile(getFormData())) != Utilities.PERMISSION_NONE;
    }

    @Override
    public String getName() {
        return AppPluginUtil.getMessage("roleManagement.permission", getClassName(), "/messages/RoleManagement");
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
        return getName();
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/RoleManagementPermission.json", new String[] {PropertyOptionsOptionsBindersWebService.class.getName()},  false, "/messages/RoleManagement");
    }
}
