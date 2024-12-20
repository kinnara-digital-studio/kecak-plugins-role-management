package com.kinnarastudio.kecakplugins.rolemanagement;

import java.util.ArrayList;
import java.util.Collection;

import com.kinnarastudio.kecakplugins.rolemanagement.optionsbinder.AppOptionsBinder;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<>();

        //Register plugin here
        registrationList.add(context.registerService(PropertyOptionsOptionsBindersWebService.class.getName(), new PropertyOptionsOptionsBindersWebService(), null));
        registrationList.add(context.registerService(RoleManagementFormLoadBinder.class.getName(), new RoleManagementFormLoadBinder(), null));
        registrationList.add(context.registerService(RoleManagementPermission.class.getName(), new RoleManagementPermission(), null));
        registrationList.add(context.registerService(RoleManagementRoleGroupParticipant.class.getName(), new RoleManagementRoleGroupParticipant(), null));
        registrationList.add(context.registerService(RoleManagementWhitelistParticipant.class.getName(), new RoleManagementWhitelistParticipant(), null));
        registrationList.add(context.registerService(RoleManagementConfiguration.class.getName(), new RoleManagementConfiguration(), null));
        registrationList.add(context.registerService(AppOptionsBinder.class.getName(), new AppOptionsBinder(), null));
    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}