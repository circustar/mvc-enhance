package com.circustar.mybatis_accessor.model;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.circustar.mybatis_accessor.annotation.QueryOrder;
import com.circustar.mybatis_accessor.classInfo.DtoClassInfo;
import com.circustar.mybatis_accessor.classInfo.DtoClassInfoHelper;
import com.circustar.mybatis_accessor.classInfo.DtoField;
import com.circustar.mybatis_accessor.classInfo.EntityClassInfo;
import com.circustar.common_utils.reflection.FieldUtils;
import com.circustar.mybatis_accessor.utils.TableJoinColumnPrefixManager;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class QueryWrapperCreator {
    private String tableName;
    private List<QuerySelectModel> querySelectModels;
//    private List<QueryJoinModel> queryJoinModels;
    private List<QueryWhereModel> queryWhereModels;
    private List<QueryGroupByModel> queryGroupByModels;
    private List<QueryHavingModel> queryHavingModels;
    private List<QueryOrderModel> queryOrders;
    private DtoClassInfo dtoClassInfo;
    private EntityClassInfo entityClassInfo;
    private TableInfo tableInfo;
    private List<DtoField> joinTableDtoFields;
    private DtoClassInfoHelper dtoClassInfoHelper;

    public QueryWrapperCreator(DtoClassInfoHelper dtoClassInfoHelper, DtoClassInfo dtoClassInfo) {
        this.dtoClassInfoHelper = dtoClassInfoHelper;
        this.dtoClassInfo = dtoClassInfo;
        this.entityClassInfo = dtoClassInfo.getEntityClassInfo();
        this.tableInfo = entityClassInfo.getTableInfo();
        this.tableName = this.tableInfo.getTableName();
        this.queryGroupByModels = dtoClassInfo.getNormalFieldList()
                .stream().filter(x -> x.getQueryGroupBy() != null)
                .map(x -> new QueryGroupByModel(x.getQueryGroupBy()
                        , this.entityClassInfo.getTableInfo().getTableName()
                        , x.getEntityFieldInfo().getColumnName()))
                .collect(Collectors.toList());

//        this.queryJoinModels = dtoClassInfo.getSubDtoFieldList()
//                .stream()
//                .filter(x -> x.getQueryJoin() != null
//                        || (x.getQueryJoin() == null && x.getEntityFieldInfo() != null))
//                .map(x -> {
//                    if(x.getQueryJoin() != null && !StringUtils.isEmpty(x.getQueryJoin().joinExpression())) {
//                        return new QueryJoinModel(x.getQueryJoin());
//                    }
//                    String thisTableId = this.tableInfo.getKeyColumn();
//                    String thatTableId = null;
//                    DtoClassInfo thatDtoClassInfo = this.dtoClassInfoHelper.getDtoClassInfo(x.getEntityDtoServiceRelation().getDtoClass());
//                    for(EntityFieldInfo entityFieldInfo : thatDtoClassInfo.getEntityClassInfo().getFieldList()) {
//                        if(thisTableId.equals(entityFieldInfo.getColumnName())) {
//                            thatTableId = entityFieldInfo.getColumnName();
//                            break;
//                        }
//                    }
//                    if(thatTableId == null) {
//                        thisTableId = null;
//                        thatTableId = thatDtoClassInfo.getEntityClassInfo().getTableInfo().getKeyColumn();
//                        for(EntityFieldInfo entityFieldInfo : this.entityClassInfo.getFieldList()) {
//                            if(thatTableId.equals(entityFieldInfo.getColumnName())) {
//                                thisTableId = entityFieldInfo.getColumnName();
//                            }
//                        }
//                    }
//                    if(StringUtils.isEmpty(thisTableId) || StringUtils.isEmpty(thatTableId)) {
//                        return null;
//                    }
//                    joinTableDtoClassList.add(x.getDtoClassInfo());
//                    return new QueryJoinModel(x.getQueryJoin()
//                        , this.tableName, thisTableId
//                        , thatDtoClassInfo.getEntityClassInfo().getTableInfo().getTableName()
//                        , thatTableId);
//                }).filter(x -> x != null)
//                .sorted(Comparator.comparingInt(QueryJoinModel::getOrder))
//                .collect(Collectors.toList());
        this.queryWhereModels = dtoClassInfo.getNormalFieldList()
                .stream().map(x -> new QueryWhereModel(x.getQueryWhere()
                        , this.tableName, x))
                .collect(Collectors.toList());
        this.queryOrders = dtoClassInfo.getNormalFieldList()
                .stream().filter(x -> x.getQueryOrder() != null)
                .map(x -> new QueryOrderModel(x.getQueryOrder()))
                .sorted(Comparator.comparingInt(QueryOrderModel::getSortIndex))
                .collect(Collectors.toList());

        if(this.queryGroupByModels.size() > 0) {
            this.queryHavingModels = dtoClassInfo.getNormalFieldList()
                    .stream().filter(x -> x.getQueryHaving() != null)
                    .map(x -> new QueryHavingModel(x.getQueryHaving()))
                    .collect(Collectors.toList());
            this.querySelectModels = dtoClassInfo.getNormalFieldList()
                    .stream().filter(x -> x.getQuerySelect() != null || x.getQueryGroupBy() != null)
                    .map(x -> {
                        if(x.getQuerySelect() != null) {
                            return new QuerySelectModel(x.getQuerySelect()
                                    , this.tableName
                                    , x.getEntityFieldInfo().getColumnName());
                        } else {
                            return new QuerySelectModel(x.getQueryGroupBy()
                                    , this.tableName
                                    , x.getEntityFieldInfo().getColumnName());
                        }
                    })
                    .collect(Collectors.toList());
        } else {
            this.queryHavingModels = new ArrayList<>();
            this.joinTableDtoFields = dtoClassInfo.getSubDtoFieldList()
                .stream()
                .filter(x -> x.getTableJoinInfo() != null)
                .collect(Collectors.toList());
            ;
            List<QuerySelectModel> joinQueryModels = this.joinTableDtoFields.stream().map(x -> {
                return this.dtoClassInfoHelper.getDtoClassInfo(x.getEntityDtoServiceRelation().getDtoClass())
                        .getNormalFieldList().stream()
                        .filter(y -> y.getQuerySelect() != null || y.getEntityFieldInfo() != null)
                        .map(y ->
                        new QuerySelectModel(y.getQuerySelect()
                                , x.getTableJoinInfo().getQueryJoin().getTableAlias()
                                , y.getEntityFieldInfo().getColumnName()
                                , TableJoinColumnPrefixManager.tryGet((Class)x.getDtoClassInfo().getEntityClassInfo().getEntityClass()
                                , x.getField().getName())))
                        .collect(Collectors.toList());
            }).flatMap(x -> x.stream()).collect(Collectors.toList());
            List<QuerySelectModel> querySelectModels = dtoClassInfo.getNormalFieldList()
                    .stream().filter(x -> x.getQuerySelect() != null || x.getEntityFieldInfo() != null)
                    .map(x -> {
                        return new QuerySelectModel(x.getQuerySelect()
                                , this.tableName
                                , x.getEntityFieldInfo().getColumnName());
                    })
                    .collect(Collectors.toList());
            this.querySelectModels = new ArrayList<>();
            this.querySelectModels.addAll(querySelectModels);
            this.querySelectModels.addAll(joinQueryModels);
        }
    }

    private QueryWrapperBuilder baseWrapperBuilder = null;
    public static QueryWrapperBuilder createQueryWrapperBuilder(
            List<QuerySelectModel> querySelectModels
           ,List<QueryGroupByModel> queryGroupByModels
           ,List<QueryHavingModel> queryHavingModels
           ,List<QueryOrderModel> queryOrders) {
        List<String> columnList = querySelectModels.stream().map(x -> x.getExpression()).collect(Collectors.toList());
        List<String> groupByList = queryGroupByModels.stream().map(x -> x.getExpression()).collect(Collectors.toList());
        List<String> havingList = queryHavingModels.stream().map(x -> x.getExpression()).collect(Collectors.toList());
        List<QueryOrderModel> orderByList = queryOrders.stream().sorted(Comparator.comparingInt(QueryOrderModel::getSortIndex))
                .collect(Collectors.toList());

        return new QueryWrapperBuilder(columnList.toArray(new String[columnList.size()])
                ,groupByList
                , havingList.stream().collect(Collectors.joining(","))
                , orderByList);

    }
    public static <T> QueryWrapper<T> createQueryWrapper(Object dto, List<QueryWhereModel> queryWhereModels, QueryWrapperBuilder queryWrapperBuilder){
        QueryWrapper<T> result = queryWrapperBuilder.createQueryWrapper();
        try {
            for (QueryWhereModel queryWhere : queryWhereModels) {
                queryWhere.getConnector().consume(queryWhere.getExpression()
                        , result, FieldUtils.getFieldValue(dto, queryWhere.getDtoField().getPropertyDescriptor().getReadMethod()));
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return result;
    }

    public <T> QueryWrapper<T> createQueryWrapper(Object dto, List<QueryWhereModel> queryWhereModels) {
        if(this.baseWrapperBuilder == null) {
            this.baseWrapperBuilder = createQueryWrapperBuilder(this.querySelectModels
                    , this.queryGroupByModels
                    , this.queryHavingModels
                    , this.queryOrders);
        }
        return createQueryWrapper(dto, queryWhereModels, this.baseWrapperBuilder);
    }

    public <T> QueryWrapper<T> createQueryWrapper(Object dto) {
        return createQueryWrapper(dto, this.queryWhereModels);
    }

    public static class QueryWrapperBuilder {
        private String[] columns;
        private List<String> groupBys;
        private String having;
        private List<QueryOrderModel> orderModels;

        private QueryWrapperBuilder(String[] columns, List<String> groupBys, String having
                , List<QueryOrderModel> orderModels) {
            this.columns = columns;
            this.groupBys = groupBys;
            this.having = having;
            this.orderModels = orderModels;
        }

        public <T> QueryWrapper<T> createQueryWrapper() {
            QueryWrapper<T> qw = new QueryWrapper<>();
            if(columns != null && columns.length > 0) {
                qw.select(columns);
            }
            if(groupBys != null && groupBys.size() > 0) {
                qw.groupBy(groupBys);
            }
            if(!StringUtils.isEmpty(having)) {
                qw.having(having);
            }
            orderModels.stream().forEach(x ->  {
                if(QueryOrder.ORDER_DESC.equals(x.getSortOrder())) {
                    qw.orderByDesc(x.getOrderExpression());
                } else {
                    qw.orderByAsc(x.getOrderExpression());
                }
            });

            return qw;
        }
    }
}
