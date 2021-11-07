package com.circustar.mybatis_accessor.listener.event.update_event;

import com.circustar.common_utils.parser.SPELParser;
import com.circustar.mybatis_accessor.classInfo.DtoClassInfo;
import com.circustar.mybatis_accessor.listener.ExecuteTiming;
import com.circustar.mybatis_accessor.listener.event.IUpdateEvent;
import com.circustar.mybatis_accessor.listener.event.UpdateEventModel;
import com.circustar.mybatis_accessor.provider.command.IUpdateCommand;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.context.ApplicationContext;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UpdateExecuteExpressionEvent implements IUpdateEvent<UpdateEventModel> {
    private static SqlSessionFactory sqlSessionFactory = null;
    private void initSqlSessionFactory(ApplicationContext applicationContext) {
        String[] beanNamesForType = applicationContext.getBeanNamesForType(SqlSessionFactory.class);
        if(beanNamesForType.length > 0) {
            UpdateExecuteExpressionEvent.sqlSessionFactory = applicationContext.getBean(beanNamesForType[0], SqlSessionFactory.class);
        }
    }

    @Override
    public ExecuteTiming getDefaultExecuteTiming() {
        return ExecuteTiming.AFTER_UPDATE;
    }

    @Override
    public IUpdateCommand.UpdateType[] getDefaultUpdateTypes() {
        return new IUpdateCommand.UpdateType[]{IUpdateCommand.UpdateType.INSERT, IUpdateCommand.UpdateType.UPDATE
                , IUpdateCommand.UpdateType.DELETE};
    }

    @Override
    public void exec(UpdateEventModel model, IUpdateCommand.UpdateType updateType, DtoClassInfo dtoClassInfo
            , List<Object> dtoList, List<Object> entityList) {
        if(UpdateExecuteExpressionEvent.sqlSessionFactory == null) {
            initSqlSessionFactory(dtoClassInfo.getDtoClassInfoHelper().getApplicationContext());
        }
        if(UpdateExecuteExpressionEvent.sqlSessionFactory == null) {
            throw new RuntimeException("sqlSessionFactory not found in ApplicationContext");
        }
        final List<String> sqlExpressions = new ArrayList<>();
        for(int i = 0; i < entityList.size(); i++) {
            String sql = SPELParser.parseExpression(entityList.get(i), model.getUpdateParams().get(0)).toString();
            if(sql.contains("#{")) {
                sql = SPELParser.parseExpression(dtoList.get(i), sql).toString();
            }
            sqlExpressions.add(sql);
        }
        try(SqlSession sqlSession = sqlSessionFactory.openSession()) {
            try(Connection connection = sqlSession.getConnection()) {
                Statement statement = null;
                try {
                    statement = connection.createStatement();
                    for(String sql : sqlExpressions) {
                        statement.execute(sql);
                    }
                } finally {
                    if(statement != null) {
                        statement.close();
                    }
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
