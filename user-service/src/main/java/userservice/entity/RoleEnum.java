package userservice.entity;

import java.util.HashSet;
import java.util.Set;

public enum RoleEnum implements Role {
    ROLE_USER,
    ROLE_ADMIN;

    private final Set<Role> children = new HashSet<>();

    static {
        ROLE_ADMIN.children.add(ROLE_USER);
    }

    @Override
    public boolean includes(Role role) {
        return this.equals(role) || children.stream()
                .anyMatch(r -> r.includes(role));
    }
}
