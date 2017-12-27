package com.kinnara.kecakplugins.rolemanagement;

import org.joget.apps.app.dao.AppDefinitionDao;
import org.joget.apps.app.dao.FormDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.FormDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.service.FormService;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;

public class Utilities {
    public final static String MASTER_ROLE_GROUP_FORM_DEF_ID = "master_role_group";
    public final static String MASTER_ROLE_FORM_DEF_ID = "master_role";
    public final static String MASTER_AUTH_OBJECT_FORM_DEF_ID = "master_auth_obj";

    public final static int PERMISSION_NONE = 0;
    public final static int PERMISSION_READ = 1;
    public final static int PERMISSION_WRITE = 3;

    private final static Map<String, Form> formCache = new HashMap<String, Form>();

    public static Form generateForm(String formDefId) {
        return generateForm(AppUtil.getCurrentAppDefinition(), formDefId);
    }

    public static Form generateForm(String appId, String appVersion, String formDefId) {
        AppDefinitionDao appDefinitionDao = (AppDefinitionDao) AppUtil.getApplicationContext().getBean("appDefinitionDao");
        return generateForm(appDefinitionDao.loadVersion(appId, Long.valueOf(appVersion)), formDefId);
    }

    public static Form generateForm(AppDefinition appDef, String formDefId) {
        // check in cache
        if(formCache != null && formCache.containsKey(formDefId))
            return formCache.get(formDefId);

        // proceed without cache
        ApplicationContext appContext = AppUtil.getApplicationContext();
        FormService formService = (FormService) appContext.getBean("formService");


        if (appDef != null && formDefId != null && !formDefId.isEmpty()) {
            FormDefinitionDao formDefinitionDao =
                    (FormDefinitionDao) AppUtil.getApplicationContext().getBean("formDefinitionDao");

            FormDefinition formDef = formDefinitionDao.loadById(formDefId, appDef);
            if (formDef != null) {
                String json = formDef.getJson();
                Form form = (Form)formService.createElementFromJson(json);

                // put in cache if possible
                if(formCache != null)
                    formCache.put(formDefId, form);

                return form;
            }
        }
        return null;
    }
}
