package com.circustar.mybatis_accessor.updateProcessor;

import com.baomidou.mybatisplus.extension.service.IService;
import com.circustar.common_utils.parser.SPELParser;
import com.circustar.mybatis_accessor.annotation.after_update.AfterUpdateModel;
import com.circustar.mybatis_accessor.annotation.after_update.ExecuteTiming;
import com.circustar.mybatis_accessor.annotation.on_change.OnChangeModel;
import com.circustar.mybatis_accessor.classInfo.DtoClassInfo;
import com.circustar.mybatis_accessor.classInfo.EntityClassInfo;
import com.circustar.mybatis_accessor.classInfo.EntityFieldInfo;
import com.circustar.mybatis_accessor.provider.command.IUpdateCommand;
import com.circustar.common_utils.reflection.FieldUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DefaultEntityCollectionUpdateProcessor implements IEntityUpdateProcessor<Collection> {
    public DefaultEntityCollectionUpdateProcessor(IService service
            , IUpdateCommand updateCommand
            , Object option
            , DtoClassInfo dtoClassInfo
            , List updateDtoList
            , List updateEntityList
            , Boolean updateChildrenFirst
            , boolean updateChildrenOnly) {
        this.option = option;
        this.updateCommand = updateCommand;
        this.service = service;
        this.updateDtoList = updateDtoList;
        this.updateEntityList = updateEntityList;
        this.updateChildFirst = updateChildrenFirst;
        this.dtoClassInfo = dtoClassInfo;
        this.entityClassInfo = dtoClassInfo.getEntityClassInfo();
        this.updateChildrenOnly = updateChildrenOnly;
    }
    private Object option;
    private IUpdateCommand updateCommand;
    private IService service;
    private Boolean updateChildFirst;
    private List updateDtoList;
    private List updateEntityList;
    private List<IEntityUpdateProcessor> subUpdateEntities;
    private DtoClassInfo dtoClassInfo;
    private EntityClassInfo entityClassInfo;
    private boolean updateChildrenOnly;

    public void addSubUpdateEntity(DefaultEntityCollectionUpdateProcessor subDefaultEntityCollectionUpdater) {
        if(this.subUpdateEntities == null) {
            this.subUpdateEntities = new ArrayList<>();
        }
        this.subUpdateEntities.add(subDefaultEntityCollectionUpdater);
    }
    public void addSubUpdateEntities(Collection<IEntityUpdateProcessor> subUpdateEntities) {
        if(subUpdateEntities == null) {
            return;
        }
        if(this.subUpdateEntities == null) {
            this.subUpdateEntities = new ArrayList<>();
        }
        this.subUpdateEntities.addAll(subUpdateEntities);
    }
    public List<IEntityUpdateProcessor> getSubUpdateEntities() {
        return subUpdateEntities;
    }

    public List getUpdatedEntityList() {
        return updateEntityList;
    }

    @Override
    public boolean execUpdate() {
        return execUpdate(new HashMap<>(), new ArrayList<>(), 0);
    }

    @Override
    public boolean execUpdate(Map<String, Object> keyMap, List<Supplier<Integer>> consumerList, int level) {
        boolean result;
        if (updateChildFirst) {
            result = execSubEntityUpdate(keyMap, consumerList, level);
            if(!result){return false;}
        }
        Optional firstEntity = updateEntityList.stream().findFirst();
        if (entityClassInfo != null && firstEntity.isPresent()
                && entityClassInfo.getEntityClass().isAssignableFrom(firstEntity.get().getClass())) {
            List<String> avoidIdList = null;
            if (entityClassInfo.getKeyField() != null && entityClassInfo.getIdReferenceFieldInfo() != null) {
                Object parentPropertyValue = keyMap.get(entityClassInfo.getKeyField().getField().getName());
                if (parentPropertyValue != null) {
                    avoidIdList = Arrays.asList(entityClassInfo.getKeyField().getField().getName()
                            , entityClassInfo.getIdReferenceFieldInfo().getField().getName());
                    Method keyFieldWriteMethod = entityClassInfo.getIdReferenceFieldInfo().getPropertyDescriptor().getWriteMethod();
                    for (Object updateEntity : updateEntityList) {
                        FieldUtils.setFieldValue(updateEntity
                                , keyFieldWriteMethod
                                , parentPropertyValue);
                    }
                }
            }

            for (Map.Entry<String, Object> keyEntry : keyMap.entrySet()) {
                if (avoidIdList != null && avoidIdList.contains(keyEntry.getKey())) {
                    continue;
                }
                EntityFieldInfo entityFieldInfo = entityClassInfo.getFieldByName(keyEntry.getKey());
                if (entityFieldInfo == null) {
                    continue;
                }
                for (Object updateEntity : updateEntityList) {
                    FieldUtils.setFieldValue(updateEntity
                            , entityFieldInfo.getPropertyDescriptor().getWriteMethod(), keyEntry.getValue());
                }
            }
        }
        if (!updateChildrenOnly) {
            List dtoListBeforeUpdate = getDtoListBeforeUpdate(this.updateCommand.getUpdateType()
                    ,this.dtoClassInfo, this.dtoClassInfo.getOnChangeList()
                    , this.updateDtoList);
            executeOnChangeExecutor(Arrays.asList(ExecuteTiming.BEFORE_UPDATE, ExecuteTiming.BEFORE_SUB_ENTITY_UPDATE)
                    , this.updateCommand.getUpdateType()
                    , this.dtoClassInfo, this.dtoClassInfo.getOnChangeList()
                    , this.updateDtoList, dtoListBeforeUpdate);
            executeAfterUpdateExecutor(ExecuteTiming.BEFORE_UPDATE, updateCommand.getUpdateType()
                    , dtoClassInfo, this.dtoClassInfo.getAfterUpdateList()
                    , this.updateDtoList, this.updateEntityList);
            result = this.updateCommand.update(this.service, this.updateEntityList, option);
            if (!result) return false;
            executeOnChangeExecutor(Arrays.asList(ExecuteTiming.AFTER_UPDATE, ExecuteTiming.AFTER_SUB_ENTITY_UPDATE)
                    , this.updateCommand.getUpdateType()
                    , this.dtoClassInfo, this.dtoClassInfo.getOnChangeList()
                    , this.updateDtoList, dtoListBeforeUpdate);
            executeAfterUpdateExecutor(ExecuteTiming.AFTER_UPDATE, updateCommand.getUpdateType()
                    , dtoClassInfo, this.dtoClassInfo.getAfterUpdateList()
                    , this.updateDtoList, this.updateEntityList);
        }

        if (entityClassInfo != null && firstEntity.isPresent()
                && entityClassInfo.getEntityClass().isAssignableFrom(firstEntity.get().getClass())) {
            EntityFieldInfo keyField = entityClassInfo.getKeyField();
            if (keyField != null) {
                Object masterKeyValue = FieldUtils.getFieldValue(firstEntity.get()
                        , keyField.getPropertyDescriptor().getReadMethod());
                keyMap.put(keyField.getField().getName(), masterKeyValue);
            }
        }

        if ((!updateChildFirst)) {
            result = execSubEntityUpdate(keyMap, consumerList, level);
            if(!result){return false;}
        }
        return true;
    }

    private boolean execSubEntityUpdate(Map<String, Object> keyMap, List<Supplier<Integer>> consumerList
            , int level) {
        List<AfterUpdateModel> afterUpdateList = this.dtoClassInfo.getAfterUpdateList();
        executeAfterUpdateExecutor(ExecuteTiming.BEFORE_SUB_ENTITY_UPDATE, this.updateCommand.getUpdateType()
                , this.dtoClassInfo, afterUpdateList
                , this.updateDtoList, this.updateEntityList);
        if(this.subUpdateEntities != null && !this.subUpdateEntities.isEmpty()) {
            if(afterUpdateList != null && !afterUpdateList.isEmpty()) {
                consumerList.add(() -> {
                    executeAfterUpdateExecutor(ExecuteTiming.AFTER_SUB_ENTITY_UPDATE, this.updateCommand.getUpdateType()
                            , this.dtoClassInfo, afterUpdateList
                            , this.updateDtoList, this.updateEntityList);
                    return level;
                });
            }
            for (IEntityUpdateProcessor entityUpdateProcessor : subUpdateEntities) {
                boolean result = entityUpdateProcessor.execUpdate(new HashMap<>(keyMap), consumerList, level + 1);
                if (!result) {
                    return false;
                }
            }
        }
        if(level == 0) {
            int size = consumerList.size();
            for(int i = 0; i < size; i++) {
                consumerList.remove(0).get();
            }
        }
        return true;
    }

    private void executeAfterUpdateExecutor( ExecuteTiming executeTiming, IUpdateCommand.UpdateType updateType
            , DtoClassInfo dtoClassInfo
            , List<AfterUpdateModel> afterUpdateList
            , List updateDtoList, List updateEntityList) {
        if(this.updateChildrenOnly || (afterUpdateList == null || afterUpdateList.isEmpty())) {
            return;
        }
        List<AfterUpdateModel> updateModelList = afterUpdateList.stream()
                .filter(x -> executeTiming.equals(x.getAfterUpdateExecutor().getExecuteTiming()))
                .filter(x -> Arrays.stream(x.getAfterUpdateExecutor().getUpdateTypes()).anyMatch(y -> updateType.equals(y)))
                .collect(Collectors.toList());
        for(AfterUpdateModel m : updateModelList) {
            List executeDtoList = new ArrayList();
            List executeEntityList = new ArrayList();
            for(int i = 0 ; i < updateDtoList.size(); i++) {
                boolean execFlag = true;
                if(StringUtils.hasLength(m.getOnExpression())) {
                    execFlag = (boolean) SPELParser.parseExpression(updateDtoList.get(i),m.getOnExpression());
                }
                if(!execFlag) {
                    continue;
                }
                executeDtoList.add(updateDtoList.get(i));
                executeEntityList.add(updateEntityList.get(i));
            }
            if(!executeDtoList.isEmpty()) {
                m.getAfterUpdateExecutor().exec(this.updateCommand.getUpdateType(),
                        dtoClassInfo, executeDtoList, executeEntityList, m.getUpdateParams());
            }
        }
    }

    private List getDtoListBeforeUpdate(IUpdateCommand.UpdateType updateType
            , DtoClassInfo dtoClassInfo, List<OnChangeModel> onChangeList, List updateDtoList) {
        if(onChangeList == null || onChangeList.isEmpty()) {
            return null;
        }
        if(!onChangeList.stream().flatMap(x -> Arrays.stream(x.getOnChangeExecutor().getUpdateTypes()))
                .anyMatch(x -> updateType.equals(x))) {
            return null;
        }
        List result = new ArrayList();
        Method keyFieldReadMethod = dtoClassInfo.getKeyField().getPropertyDescriptor().getReadMethod();
        for(int i = 0 ; i < updateDtoList.size(); i++) {
            Object updateDto = updateDtoList.get(i);
            if(dtoClassInfo.getClazz().isAssignableFrom(updateDto.getClass())) {
                Serializable key = (Serializable) FieldUtils.getFieldValue(updateDto, keyFieldReadMethod);
                if(key == null) {
                    result.add(null);
                } else {
                    Object oldEntity = service.getById(key);
                    Object oldDto = dtoClassInfo.getDtoClassInfoHelper().convertFromEntity(oldEntity, dtoClassInfo);
                    result.add(oldDto);
                }
            } else {
                result.add(null);
            }
        }
        return result;
    }

    private void executeOnChangeExecutor(List<ExecuteTiming> executeTimingList, IUpdateCommand.UpdateType updateType
            , DtoClassInfo dtoClassInfo, List<OnChangeModel> onChangeList
            , List updateDtoList, List oldDtoList) {
        if(onChangeList == null || onChangeList.isEmpty() || oldDtoList == null || oldDtoList.isEmpty()) {
            return;
        }
        for(OnChangeModel onChangeModel: onChangeList) {
            if(!executeTimingList.stream().anyMatch(x -> x.equals(onChangeModel.getOnChangeExecutor().getExecuteTiming()))) {
                continue;
            }
            if(!Arrays.stream(onChangeModel.getOnChangeExecutor().getUpdateTypes()).anyMatch(x -> updateType.equals(x))) {
                continue;
            }
            for(int i = 0; i < updateDtoList.size(); i++) {
                Object newDto = updateDtoList.get(i);
                Object oldDto = oldDtoList.get(i);
                int compareResult = DtoClassInfo.equalProperties(dtoClassInfo
                        , newDto, oldDto, onChangeModel.getChangePropertyNames());
                boolean execFlag;
                if(onChangeModel.isTriggerOnAnyChanged()) {
                    execFlag = compareResult <= 0;
                } else {
                    execFlag = compareResult < 0;
                }
                if(execFlag) {
                    onChangeModel.getOnChangeExecutor().exec(updateType
                            , dtoClassInfo, newDto, oldDto, onChangeModel.getUpdateParams());
                }
            }
        }
    }
}
