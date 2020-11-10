package com.circustar.mvcenhance.common.response;

import org.springframework.validation.FieldError;

import java.util.List;

public class DefaultServiceResult<T> implements IServiceResult<T> {
    private T data;
    private List<FieldError> errorList;
    private PageInfo pageInfo;
    @Override
    public void setData(T data) {
        this.data = data;
    }


    @Override
    public T getData() {
        return data;
    }

    @Override
    public void setFieldErrorList(List<FieldError> errorList) {
        this.errorList = errorList;
    }

    @Override
    public List<FieldError> getFieldErrorList() {
        return errorList;
    }

    @Override
    public void addFieldErrorList(List<FieldError> errorList) {
        if(this.errorList == null) {
            this.errorList = errorList;
        } else {
            this.errorList.addAll(errorList);
        }
    }


    @Override
    public void setPageInfo(PageInfo pageInfo) {
        this.pageInfo = pageInfo;
    }

    @Override
    public PageInfo getPageInfo() {
        return pageInfo;
    }
}
