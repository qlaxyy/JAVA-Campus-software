package edu.seu.vcampus.client.module.user;

import edu.seu.vcampus.common.user.AdminScope;

import java.util.Set;

/** Values entered in the create/edit account dialog. */
record UserAccountFormData(String username, String displayName, Set<AdminScope> scopes) {
}
