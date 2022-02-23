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
import org.joget.directory.model.User;
import org.joget.directory.model.service.DirectoryManager;
import org.joget.plugin.property.service.PropertyUtil;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nullable;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utilities {
    public final static String APP_ID = "roleMgmt";
    public final static String MASTER_ROLE_GROUP_FORM_DEF_ID = "master_role_group";
    public final static String MASTER_ROLE_FORM_DEF_ID = "master_role";
    public final static String MASTER_AUTH_OBJECT_FORM_DEF_ID = "master_auth_obj";

    public final static int PERMISSION_NONE = 0;
    public final static int PERMISSION_READ = 1;
    public final static int PERMISSION_WRITE = 3;

    public final static int PLATFORM_WEB = 1;
    public final static int PLATFORM_MOBILE_APP = 2;
    public final static int PLATFORM_BOTH = 3;
    public final static int PLATFORM_NONE = 0;

//    public final static String PARAMETER_NAME_MOBILE = "_mobile";

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
//        if(formCache.containsKey(formDefId))
//            return formCache.get(formDefId);

        // proceed without cache
        ApplicationContext appContext = AppUtil.getApplicationContext();
        FormService formService = (FormService) appContext.getBean("formService");


        if (appDef != null && formDefId != null && !formDefId.isEmpty()) {
            FormDefinitionDao formDefinitionDao =
                    (FormDefinitionDao) AppUtil.getApplicationContext().getBean("formDefinitionDao");

            FormDefinition formDef = formDefinitionDao.loadById(formDefId, appDef);
            if (formDef != null) {
                String json = formDef.getJson();
                Form form = (Form) formService.createElementFromJson(json);

                // put in cache if possible
//                formCache.put(formDefId, form);

                return form;
            }
        }
        return null;
    }

    public static DataList generateDataList(String dataListId) {
        return generateDataList(AppUtil.getCurrentAppDefinition(), dataListId);
    }

    public static DataList generateDataList(AppDefinition appDef, String dataListId) {
        ApplicationContext applicationContext = AppUtil.getApplicationContext();

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
        return getPermission(currentUser, authObject, objectType, null);
    }

    public static int getPermission(String currentUser, String authObject, String objectType, @Nullable Boolean isMobile) {
        try {
            ApplicationContext appContext = AppUtil.getApplicationContext();
            AppDefinitionDao appDefinitionDao = (AppDefinitionDao) appContext.getBean("appDefinitionDao");
            FormDataDao formDataDao = (FormDataDao) appContext.getBean("formDataDao");

            AppDefinition appDef = appDefinitionDao.loadById("roleMgmt");
            Form formMasterAuthObject = Utilities.generateForm(appDef, Utilities.MASTER_AUTH_OBJECT_FORM_DEF_ID);
            Form formMasterRole = Utilities.generateForm(appDef, Utilities.MASTER_ROLE_FORM_DEF_ID);
            Form formMasterRoleGroup = Utilities.generateForm(appDef, Utilities.MASTER_ROLE_GROUP_FORM_DEF_ID);

            boolean debugMode = debugMode();

            if (debugMode)
                LogUtil.info(Utilities.class.getName(), "============= Eximining auth. object [" + authObject + "] for user [" + currentUser + "] platform mobile [" + isMobile + "]=============");

            // get Master Auth Object
            final FormRow rowMasterAuthObject = formDataDao.load(formMasterAuthObject, authObject);
            if (rowMasterAuthObject == null || !objectType.equals(rowMasterAuthObject.getProperty("type"))) {
                if (debugMode)
                    LogUtil.warn(Utilities.class.getName(), "Field Authorization Object [" + authObject + "] not defined, grant WRITE access");
                return PERMISSION_WRITE;
            }

            final DirectoryManager directoryManager = (DirectoryManager) appContext.getBean("directoryManager");

            final StringBuilder conditionMasterRoleGroup = new StringBuilder();
            final List<String> argumentsMasterRoleGroup = new ArrayList<>();

            conditionMasterRoleGroup.append(" AND e.customProperties.users LIKE '%'||?||'%'");
            argumentsMasterRoleGroup.add(currentUser);

            final Set<String> dirUserGroups = Optional.of(currentUser)
                    .map(directoryManager::getGroupByUsername)
                    .map(Collection::stream)
                    .orElseGet(Stream::empty)
                    .map(Group::getId)
                    .collect(Collectors.toSet());

            dirUserGroups.forEach(s -> {
                conditionMasterRoleGroup.append(" OR e.customProperties.groups LIKE '%'||?||'%'");
                argumentsMasterRoleGroup.add(s);
            });

            conditionMasterRoleGroup.append(" OR e.customProperties.everyone in ('true', 'loggedIn')");

            final Pattern roleGroupPattern = Pattern.compile(Stream.of(currentUser).map(s -> s.replace("\\", "\\\\")).collect(Collectors.joining("\\b|\\b", "\\b", "\\b")));
            final Pattern dirGroupPattern = Pattern.compile(dirUserGroups.stream().map(s -> s.replace("\\", "\\\\")).collect(Collectors.joining("\\b|\\b", "\\b", "\\b")));
            final boolean isCurrentUserAnonymous = WorkflowUtil.isCurrentUserAnonymous();

            final FormRowSet rowSetMasterRoleGroup = Optional.ofNullable(formDataDao.find(formMasterRoleGroup, " WHERE 1 = 1 " + conditionMasterRoleGroup, argumentsMasterRoleGroup.toArray(), null, null, null, null))
                    .map(Collection::stream)
                    .orElseGet(Stream::empty)
                    .filter(row -> !row.getDeleted())
                    .filter(row -> {
                        final String everyOne = row.getProperty("everyone", "");
                        final String users = row.getProperty("users", "");
                        final String groups = row.getProperty("groups");

                        return "true".equals(everyOne)
                                || ("loggedIn".equals(everyOne) && !isCurrentUserAnonymous)
                                || roleGroupPattern.matcher(users).find()
                                || dirGroupPattern.matcher(groups).find();
                    })
                    .peek(row -> {
                        if (debugMode) {
                            final String everyone = row.getProperty("everyone");
                            if ("true".equals(everyone) || "loggedIn".equals(everyone)) {
                                LogUtil.info(Utilities.class.getName(), "Role Group [" + row.getId() + "] if for everyone [" + everyone + "]");
                            } else {
                                LogUtil.info(Utilities.class.getName(), "User [" + currentUser + "] is found in Role Group [" + row.getId() + "]");
                            }
                        }
                    })
                    .collect(FormRowSet::new, FormRowSet::add, FormRowSet::addAll);

            // create Master Role filter based on Master Role Group data
            final Set<String> roles = rowSetMasterRoleGroup.stream()
                    .map(r -> r.getProperty("roles"))
                    .filter(Objects::nonNull)
                    .map(s -> s.split(";"))
                    .flatMap(Arrays::stream)
                    .collect(Collectors.toSet());

            // get Master Role
            final FormRowSet rowSetMasterRole = getMasterRole(formMasterRole, getCompleteRoles(formMasterRole, roles), rowMasterAuthObject.getId());

            return rowSetMasterRole
                    .stream()
                    .map(r -> {
                        final String permission = r.getProperty("permission");
                        final String platform = r.getProperty("platform");

                        if (debugMode) {
                            LogUtil.info(Utilities.class.getName(), "Role [" + r.getId() + "] has [" + permission + "] permission in platform [" + platform + "]");
                        }

                        if (isMobile != null && isMobile && !platform.contains("mobileapp")) {
                            return Utilities.PERMISSION_NONE;
                        }

                        if (isMobile != null && !isMobile && !platform.contains("web")) {
                            return Utilities.PERMISSION_NONE;
                        }

                        if ("read".equals(permission)) {
                            return Utilities.PERMISSION_READ;
                        } else if ("write".equals(permission)) {
                            return Utilities.PERMISSION_WRITE;
                        } else {
                            return Utilities.PERMISSION_NONE;
                        }
                    })
                    .reduce((p1, p2) -> p1 | p2)
                    .orElse(Utilities.PERMISSION_NONE);
        } catch (Exception e) {
            LogUtil.error(Utilities.class.getName(), e, "Error while retrieving permission, grant WRITE access");
            return Utilities.PERMISSION_WRITE;
        }
    }

    static public List<String> getUsersFromRoleGroup(String roleGroups) {
        final ApplicationContext appContext = AppUtil.getApplicationContext();
        final AppDefinitionDao appDefinitionDao = (AppDefinitionDao) appContext.getBean("appDefinitionDao");
        final DirectoryManager directoryManager = (DirectoryManager) appContext.getBean("directoryManager");
        final FormDataDao formDataDao = (FormDataDao) appContext.getBean("formDataDao");
        final AppDefinition appDef = appDefinitionDao.loadById(APP_ID);
        final Form formMasterRoleGroup = Utilities.generateForm(appDef, Utilities.MASTER_ROLE_GROUP_FORM_DEF_ID);

        return Optional.ofNullable(roleGroups)
                .map(s -> s.split(";"))
                .map(Arrays::stream)
                .orElse(Stream.empty())
                .filter(s -> !s.isEmpty())
                .map(s -> formDataDao.load(formMasterRoleGroup, s))
                .filter(Objects::nonNull)
                .filter(row -> !"true".equalsIgnoreCase(row.getProperty("everyone")))
                .flatMap(row -> Stream.concat(
                        Arrays.stream(row.getProperty("users").split(";"))
                                .filter(s -> !s.isEmpty())
                                .map(directoryManager::getUserById)
                                .filter(Objects::nonNull)
                                .map(User::getUsername),

                        Arrays.stream(row.getProperty("groups").split(";"))
                                .map(directoryManager::getUserByGroupName)
                                .flatMap(Collection::stream)
                                .map(User::getUsername)
                ))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private static String getConfigurationString(String propertyName) {
        final ApplicationContext appContext = AppUtil.getApplicationContext();
        final AppDefinitionDao appDefinitionDao = (AppDefinitionDao) appContext.getBean("appDefinitionDao");
        final AppDefinition roleManagementAppDef = appDefinitionDao.loadById(APP_ID);
        final AppDefinition currentAppDef = AppUtil.getCurrentAppDefinition();

        // get Role Management Configuration
        final Map<String, Object> configurationProperties = Stream.of(currentAppDef, roleManagementAppDef)
                .filter(Objects::nonNull)
                .map(AppDefinition::getPluginDefaultPropertiesList)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(p -> p != null
                        && p.getId() != null
                        && RoleManagementConfiguration.class.getName().equals(p.getId()))
                .findFirst()
                .map(PluginDefaultProperties::getPluginProperties)
                .map(PropertyUtil::getPropertiesValueFromJson)
                .orElseGet(HashMap::new);

        return String.valueOf(configurationProperties.get(propertyName));
    }

    public static boolean debugMode() {
        return "true".equalsIgnoreCase(getConfigurationString("debugMode"));
    }

    protected static Set<String> getCompleteRoles(final Form formMasterRole, final Set<String> roleIds) {
        final Set<String> results = new HashSet<>(roleIds);
        final FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        final String condition;
        final String[] arguments;
        if (roleIds.isEmpty()) {
            condition = "where 1 = 1";
            arguments = null;
        } else {
            condition = results.stream().map(s -> "?").collect(Collectors.joining(",", "where id in (", ")"));
            arguments = results.toArray(new String[0]);
        }

        final Set<String> includeRoleIds = Optional.ofNullable(formDataDao.find(formMasterRole, condition, arguments, null, null, null, null))
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(r -> r.getProperty("include_roles"))
                .filter(Objects::nonNull)
                .map(s -> s.split(";"))
                .flatMap(Arrays::stream)
                .filter(s -> !results.contains(s))
                .collect(Collectors.toSet());

        if (!includeRoleIds.isEmpty()) {
            results.addAll(getCompleteRoles(formMasterRole, includeRoleIds));
        }

        return results;
    }

    protected static FormRowSet getMasterRole(Form formMasterRole, Set<String> roleIds, String authObjectId) {
        final FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");

        final String conditionAuthObject = " AND e.customProperties.auth_object LIKE '%" + authObjectId + "%'";

        final String conditionsRoleId;
        if (roleIds.isEmpty()) {
            conditionsRoleId = " and 1 <> 1";
        } else {
            conditionsRoleId = roleIds
                    .stream()
                    .collect(Collectors.joining("', '", " and id in ('", "')"));
        }

        return Optional.ofNullable(formDataDao.find(formMasterRole, "where 1 = 1 " + conditionsRoleId + conditionAuthObject, null, null, null, null, null))
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .filter(row -> isContainingAuthObject(authObjectId, row))
                .collect(FormRowSet::new, FormRowSet::add, FormRowSet::addAll);
    }

    protected static boolean isContainingAuthObject(String authObjectId, FormRow rowRole) {
        final Pattern patternAuthObject = Pattern.compile("\\b" + authObjectId + "\\b");
        return patternAuthObject.matcher(rowRole.getProperty("auth_object")).find();
    }
}
