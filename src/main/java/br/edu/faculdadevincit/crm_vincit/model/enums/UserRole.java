package br.edu.faculdadevincit.crm_vincit.model.enums;

public enum UserRole {

    ADMINISTRADOR(0, "administrador"),
    VENDEDOR(1, "vendedor"),
    MASTER(2, "master");

    private final int id;
    private final String role;

    UserRole(int id, String role) {
        this.id = id;
        this.role = role;
    }

    public int getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    public static UserRole fromId(int id) {
        for (UserRole role : values()) {
            if (role.id == id) {
                return role;
            }
        }
        throw new IllegalArgumentException("ID de cargo inválido: " + id);
    }
}
