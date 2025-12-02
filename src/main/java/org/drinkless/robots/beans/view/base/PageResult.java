package org.drinkless.robots.beans.view.base;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Setter
@Getter
public class PageResult<T> implements Serializable {
    private List<T> list;
    private long total;
    private int pageNum;
    private int pageSize;

    public PageResult() {}

    public PageResult(List<T> list, long total, int pageNum, int pageSize) {
        this.list = list;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }
}

