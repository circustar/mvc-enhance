package com.circustar.mybatis_accessor.provider;

import com.circustar.common_utils.collection.CollectionUtils;
import com.circustar.common_utils.reflection.FieldUtils;
import com.circustar.mybatis_accessor.classInfo.DtoClassInfo;
import com.circustar.mybatis_accessor.classInfo.DtoClassInfoHelper;
import com.circustar.mybatis_accessor.classInfo.DtoField;
import com.circustar.mybatis_accessor.provider.command.DeleteByIdCommand;
import com.circustar.mybatis_accessor.provider.parameter.DefaultEntityProviderParam;
import com.circustar.mybatis_accessor.provider.parameter.IEntityProviderParam;
import com.circustar.mybatis_accessor.relation.EntityDtoServiceRelation;
import com.circustar.mybatis_accessor.service.ISelectService;
import com.circustar.mybatis_accessor.updateProcessor.DefaultEntityCollectionUpdateProcessor;
import com.circustar.mybatis_accessor.updateProcessor.IEntityUpdateProcessor;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class DefaultDeleteProcessorProvider extends AbstractUpdateEntityProvider<IEntityProviderParam> {

    public DefaultDeleteProcessorProvider(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    @Override
    protected boolean getUpdateChildrenFirst() {
        return true;
    }

    @Override
    public List<IEntityUpdateProcessor> createUpdateEntities(EntityDtoServiceRelation relation
            , DtoClassInfoHelper dtoClassInfoHelper, Object ids, IEntityProviderParam options) {
        return this.createUpdateProcessors(relation, dtoClassInfoHelper, ids, options);
    }

    protected Object convertToUpdateTarget(DtoClassInfo dtoClassInfo, Object obj) {
        Method readMethod = dtoClassInfo.getKeyField().getPropertyDescriptor().getReadMethod();
        return FieldUtils.getFieldValue(obj, readMethod);
    }

    protected List convertToSubUpdateList(DtoClassInfoHelper dtoClassInfoHelper, DtoClassInfo dtoClassInfo, List obj) {
        return obj;
    }

    protected Object getUpdateId(Object obj, DtoField keyField) {
        return FieldUtils.getFieldValue(obj, keyField.getPropertyDescriptor().getReadMethod());
    }

    protected List<IEntityUpdateProcessor> createUpdateProcessors(EntityDtoServiceRelation relation
            , DtoClassInfoHelper dtoClassInfoHelper, Object dtoParam, IEntityProviderParam options)
    {
        List<IEntityUpdateProcessor> result = new ArrayList<>();
        List dtoList = CollectionUtils.convertToList(dtoParam);
        if(dtoList.isEmpty()) {return result;}

        DtoClassInfo dtoClassInfo = dtoClassInfoHelper.getDtoClassInfo(relation);
        List<String> children;
        if(options.isIncludeAllChildren()) {
            children = dtoClassInfo.getUpdateCascadeDtoFieldList().stream().map(x -> x.getField().getName()).collect(Collectors.toList());
        } else {
            children = options.getUpdateChildrenNames();
        }

        ISelectService selectService = this.getSelectService();

        List<String> topEntities = this.getTopEntities(dtoClassInfo, children, DEFAULT_DELIMITER);
        DefaultEntityCollectionUpdateProcessor updateProcessor;
        List updateEntityWithNoSubList = new ArrayList();

        if(topEntities == null || topEntities.size() == 0) {
            updateEntityWithNoSubList = (List) dtoList.stream()
                    .map(x -> this.convertToUpdateTarget(dtoClassInfo, x))
                    .collect(Collectors.toList());
        } else {
            List<DtoField> dtoFields = topEntities.stream().map(x -> dtoClassInfo.getDtoField(x)).collect(Collectors.toList());
            for (Object dto : dtoList) {
                Serializable id = (Serializable) getUpdateId(dto, dtoClassInfo.getKeyField());
                if(id == null) {
                    throw new RuntimeException(relation.getEntityClass().getSimpleName()
                            + " delete exception : id could not be null : " + dtoClassInfo.getClass().getSimpleName());
                }
                List<IEntityUpdateProcessor> subUpdateEntities = new ArrayList<>();
                Object object = selectService.getDtoById(relation, id,false , topEntities);
                if(object == null) {
                    throw new RuntimeException(relation.getEntityClass().getSimpleName()
                            + " delete exception : key not found : " + id);
                }
                for (DtoField subDtoField : dtoFields) {
                    Object subDto = FieldUtils.getFieldValue(object, subDtoField.getPropertyDescriptor().getReadMethod());
                    if (subDto == null) { continue; }
                    DtoClassInfo subDtoClassInfo = subDtoField.getFieldDtoClassInfo();

                    Collection childList = this.convertToSubUpdateList(dtoClassInfoHelper, subDtoClassInfo, CollectionUtils.convertToList(subDto));
                    if(childList.isEmpty()) {continue;}
                    IEntityProviderParam subOptions = new DefaultEntityProviderParam(false
                            , options.isIncludeAllChildren(), this.getChildren(children
                            , subDtoField.getField().getName(), DEFAULT_DELIMITER));
                    subUpdateEntities.addAll(this.createUpdateEntities(
                            subDtoField.getEntityDtoServiceRelation()
                            , dtoClassInfoHelper, childList, subOptions
                    ));
                }
                if(subUpdateEntities.isEmpty()) {
                    updateEntityWithNoSubList.add(this.convertToUpdateTarget(dtoClassInfo, dto));
                    continue;
                }

                if(options.isUpdateChildrenOnly()) {
                    result.addAll(subUpdateEntities);
                } else {
                    List<Object> updateList = Collections.singletonList(this.convertToUpdateTarget(dtoClassInfo, dto));
                    updateProcessor = new DefaultEntityCollectionUpdateProcessor(relation.getServiceBean(applicationContext)
                            , DeleteByIdCommand.getInstance()
                            , dtoClassInfo.isPhysicDelete()
                            , dtoClassInfo
                            , updateList
                            , false
                            , this.getUpdateChildrenFirst()
                            , false);
                    updateProcessor.addSubUpdateEntities(subUpdateEntities);
                    result.add(updateProcessor);
                }
            }
        }
        if(!options.isUpdateChildrenOnly() && !updateEntityWithNoSubList.isEmpty()) {
            updateProcessor = new DefaultEntityCollectionUpdateProcessor(relation.getServiceBean(applicationContext)
                    , DeleteByIdCommand.getInstance()
                    , dtoClassInfo.isPhysicDelete()
                    , dtoClassInfo
                    , updateEntityWithNoSubList
                    , false
                    , this.getUpdateChildrenFirst()
                    , false);
            result.add(updateProcessor);
        }

        return result;
    }
}
