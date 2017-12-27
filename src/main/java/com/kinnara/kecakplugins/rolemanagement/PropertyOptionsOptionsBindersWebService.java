package com.kinnara.kecakplugins.rolemanagement;

import org.joget.apps.app.dao.AppDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.base.PluginWebSupport;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public class PropertyOptionsOptionsBindersWebService extends DefaultApplicationPlugin implements PluginWebSupport{
    @Override
    public String getName() {
        return "Options Binders Web Service";
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
    public Object execute(Map map) {
        return null;
    }

    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String method = request.getMethod();
        String objectType = request.getParameter("type");
        if(!"GET".equals(method)) {
            String message = "Only accept GET method";
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, message);
            LogUtil.warn(getClassName(), message);
            return;
        }

        ApplicationContext appContext = AppUtil.getApplicationContext();
        AppDefinitionDao appDefinitionDao = (AppDefinitionDao) appContext.getBean("appDefinitionDao");
        AppDefinition appDef = appDefinitionDao.loadById("roleMgmt");
        FormDataDao formDataDao = (FormDataDao)appContext.getBean("formDataDao");
        Form formAuthObject = Utilities.generateForm(appDef, Utilities.MASTER_AUTH_OBJECT_FORM_DEF_ID);

        FormRowSet rowSetAuthObject = formDataDao.find(formAuthObject, objectType == null ? null : "WHERE e.customProperties.type = ?", objectType == null ? null : new String[] {objectType}, null, null, null, null);
        JSONArray jsonResult = new JSONArray();
        rowSetAuthObject.stream()
                .map(FormRow::getId)
                .map(id -> {
                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("value", id);
                        jsonObject.put("label", id);
                        return jsonObject;
                    } catch (JSONException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .forEach(jsonResult::put);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.getWriter().write(jsonResult.toString());
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
        return null;
    }
}
