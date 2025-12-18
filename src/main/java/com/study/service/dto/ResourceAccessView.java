package com.study.service.dto;

import java.util.List;

/**
 * Aggregated view of who can access a resource.
 */
public class ResourceAccessView {
    private List<ResourceRoleScope> roleScopes;
    private List<ResourceUserScope> userScopes;

    public List<ResourceRoleScope> getRoleScopes() {
        return roleScopes;
    }

    public void setRoleScopes(List<ResourceRoleScope> roleScopes) {
        this.roleScopes = roleScopes;
    }

    public List<ResourceUserScope> getUserScopes() {
        return userScopes;
    }

    public void setUserScopes(List<ResourceUserScope> userScopes) {
        this.userScopes = userScopes;
    }
}
