package com.circustar.mybatis_accessor.annotation.on_change;

import com.circustar.mybatis_accessor.annotation.after_update.ExecuteTiming;
import com.circustar.mybatis_accessor.classInfo.DtoClassInfo;
import com.circustar.mybatis_accessor.provider.command.IUpdateCommand;

import java.util.List;

public interface IOnChangeExecutor {
    default ExecuteTiming getExecuteTiming() {return ExecuteTiming.BEFORE_UPDATE;}
    IUpdateCommand.UpdateType[] getUpdateTypes();
    void exec(IUpdateCommand.UpdateType updateType, DtoClassInfo dtoClassInfo
            , Object newDto, Object oldDto, String[] params);
}
