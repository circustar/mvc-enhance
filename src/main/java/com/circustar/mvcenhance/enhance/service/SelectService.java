package com.circustar.mvcenhance.enhance.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.circustar.mvcenhance.common.query.EntityFilter;
import com.circustar.mvcenhance.common.query.QueryFieldModel;
import com.circustar.mvcenhance.common.response.PageInfo;
import com.circustar.mvcenhance.enhance.field.SubFieldInfo;
import com.circustar.mvcenhance.enhance.mybatisplus.MybatisPlusMapper;
import com.circustar.mvcenhance.enhance.relation.EntityDtoServiceRelation;
import com.circustar.mvcenhance.enhance.relation.IEntityDtoServiceRelationMap;
import com.circustar.mvcenhance.enhance.utils.EnhancedConversionService;
import com.circustar.mvcenhance.enhance.utils.FieldUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class SelectService implements ISelectService {
    public SelectService(ApplicationContext applicationContext, EnhancedConversionService converter, IEntityDtoServiceRelationMap entityDtoServiceRelationMap) {
        this.applicationContext = applicationContext;
        this.converter = converter;
        this.entityDtoServiceRelationMap = entityDtoServiceRelationMap;
    }
    private ApplicationContext applicationContext;

    private IEntityDtoServiceRelationMap entityDtoServiceRelationMap;

    private EnhancedConversionService converter;

    @Override
    public Object getDtoById(EntityDtoServiceRelation relationInfo
            , Serializable id
            , String[] subEntities) throws Exception {
        return getDtoById(relationInfo, id, subEntities, "");
    }

    @Override
    public Object getDtoById(EntityDtoServiceRelation relationInfo
            , Serializable id
            , String[] subEntities
            , String queryGroup) throws Exception {
        IService s = applicationContext.getBean(relationInfo.getService());
        Object oriEntity = s.getById(id);
        if (oriEntity == null) {
            return null;
        }
        Object result = converter.convert(oriEntity, relationInfo.getDto());
        List subEntityList;
        if(subEntities == null) {
            subEntityList = relationInfo.getDtoClassInfo().getFieldInfoList()
                    .stream()
                    .filter(x -> entityDtoServiceRelationMap.getByDtoClass((Class)x.getActualType()) != null)
                    .map(x -> x.getFieldName()).collect(Collectors.toList());
        } else {
            subEntityList = Arrays.asList(subEntities);
        }
        List<Field> subFields = FieldUtils.getExistFields(result, subEntityList, false);

        Map<String, EntityFilter[]> tableJoinerMap = new HashMap<>();
        List<String> noAnnotationInfoList = new ArrayList<>();
        FieldUtils.parseFieldAnnotationToMap(subFields, EntityFilter.class
                , tableJoinerMap, noAnnotationInfoList);

        String keyColumn = TableInfoHelper.getTableInfo(relationInfo.getEntity()).getKeyColumn();
        SubFieldInfo.setSubDtoAfterQueryById(applicationContext, converter, entityDtoServiceRelationMap
                , relationInfo, result, noAnnotationInfoList, keyColumn, id);

        SubFieldInfo.setSubDtoAfterQueryByTableJoiner(applicationContext, converter
                , entityDtoServiceRelationMap
                , relationInfo
                , result
                , tableJoinerMap
                , queryGroup);

        return result;
    }

    @Override
    public <T> PageInfo<T> getPagesByDtoAnnotation(EntityDtoServiceRelation relationInfo
            , Object object
            , String queryGroup
            , Integer page_index
            , Integer page_size
            ) throws Exception {
        List<QueryFieldModel> queryFiledModelList = QueryFieldModel.getQueryFieldModeFromDto(object, queryGroup);

        return getPagesByQueryFields(relationInfo, queryFiledModelList, page_index, page_size);
    }

    @Override
    public <T> PageInfo<T> getPagesByQueryFields(EntityDtoServiceRelation relationInfo
            , List<QueryFieldModel> queryFiledModelList
            , Integer page_index
            , Integer page_size
            ) throws Exception {
        IService service = applicationContext.getBean(relationInfo.getService());
        QueryWrapper qw = new QueryWrapper();
        QueryFieldModel.setQueryWrapper(queryFiledModelList, qw);

        PageInfo pageInfo = null;
        Page page = new Page(page_index, page_size);
        IPage pageResult = service.page(page, qw);

        List dtoList = converter.convertList(pageResult.getRecords(), relationInfo.getDto());
        pageInfo = new PageInfo(pageResult.getTotal(), pageResult.getSize(), pageResult.getCurrent(), dtoList);

        return pageInfo;
    }

    @Override
    public List getListByDtoAnnotation(EntityDtoServiceRelation relationInfo
            , Object object
            , String queryGroup
    ) throws Exception {
        List<QueryFieldModel> queryFiledModelList = QueryFieldModel.getQueryFieldModeFromDto(object, queryGroup);

        return getListByQueryFields(relationInfo, queryFiledModelList);
    }

    @Override
    public <T> List<T> getListByQueryFields(EntityDtoServiceRelation relationInfo
            , List<QueryFieldModel> queryFiledModelList
    ) throws Exception {
        IService service = applicationContext.getBean(relationInfo.getService());
        QueryWrapper qw = new QueryWrapper();
        QueryFieldModel.setQueryWrapper(queryFiledModelList, qw);

        List<T> dtoList = null;
        if (relationInfo.getDtoClassInfo().containsSubDtoFields()) {
            List entityList = ((MybatisPlusMapper)service.getBaseMapper()).selectListWithJoin(qw);
            dtoList = converter.convertList(entityList, relationInfo.getDto());
        } else {
            List entityList = service.list(qw);
            dtoList = converter.convertList(entityList, relationInfo.getDto());
        }

        return dtoList;
    }
}
