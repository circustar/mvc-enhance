package com.circustar.mvcenhance.classInfo;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.circustar.mvcenhance.annotation.Selector;
import com.circustar.mvcenhance.annotation.QueryField;
import com.circustar.mvcenhance.relation.EntityDtoServiceRelation;

import java.util.*;

public class DtoField {
    private String fieldName;
    private FieldTypeInfo fieldTypeInfo;
    private EntityDtoServiceRelation entityDtoServiceRelation;
    private DtoClassInfo dtoClassInfo;
    private List<QueryField> queryFields;
    private List<Selector> selectors;
    private Class relatedEntityClass = null;
    private Boolean hasEntityClass = null;

    public DtoField(String fieldName,FieldTypeInfo fieldTypeInfo, DtoClassInfo dtoClassInfo, EntityDtoServiceRelation entityDtoServiceRelation) {
        this.fieldName = fieldName;
        this.fieldTypeInfo = fieldTypeInfo;
        this.dtoClassInfo = dtoClassInfo;
        this.entityDtoServiceRelation = entityDtoServiceRelation;

        QueryField[] queryFields = fieldTypeInfo.getField().getAnnotationsByType(QueryField.class);
        this.queryFields = Arrays.asList(queryFields);
        Selector[] selectors = fieldTypeInfo.getField().getAnnotationsByType(Selector.class);
        this.selectors = Arrays.asList(selectors);
    }

    public String getFieldName() {
        return fieldName;
    }

    public FieldTypeInfo getFieldTypeInfo() {
        return fieldTypeInfo;
    }

    public EntityDtoServiceRelation getEntityDtoServiceRelation() {
        return entityDtoServiceRelation;
    }

    public DtoClassInfo getDtoClassInfo() {
        return dtoClassInfo;
    }

    public List<QueryField> getQueryFields() {
        return queryFields;
    }

    public List<Selector> getSelectors() {
        return selectors;
    }

    public Boolean getHasEntityClass() {
        return hasEntityClass;
    }

    public void setHasEntityClass(Boolean hasEntityClass) {
        this.hasEntityClass = hasEntityClass;
    }

    public Class getRelatedEntityClass() {
        return relatedEntityClass;
    }

    public void setRelatedEntityClass(Class relatedEntityClass) {
        this.relatedEntityClass = relatedEntityClass;
    }
}
