package com.circustar.mybatis_accessor.service;

import com.circustar.mybatis_accessor.classInfo.*;
import com.circustar.mybatis_accessor.common.MessageProperties;
import com.circustar.mybatis_accessor.provider.*;
import com.circustar.mybatis_accessor.provider.parameter.IProviderParam;
import com.circustar.mybatis_accessor.relation.EntityDtoServiceRelation;
import com.circustar.mybatis_accessor.updateProcessor.IEntityUpdateProcessor;


import java.util.*;

public class UpdateService implements IUpdateService {
    public UpdateService(DtoClassInfoHelper dtoClassInfoHelper) {
        this.dtoClassInfoHelper = dtoClassInfoHelper;
    }

    private DtoClassInfoHelper dtoClassInfoHelper;

    @Override
    public <T> List<T> updateByProviders(EntityDtoServiceRelation relationInfo
            , Object object, IUpdateEntityProvider provider
            , IProviderParam options) {
        List<T> updatedObjects = new ArrayList<>();
        List<IEntityUpdateProcessor> objList = provider.createUpdateEntities(relationInfo, dtoClassInfoHelper
                , object, options);
        for(IEntityUpdateProcessor o : objList) {
            boolean result = o.execUpdate();
            if(!result) {
                throw new RuntimeException(String.format(MessageProperties.UPDATE_TARGET_NOT_FOUND
                        , "DTO CLASS - " + relationInfo.getDtoClass().getSimpleName()
                                + ", UPDATE PROCESSOR - " + o.getClass().getSimpleName()));
            }
            if(o.getUpdatedEntityList() != null && o.getUpdatedEntityList().size() > 0) {
                updatedObjects.addAll(o.getUpdatedEntityList());
            }
        }
        provider.onSuccess(object, updatedObjects);

        return updatedObjects;
    }
}
