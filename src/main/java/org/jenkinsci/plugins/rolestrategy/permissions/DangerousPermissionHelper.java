/*
 * The MIT License
 *
 * Copyright (c) 2017 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.rolestrategy.permissions;

import com.michelin.cio.hudson.plugins.rolestrategy.Role;
import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy;
import hudson.PluginManager;
import hudson.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Helper methods for dangerous permission handling.
 * @author Oleg Nenashev
 */
@Restricted(NoExternalUse.class)
public class DangerousPermissionHelper {
    
    /**
     * List of the dangerous permissions, which need to be suppressed by the plugin.
     */
    @Restricted(NoExternalUse.class)
    public static final Set<Permission> DANGEROUS_PERMISSIONS = Collections.unmodifiableSet(new HashSet<Permission>(Arrays.asList(
            Jenkins.RUN_SCRIPTS,
            PluginManager.CONFIGURE_UPDATECENTER,
            PluginManager.UPLOAD_PLUGINS)));
    
    private DangerousPermissionHelper() {
        // Cannot be constructed
    }
    
    /**
     * Check if the permissions is dangerous.
     * Takes the current {@link DangerousPermissionHandlingMode} into account.
     * @param p Permission
     * @return {@code true} if the permission is considered as dangerous.
     *         Always {@code false} in the {@link DangerousPermissionHandlingMode#ENABLED} mode.
     */
    public static boolean isDangerous(@Nonnull Permission p) {
        if (DangerousPermissionHandlingMode.CURRENT == DangerousPermissionHandlingMode.ENABLED) {
            // all permissions are fine
            return false;
        }
        
        return DANGEROUS_PERMISSIONS.contains(p);
    }
    
    /**
     * Checks if the role is potentially dangerous.
     * Does not take the current {@link DangerousPermissionHandlingMode} into account.
     * @param r Role
     * @return {@code true} if the role contains a dangerous permission without {@link Jenkins#ADMINISTER}.
     */
    public static boolean hasPotentionallyDangerousPermissions(@Nonnull Role r) {
        // We do not care about permissions for Jenkins admins, otherwise we report the issue
        return !r.hasPermission(Jenkins.ADMINISTER) && r.hasAnyPermission(DANGEROUS_PERMISSIONS);
    }
    
    /**
     * Prepare the report string about dangerous roles.
     * @param strategy Strategy
     * @return Summary report string describing dangerous roles.
     *         {@code null} if all roles are fine.
     */
    @CheckForNull
    public static String reportDangerousPermissions(@Nonnull RoleBasedAuthorizationStrategy strategy) {
        SortedMap<Role, Set<String>> grantedRoles = strategy.getGrantedRoles(RoleBasedAuthorizationStrategy.GLOBAL);
        return reportDangerousPermissions(grantedRoles.keySet());
    }
    
    /**
     * Prepare the report string about dangerous roles.
     * @param roles Roles
     * @return Summary report string describing dangerous roles.
     *         {@code null} if all roles are fine.
     */
    @CheckForNull
    public static String reportDangerousPermissions(@Nonnull Iterable<Role> roles) {
       final ArrayList<String> dangerousRoleNames = new ArrayList<String>();
       for (Role role : roles) {
           if (hasPotentionallyDangerousPermissions(role)) {
               dangerousRoleNames.add(role.getName());
           }
       }
       
       if (dangerousRoleNames.isEmpty()) {
           // No dangerous roles
           return null;
       }
       
       StringBuilder report = new StringBuilder("Dangerous roles found: [");
       report.append(StringUtils.join(dangerousRoleNames, ","));
       report.append("] do not declare Jenkins.ADMINISTER and contain one of the following permissions: ");
       report.append(StringUtils.join(DANGEROUS_PERMISSIONS, ","));
       return report.toString();
    }
}
