package com.kinnarastudio.kecakplugins.rolemanagement;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import org.joget.apps.app.dao.AppDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.kecak.apps.exception.ApiException;
import org.springframework.context.ApplicationContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author aristo
 * <p>
 * Options binder for Role Management data
 */
public class PropertyOptionsOptionsBindersWebService extends DefaultApplicationPlugin implements PluginWebSupport {
    public final static String LABEL = "Options Binders Web Service";

    @Override
    public String getName() {
        return LABEL;
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
    public Object execute(Map map) {
        return null;
    }

    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LogUtil.info(getClass().getName(), "Executing Rest API [" + request.getRequestURI() + "] in method [" + request.getMethod() + "] contentType [" + request.getContentType() + "] as [" + WorkflowUtil.getCurrentUsername() + "]");

        try {
            final String method = request.getMethod();
            final String formDefId = Utilities.getParameter(request, "formDefId");
            if (!"GET".equals(method)) {
                throw new ApiException(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Only accept GET method");
            }

            final AppDefinition currentAppDefinition = AppUtil.getCurrentAppDefinition();
            final ApplicationContext appContext = AppUtil.getApplicationContext();
            final AppDefinitionDao appDefinitionDao = (AppDefinitionDao) appContext.getBean("appDefinitionDao");
            final AppDefinition appDefRoleManagement = appDefinitionDao.loadById("roleMgmt");
            final FormDataDao formDataDao = (FormDataDao) appContext.getBean("formDataDao");

            final Form form = Utilities.generateForm(appDefRoleManagement, formDefId);
            if (form == null) {
                throw new ApiException(HttpServletResponse.SC_NOT_FOUND, "Form not found");
            }

            final Pattern ignoredParameters = Pattern.compile("appId|appVersion|formDefId|pluginName|_");
            final StringBuilder query = new StringBuilder("WHERE (e.customProperties.app_id in (?, '') or e.customProperties.app_id is null)");
            final List<String> arguments = new ArrayList<>();
            arguments.add(currentAppDefinition.getAppId());

            ((Map<String, String[]>) request.getParameterMap()).entrySet()
                    .stream()
                    .filter(e -> !ignoredParameters.matcher(e.getKey()).find())
                    .forEach(e -> {
                        query.append(" AND e.customProperties.")
                                .append(e.getKey()).append(" IN ")
                                .append(Arrays.stream(e.getValue())
                                        .peek(arguments::add)
                                        .map(s -> "?")
                                        .collect(Collectors.joining(",", "(", ")")));
                    });

            final FormRowSet rowSetAuthObject = formDataDao.find(form, query.toString(), arguments.toArray(new String[0]), null, null, null, null);
            final JSONArray jsonResult = Optional.ofNullable(rowSetAuthObject)
                    .stream()
                    .flatMap(Collection::stream)
                    .map(FormRow::getId)
                    .map(Try.onFunction(s -> {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("value", s);
                        jsonObject.put("label", s);
                        return jsonObject;
                    }))
                    .collect(JSONCollectors.toJSONArray());

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.getWriter().write(jsonResult.toString());
        } catch (ApiException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
            response.sendError(e.getErrorCode(), e.getMessage());
        }
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
        return null;
    }
}
