package com.circustar.mybatis_accessor.listener.event.update;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.circustar.common_utils.reflection.FieldUtils;
import com.circustar.mybatis_accessor.annotation.event.IUpdateEvent;
import com.circustar.mybatis_accessor.class_info.DtoClassInfo;
import com.circustar.mybatis_accessor.class_info.DtoField;
import com.circustar.mybatis_accessor.class_info.EntityFieldInfo;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
// 解决分配问题
// 参数1：需要分配的值
// 参数2：分配目标子表对应的变量名
// 参数3：分配目标字段对应的变量名
// 参数4：精度
// 参数5：分配权重对应的变量名
// 参数6：总权重，可以为空，空时等于sum(参数5)

public class UpdateAssignSqlEvent extends UpdateAvgSqlEvent implements IUpdateEvent<UpdateEventModel> {

    protected static final String WEIGHT_VALUE_COLUMN = "##WEIGHT_VALUE##";
    protected static final String SELECT_SQL = "Round((sum(%s) over (partition by %s order by %s)) / " + WEIGHT_VALUE_COLUMN + " * %s, %s) " +
            "- Round(((sum(%s) over (partition by %s order by %s)) - %s) / " + WEIGHT_VALUE_COLUMN + " * %s, %s) as %s";

    protected static final String CALC_WEIGHT_SQL = "(sum(%s) over (partition by %s))";

    protected boolean patternProvidedWeight = false;

    @Override
    protected List<DtoField> parseDtoFieldList(UpdateEventModel updateEventModel, DtoClassInfo dtoClassInfo) {
        List<DtoField> dtoFields = super.parseDtoFieldList(updateEventModel, dtoClassInfo);
        String sWeightFieldName = updateEventModel.getUpdateParams().get(4);
        DtoField sWeightField = dtoFields.get(1).getFieldDtoClassInfo().getDtoField(sWeightFieldName);
        dtoFields.add(sWeightField);
        this.patternProvidedWeight = false;
        if(updateEventModel.getUpdateParams().size() >= 6) {
            String sAllWeightFieldName = updateEventModel.getUpdateParams().get(5);
            DtoField sAllWeightField = dtoFields.get(1).getFieldDtoClassInfo().getDtoField(sAllWeightFieldName);
            dtoFields.add(sAllWeightField);
            this.patternProvidedWeight = true;
        }

        return dtoFields;
    }

    @Override
    protected String createSqlPart(UpdateEventModel updateEventModel, DtoClassInfo dtoClassInfo, TableInfo tableInfo
            , DtoClassInfo subDtoClassInfo, TableInfo subTableInfo, List<DtoField> dtoFields) {
        String mainTableId = tableInfo.getKeyColumn();
        String sTableId = subTableInfo.getKeyColumn();
        if(tableInfo.equals(subTableInfo)) {
            mainTableId = dtoClassInfo.getEntityClassInfo().getIdReferenceFieldInfo().getColumnName();
        }
        String sAssignColumnName = dtoFields.get(2).getEntityFieldInfo().getColumnName();
        String sWeightColumnName = dtoFields.get(3).getEntityFieldInfo().getColumnName();
        String precision = updateEventModel.getUpdateParams().get(3);

        if(patternProvidedWeight) {
            return String.format(SELECT_SQL.replace(WEIGHT_VALUE_COLUMN, "%s")
                    , sWeightColumnName, mainTableId, sTableId, "%s"
                    , "%s", precision
                    , sWeightColumnName, mainTableId, sTableId, sWeightColumnName
                    , "%s", "%s", precision
                    , sAssignColumnName);
        } else {
            return String.format(SELECT_SQL.replace(WEIGHT_VALUE_COLUMN, CALC_WEIGHT_SQL)
                    , sWeightColumnName, mainTableId, sTableId, sWeightColumnName
                    , mainTableId, "%s", precision
                    , sWeightColumnName, mainTableId, sTableId, sWeightColumnName
                    , sWeightColumnName, mainTableId, "%s", precision
                    , sAssignColumnName);
        }

    }

