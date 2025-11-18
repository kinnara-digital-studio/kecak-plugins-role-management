package com.kinnarastudio.kecakplugins.rolemanagement;

import com.kinnarastudio.kecakplugins.rolemanagement.commons.CacheUtil;
import com.kinnarastudio.kecakplugins.rolemanagement.commons.Utilities;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DatalistPermission;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormPermission;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.service.FormUtil;
import org.joget.apps.userview.model.Permission;
import org.joget.apps.userview.model.UserviewAccessPermission;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.kecak.apps.userview.model.Platform;
import org.springframework.context.ApplicationContext;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author aristo
 */
public class RoleManagementPermission extends Permission implements FormPermission, UserviewAccessPermission, DatalistPermission {
    public final static String LABEL = "Role Management Permission";

    private static final Cache<String, Boolean> permissionCache = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .maximumSize(100)
        .build();

    // private final Map<String, Boolean> permissionCache = new HashMap<>();

    @Override
    public boolean isAuthorize() {
        ApplicationContext appContext = AppUtil.getApplicationContext();
        FormDataDao formDataDao = (FormDataDao) appContext.getBean("formDataDao");
        WorkflowManager wfManager = (WorkflowManager) appContext.getBean("workflowManager");
        WorkflowUserManager wfUserManager = wfManager.getWorkflowUserManager();

        String username = wfUserManager.getCurrentUsername();
        String authObject = getPropertyString("authObject");

        LogUtil.debug(getClassName(), "Auth Object: [" + authObject + "]");

        String cacheKey = CacheUtil.getCacheKey(getClass(), username, authObject);

        return Boolean.TRUE.equals(permissionCache.get(cacheKey, key -> {
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

            // final Form formMasterAuthObject = Utilities.generateForm(appDef, Utilities.MASTER_AUTH_OBJECT_FORM_DEF_ID);

            FormRow formRow = formDataDao.load(Utilities.MASTER_AUTH_OBJECT_FORM_DEF_ID, "master_role", authObject);

            final List<Element> fields = Optional.ofNullable(formRow)//Form, FormId -> FormId, Table ID, primaryKey
                    .map(r -> r.getProperty("object_name"))
                    .map(s -> s.split(";"))
                    .stream()
                    .flatMap(Arrays::stream)
                    .map(id -> FormUtil.findElement(id, currentForm, formData, true))
                    .collect(Collectors.toList());

            if (fields.isEmpty()) {
                Optional.ofNullable(element)
                        .map(Element::getChildren)
                        .stream()
                        .flatMap(Collection::stream)
                        .forEach(elementConsumer);
            } else {
                fields.forEach(elementConsumer);
            }

            boolean result = Utilities.getPermission(username, authObject, "menu", getPlatform() == Platform.MOBILE) != Utilities.PERMISSION_NONE;
            return result;
        }));
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
        return LABEL;
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        final String[] args = new String[]{
                PropertyOptionsOptionsBindersWebService.class.getName()
        };
        return AppUtil.readPluginResource(getClassName(), "/properties/RoleManagementPermission.json", args, false, "/messages/RoleManagement");
    }
}
