package com.circustar.mybatis_accessor.provider.command;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.circustar.mybatis_accessor.error.UpdateTargetNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

public class UpdateWrapperCommand implements IUpdateCommand {

    private static UpdateWrapperCommand batchCommand = new UpdateWrapperCommand();
    public static UpdateWrapperCommand getInstance() {
        return batchCommand;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public <T extends Collection> boolean update(IService service, T collection, Object option) throws Exception {
        for(Object var1 : collection) {
            boolean result = service.update((Wrapper) var1);
            if(!result) {
                throw  new UpdateTargetNotFoundException("updateTragetNotFound");
            }
        }
        return true;
    }
}