    @Override
    protected void execUpdate(DtoClassInfo dtoClassInfo, DtoClassInfo fieldDtoClassInfo
            , List<Object> dtoList, List<DtoField> dtoFields, List<Object> parsedParams
            , String updateEventLogId) {
        TableInfo tableInfo = dtoClassInfo.getEntityClassInfo().getTableInfo();
        TableInfo subTableInfo = fieldDtoClassInfo.getEntityClassInfo().getTableInfo();
        Method mKeyFieldReadMethod = dtoClassInfo.getKeyField().getPropertyDescriptor().getReadMethod();
        Method mFieldReadMethod = dtoFields.get(0).getEntityFieldInfo().getPropertyDescriptor().getReadMethod();
        Method mWeightFileReadMethod = null;
        if(patternProvidedWeight) {
            mWeightFileReadMethod = dtoFields.get(4).getEntityFieldInfo().getPropertyDescriptor().getReadMethod();
        }
        String assignTemplateSql = parsedParams.get(0).toString();
        String mTableId = tableInfo.getKeyColumn();
        if(fieldDtoClassInfo.equals(dtoClassInfo)) {
            mTableId = dtoClassInfo.getEntityClassInfo().getIdReferenceFieldInfo().getColumnName();
        }
        String sTableId = subTableInfo.getKeyColumn();
        Method sKeyFieldReadMethod = fieldDtoClassInfo.getEntityClassInfo().getKeyField().getPropertyDescriptor().getReadMethod();
        EntityFieldInfo sAssignEntityFieldInfo = dtoFields.get(2).getEntityFieldInfo();
        Method sAssignFieldReadMethod = sAssignEntityFieldInfo.getPropertyDescriptor().getReadMethod();
        String sAssignColumnName = sAssignEntityFieldInfo.getColumnName();

        IService mServiceBean = dtoClassInfo.getServiceBean();
        IService sServiceBean = fieldDtoClassInfo.getServiceBean();

        QueryWrapper queryWrapper = new QueryWrapper();
        UpdateWrapper updateWrapper = new UpdateWrapper();
        for(Object o : dtoList) {
            Object keyValue = FieldUtils.getFieldValue(o, mKeyFieldReadMethod);
            Object entity = mServiceBean.getById((Serializable) keyValue);
            Object summaryValue = FieldUtils.getFieldValue(entity, mFieldReadMethod);
            if(summaryValue == null) {
                continue;
            }

            String assignValueSql;
            if(mWeightFileReadMethod != null) {
                Object weightValue = FieldUtils.getFieldValue(entity, mWeightFileReadMethod);
                assignValueSql = String.format(assignTemplateSql, weightValue, summaryValue, weightValue, summaryValue);
            } else {
                assignValueSql = String.format(assignTemplateSql, summaryValue, summaryValue);
            };
            queryWrapper.clear();
            queryWrapper.select(sTableId,mTableId,assignValueSql);
            queryWrapper.eq(mTableId, keyValue);
            if(fieldDtoClassInfo.getDeleteFlagField() != null) {
                queryWrapper.eq(fieldDtoClassInfo.getDeleteFlagField().getEntityFieldInfo().getColumnName()
                        , subTableInfo.getLogicDeleteFieldInfo().getLogicNotDeleteValue());
            }
            List subEntityList = sServiceBean.list(queryWrapper);
            for(Object subEntity : subEntityList) {
                updateWrapper.clear();
                Object subKeyValue = FieldUtils.getFieldValue(subEntity, sKeyFieldReadMethod);
                updateWrapper.eq(sTableId, subKeyValue);
                Object assignValue = FieldUtils.getFieldValue(subEntity, sAssignFieldReadMethod);
                updateWrapper.set(sAssignColumnName, assignValue);
                sServiceBean.update(updateWrapper);
            }
        }
    }
}
