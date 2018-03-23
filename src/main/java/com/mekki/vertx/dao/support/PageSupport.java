package com.mekki.vertx.dao.support;

import java.util.List;

/**
 * Created by Mekki on 2018/3/23.
 */
public class PageSupport<E> {
    private Long total;

    private Integer count;

    private List<E> elements;

    private Integer pageNum;

    private Integer page;

    private Integer size;

    private Integer startRow;

    private Integer endRow;

    public PageSupport(Integer page, Integer size) {
        this.page = page;
        this.size = size;

//        if (page <= 0 || size <= 0) {
//            throw new
//        }
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public List<E> getElements() {
        return elements;
    }

    public void setElements(List<E> elements) {
        this.elements = elements;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getStartRow() {
        return startRow;
    }

    public void setStartRow(Integer startRow) {
        this.startRow = startRow;
    }

    public Integer getEndRow() {
        return endRow;
    }

    public void setEndRow(Integer endRow) {
        this.endRow = endRow;
    }
}
