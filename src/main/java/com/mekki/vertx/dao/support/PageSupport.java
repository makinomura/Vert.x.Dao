package com.mekki.vertx.dao.support;

import java.util.List;

/**
 * Created by Mekki on 2018/3/23.
 * 分页信息
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

    private String orderBy;

    public PageSupport(Integer page, Integer size) {
        this.page = page;
        this.size = size;

        if (page <= 0 || size <= 0) {
            throw new RuntimeException("Wrong arguments");
        }

        startRow = (page - 1) * size;
    }

    public PageSupport(Integer page, Integer size, String orderBy) {
        this(page, size);
        this.orderBy = orderBy;
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

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    @Override
    public String toString() {
        return "PageSupport{" +
            "total=" + total +
            ", count=" + count +
            ", elements={" + (elements != null ? elements.stream().map(Object::toString).reduce(String::concat).orElse("") : "") +
            "}, pageNum=" + pageNum +
            ", page=" + page +
            ", size=" + size +
            ", startRow=" + startRow +
            ", endRow=" + endRow +
            ", orderBy='" + orderBy + '\'' +
            '}';
    }
}
