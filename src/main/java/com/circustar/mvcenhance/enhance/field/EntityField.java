package com.circustar.mvcenhance.enhance.field;

public class EntityField {
    private String fieldName;
    private FieldTypeInfo fieldTypeInfo;
    private boolean isPrimary;

    public EntityField(String fieldName, FieldTypeInfo fieldTypeInfo) {
        this.fieldName = fieldName;
        this.fieldTypeInfo = fieldTypeInfo;
    }

}
