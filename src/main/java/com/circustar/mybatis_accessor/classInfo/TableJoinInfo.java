package com.circustar.mybatis_accessor.classInfo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.circustar.common_utils.reflection.FieldUtils;
import com.circustar.mybatis_accessor.annotation.QueryJoin;
import com.circustar.mybatis_accessor.utils.TableInfoUtils;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandler;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class TableJoinInfo {
    private String fieldName;
    private Class targetClass;
    private Field field;
    private QueryJoin queryJoin;
    private boolean isCollection;
    private Type actualType;
    private Type ownerType;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public Class getTargetClass() {
        return targetClass;
    }

    public void setTargetClass(Class targetClass) {
        this.targetClass = targetClass;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public QueryJoin getQueryJoin() {
        return queryJoin;
    }

    public void setQueryJoin(QueryJoin queryJoin) {
        this.queryJoin = queryJoin;
    }

    public boolean isCollection() {
        return isCollection;
    }

    public void setCollection(boolean collection) {
        isCollection = collection;
    }

    public Type getActualType() {
        return actualType;
    }

    public void setActualType(Type actualType) {
        this.actualType = actualType;
    }

    public Type getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(Type ownerType) {
        this.ownerType = ownerType;
    }

    public static List<TableJoinInfo> parseDtoTableJoinInfo(Class targetClass) {
        List<TableJoinInfo> tableJoinInfos = new ArrayList<>();
        List<PropertyDescriptor> propertyDescriptors = FieldUtils.getPropertyDescriptors(targetClass);
        for(PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            Field field = FieldUtils.getField(targetClass, propertyDescriptor.getName());
            QueryJoin queryJoin = field.getAnnotation(QueryJoin.class);
            if(queryJoin == null) {
                continue;
            }
            TableJoinInfo tableJoinInfo = new TableJoinInfo();
            tableJoinInfo.setField(field);
            tableJoinInfo.setFieldName(field.getName());
            tableJoinInfo.setTargetClass(targetClass);

            if(Collection.class.isAssignableFrom(field.getType())) {
                tableJoinInfo.setCollection(true);
                Type dtoType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                tableJoinInfo.setActualType(dtoType);
                tableJoinInfo.setOwnerType(((ParameterizedType) field.getGenericType()).getRawType());
            } else {
                tableJoinInfo.setCollection(false);
                tableJoinInfo.setOwnerType(field.getType());
                tableJoinInfo.setActualType(field.getType());
            }

            tableJoinInfo.setQueryJoin(queryJoin);

            tableJoinInfos.add(tableJoinInfo);
        }
        return tableJoinInfos.stream().sorted(Comparator.comparingInt(x -> x.getQueryJoin().order()))
                .collect(Collectors.toList());
    }

    public static List<TableJoinInfo> parseEntityTableJoinInfo(Configuration configuration, Class targetClass) {
        List<TableJoinInfo> tableJoinInfos = new ArrayList<>();
        List<PropertyDescriptor> propertyDescriptors = FieldUtils.getPropertyDescriptors(targetClass);
        for(PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            Field field = FieldUtils.getField(targetClass, propertyDescriptor.getName());
            TableField[] tableField = field.getAnnotationsByType(TableField.class);
            if(tableField == null || tableField.length == 0 || tableField[0].exist()) {
                continue;
            }
            Class clazz = field.getType();
            if(clazz == null || TableInfoUtils.isMybatisSupportType(clazz)) {
                continue;
            }
            TypeHandler<? extends TableField[]> typeHandler = configuration.getTypeHandlerRegistry().getTypeHandler(clazz);
            if(typeHandler != null) {
                continue;
            }
            TableJoinInfo tableJoinInfo = new TableJoinInfo();
            tableJoinInfo.setField(field);
            tableJoinInfo.setFieldName(field.getName());
            tableJoinInfo.setTargetClass(targetClass);

            if(Collection.class.isAssignableFrom(field.getType())) {
                tableJoinInfo.setCollection(true);
                Type dtoType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                tableJoinInfo.setActualType(dtoType);
                tableJoinInfo.setOwnerType(((ParameterizedType) field.getGenericType()).getRawType());
            } else {
                tableJoinInfo.setCollection(false);
                tableJoinInfo.setOwnerType(field.getType());
                tableJoinInfo.setActualType(field.getType());
            }

            tableJoinInfos.add(tableJoinInfo);
        }
        return tableJoinInfos;
    }

}
