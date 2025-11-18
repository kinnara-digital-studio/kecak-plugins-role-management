package com.kinnarastudio.kecakplugins.rolemanagement;

public enum Permission {
    NONE(0),
    READ(1),
    WRITE(3);

    private int code;

    Permission(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
