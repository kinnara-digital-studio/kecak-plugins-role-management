package com.kinnara.kecakplugins.rolemanagement;

import org.joget.apps.app.dao.AppDefinitionDao;
import org.joget.apps.app.dao.DatalistDefinitionDao;
import org.joget.apps.app.dao.FormDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.DatalistDefinition;
import org.joget.apps.app.model.FormDefinition;
import org.joget.apps.app.model.PluginDefaultProperties;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.service.DataListService;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormService;
import org.joget.commons.util.LogUtil;
import org.joget.directory.model.Group;
import org.joget.directory.model.service.DirectoryManager;
import org.joget.plugin.property.service.PropertyUtil;
import org.joget.workflow.model.service.WorkflowManager;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utilities {
    public final static String MASTER_ROLE_GROUP_FORM_DEF_ID = "master_role_group";
    public final static String MASTER_ROLE_FORM_DEF_ID = "master_role";
    public final static String MASTER_AUTH_OBJECT_FORM_DEF_ID = "master_auth_obj";

    public final static int PERMISSION_NONE = 0;
    public final static int PERMISSION_READ = 1;
    public final static int PERMISSION_WRITE = 3;

    private final static Map<String, Form> formCache = new WeakHashMap<>();
    private final static Map<String, DataList> datalistCache = new WeakHashMap<>();

    public static Form generateForm(String formDefId) {
        return generateForm(AppUtil.getCurrentAppDefinition(), formDefId);
    }

    public static Form generateForm(String appId, String appVersion, String formDefId) {
        AppDefinitionDao appDefinitionDao = (AppDefinitionDao) AppUtil.getApplicationContext().getBean("appDefinitionDao");
        return generateForm(appDefinitionDao.loadVersion(appId, Long.valueOf(appVersion)), formDefId);
    }

    public static Form generateForm(AppDefinition appDef, String formDefId) {
        // check in cache
        if(formCache.containsKey(formDefId))
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
                formCache.put(formDefId, form);

                return form;
            }
        }
        return null;
    }

    public static DataList generateDataList(String dataListId) {
        return generateDataList(AppUtil.getCurrentAppDefinition(), dataListId);
    }

    public static DataList generateDataList(AppDefinition appDef, String dataListId) {
        ApplicationContext applicationContext     = AppUtil.getApplicationContext();

        if (datalistCache.containsKey(dataListId))
            return datalistCache.get(dataListId);

        DataListService dataListService = (DataListService) applicationContext.getBean("dataListService");
        DatalistDefinitionDao datalistDefinitionDao = (DatalistDefinitionDao) applicationContext.getBean("datalistDefinitionDao");
        DatalistDefinition datalistDefinition = datalistDefinitionDao.loadById(dataListId, appDef);
        if (datalistDefinition != null) {
            DataList dataList = dataListService.fromJson(datalistDefinition.getJson());
            dataList.setDefaultPageSize(DataList.MAXIMUM_PAGE_SIZE);
            datalistCache.put(dataListId, dataList);
            return dataList;
        }
        return null;
    }

    public static int getPermission(String currentUser, String authObject, String objectType) {
        try {
            ApplicationContext appContext = AppUtil.getApplicationContext();
            AppDefinitionDao appDefinitionDao = (AppDefinitionDao) appContext.getBean("appDefinitionDao");
            Collection<PluginDefaultProperties> pluginDefaultProperties = AppUtil.getCurrentAppDefinition().getPluginDefaultPropertiesList();
            FormDataDao formDataDao = (FormDataDao) appContext.getBean("formDataDao");

            AppDefinition appDef = appDefinitionDao.loadById("roleMgmt");
            Form formMasterAuthObject = Utilities.generateForm(appDef, Utilities.MASTER_AUTH_OBJECT_FORM_DEF_ID);
            Form formMasterRole = Utilities.generateForm(appDef, Utilities.MASTER_ROLE_FORM_DEF_ID);
            Form formMasterRoleGroup = Utilities.generateForm(appDef, Utilities.MASTER_ROLE_GROUP_FORM_DEF_ID);

            // get Role Management Configuration
            Map<String, Object> configurationProperties = pluginDefaultProperties.stream()
                    .filter(p -> p.getId().equals(RoleManagementConfiguration.class.getName()))
                    .findFirst()
                    .map(p -> PropertyUtil.getPropertiesValueFromJson(p.getPluginProperties()))
                    .orElse(new HashMap<>());

            boolean debugMode = "true".equals(configurationProperties.get("debugMode"));


            if(debugMode)
                LogUtil.info(Utilities.class.getName(), "============= Eximining auth. object [" + authObject + "] for user [" + currentUser + "]=============");

            // get Master Auth Object
            FormRow rowMasterAuthObject = formDataDao.load(formMasterAuthObject, authObject);
            if (rowMasterAuthObject == null || !objectType.equals(rowMasterAuthObject.getProperty("type"))) {
                if(debugMode)
                    LogUtil.warn(Utilities.class.getName(), "Field Authorization Object [" + authObject + "] not defined, grant WRITE access");
                return PERMISSION_WRITE;
            }

            DirectoryManager directoryManager = (DirectoryManager) appContext.getBean("directoryManager");
            final StringBuilder conditionMasterRoleGroup = new StringBuilder();
            final List<String> argumentsMasterRoleGroup = new ArrayList<>();
            conditionMasterRoleGroup.append(" AND e.customProperties.users LIKE '%'||?||'%'");
            argumentsMasterRoleGroup.add(currentUser);
            Collection<Group> groups = directoryManager.getGroupByUsername(currentUser);
            if (groups != null) {
                // filter by group
                groups.stream()
                        .map(Group::getName)
                        .peek(g -> {
                            if(debugMode)
                                LogUtil.info(Utilities.class.getName(), "User [" + currentUser + "] is found in Directory Group ["+ g +"]");
                        })
                        .forEach(name -> {
                            conditionMasterRoleGroup.append(" OR e.customProperties.groups LIKE '%'||?||'%'");
                            argumentsMasterRoleGroup.add(name);
                        });
            }
            conditionMasterRoleGroup.append(" OR e.customProperties.everyone = 'true'");

            // get Master Role Group
            Pattern roleGroupPattern = Pattern.compile(argumentsMasterRoleGroup.stream().map(s -> s.replace("\\", "\\\\")).collect(Collectors.joining("\\b|\\b", "\\b", "\\b")));
            FormRowSet rowSetMasterRoleGroup = formDataDao.find(formMasterRoleGroup, " WHERE 1 = 1 " + conditionMasterRoleGroup.toString(), argumentsMasterRoleGroup.toArray(), null, null, null, null)
                    .stream()
                    .filter(formRow -> "true".equals(formRow.getProperty("everyone"))
                            || (formRow.getProperty("groups") != null && roleGroupPattern.matcher(formRow.getProperty("groups")).find()))
                    .peek(rg -> {
                        if(debugMode) {
                            if("true".equals(rg.getProperty("everyone"))) {
                                LogUtil.info(Utilities.class.getName(), "Role Group [" + rg.getId() + "] if for everyone");
                            } else {
                                LogUtil.info(Utilities.class.getName(), "User [" + currentUser + "] is found in Role Group [" + rg.getId() + "]");
                            }


                        }
                    })
                    .collect(FormRowSet::new, FormRowSet::add, FormRowSet::addAll);

            String conditionsRoleId;
            if (rowSetMasterRoleGroup != null) {
                // create Master Role filter based on Master Role Group data
                conditionsRoleId =
                        rowSetMasterRoleGroup
                                .stream()
                                .flatMap(row -> Arrays.stream(row.getProperty("roles").split(";")))
                                .distinct()
                                .collect(Collectors.joining("', '", " AND id IN ('", "')"));
            } else {
                conditionsRoleId = "";
            }

            String conditionAuthObject = " AND e.customProperties.auth_object LIKE '%" + rowMasterAuthObject.getId() + "%'";

            // get Master Role
            FormRowSet rowSetMasterRole = formDataDao.find(formMasterRole, "WHERE 1 = 1 " + conditionsRoleId + conditionAuthObject, null, null, null, null, null);
            Pattern patternAuthObject = Pattern.compile("\\b" + rowMasterAuthObject.getId() + "\\b");
            int permission = rowSetMasterRole
                    .stream()
                    .filter(row -> patternAuthObject.matcher(row.getProperty("auth_object")).find())
                    .peek(r -> {
                        // print log in debug mode
                        if(debugMode)
                            LogUtil.info(Utilities.class.getName(), "Loading role [" + r.getId() + "]");
                    })
                    .map(r -> r.getProperty("permission"))
                    .peek(p -> {
                        if(debugMode)
                            LogUtil.info(Utilities.class.getName(), "Current role has [" + p + "] permission");
                    })
                    .map(p ->
                            "read".equals(p) ? Utilities.PERMISSION_READ
                                    : "write".equals(p) ? Utilities.PERMISSION_WRITE
                                    : Utilities.PERMISSION_NONE)
                    .reduce((p1, p2) -> p1 | p2)
                    .orElse(Utilities.PERMISSION_NONE);

            return permission;
        } catch (Exception e) {
            LogUtil.error(Utilities.class.getName(), e, "Error while retrieving permission, grant WRITE access");
            return Utilities.PERMISSION_WRITE;
        }
    }
}
