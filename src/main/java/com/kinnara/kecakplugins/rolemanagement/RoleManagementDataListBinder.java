package com.kinnara.kecakplugins.rolemanagement;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.*;

import java.util.Map;

public class RoleManagementDataListBinder extends DataListBinderDefault {
    @Override
    public DataListColumn[] getColumns() {
        return new DataListColumn[0];
    }

    @Override
    public String getPrimaryKeyColumnName() {
        return null;
    }

    @Override
    public DataListCollection getData(DataList dataList, Map properties, DataListFilterQueryObject[] filterQueryObjects, String sort, Boolean desc, Integer start, Integer rows) {
        return null;
    }

    @Override
    public int getDataTotalRowCount(DataList dataList, Map properties, DataListFilterQueryObject[] filterQueryObjects) {

        return 0;
    }

    @Override
    public String getName() {
        return "Role Management DataList Binder";
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
        return AppUtil.readPluginResource(getClassName(), "/properties/RoleManagementDataListBinder.json", new String[] {PropertyOptionsOptionsBindersWebService.class.getName()},  false, "/messages/RoleManagementDataListBinder");
    }
}
