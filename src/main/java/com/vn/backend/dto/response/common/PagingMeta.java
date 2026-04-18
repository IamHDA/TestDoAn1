package com.vn.backend.dto.response.common;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.vn.backend.dto.request.common.SortField;
import com.vn.backend.enums.SortDirection;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handle paging for a list of records
 */
@Getter
@Setter
@NoArgsConstructor
@JsonPropertyOrder({"pageNum", "pageSize", "totalPages", "totalRows"})
public class PagingMeta implements Serializable {

    @Serial
    private static final long serialVersionUID = 2405172041950251807L;

    /**
     * Total Record, default 0
     */
    private long totalRows;

    private long totalPages;

    @JsonIgnore
    private long realTotal;

    @JsonIgnore
    private boolean exceeding = false;

    /**
     * Current page number (1-based) - Optional, null means unpaged
     */
    private Integer pageNum;

    /**
     * Number of records on a page - Optional, null means unpaged
     */
    private Integer pageSize;

    /**
     * Number of setting limit
     */
    @JsonIgnore
    private int limitSetting;

    /**
     * Index of first record on current page
     */
    @JsonIgnore
    private int from;

    /**
     * Index of last record on current page
     */
    @JsonIgnore
    private int to;


    /**
     * Multiple sort fields with individual directions - Optional
     */
    @JsonIgnore
    private List<SortField> sortFields;

    /**
     * Create new object with current page number and total records on a page
     */
    public PagingMeta(Integer pageNum, Integer pageSize) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.calculateFromTo();
    }

    /**
     * Create with single sort field
     */
    public PagingMeta(Integer pageNum, Integer pageSize, String sortBy, String sortType) {
        this(pageNum, pageSize);
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            this.addSort(sortBy, sortType);
        }
    }

    /**
     * Create with multiple sort fields
     */
    public PagingMeta(Integer pageNum, Integer pageSize, List<SortField> sortFields) {
        this(pageNum, pageSize);
        this.sortFields = sortFields != null ? new ArrayList<>(sortFields) : null;
    }

    // Static factory methods
    public static PagingMeta of(Integer pageNum, Integer pageSize) {
        return new PagingMeta(pageNum, pageSize);
    }

    public static PagingMeta of(Integer pageNum, Integer pageSize, String field, String direction) {
        return new PagingMeta(pageNum, pageSize, field, direction);
    }

    public static PagingMeta of(Integer pageNum, Integer pageSize, List<SortField> sortFields) {
        return new PagingMeta(pageNum, pageSize, sortFields);
    }

    /**
     * Create un-paged instance (no pagination)
     */
    public static PagingMeta unPaged() {
        return new PagingMeta(null, null);
    }

    /**
     * Create un-paged with sorting
     */
    public static PagingMeta unPagedWithSort(String field, String direction) {
        return new PagingMeta(null, null, field, direction);
    }

    /**
     * Create un-paged with multiple sorts
     */
    public static PagingMeta unPagedWithSorts(List<SortField> sortFields) {
        return new PagingMeta(null, null, sortFields);
    }

    /**
     * Create sort-only instance (with default pagination)
     */
    public static PagingMeta sortOnly(String field, String direction) {
        return new PagingMeta(1, 20, field, direction);
    }

    /**
     * Parse sort string like "field1:desc,field2:asc,field3"
     */
    public static PagingMeta parseSort(Integer pageNum, Integer pageSize, String sortString) {
        PagingMeta pagingMeta = new PagingMeta(pageNum, pageSize);

        if (sortString == null || sortString.trim().isEmpty()) {
            return pagingMeta;
        }

        String[] sortParts = sortString.split(",");
        for (String part : sortParts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                String[] fieldDirection = trimmed.split(":");
                String field = fieldDirection[0].trim();
                String direction =
                        fieldDirection.length > 1 ? fieldDirection[1].trim() : SortDirection.ASC.getValue();
                pagingMeta.addSort(field, direction);
            }
        }

        return pagingMeta;
    }

    // Convenience methods for adding sorts
    public PagingMeta addSort(String field, String direction) {
        if (field != null && !field.trim().isEmpty()) {
            if (this.sortFields == null) {
                this.sortFields = new ArrayList<>();
            }
            this.sortFields.add(new SortField(field.trim(), direction));
        }
        return this;
    }

    public PagingMeta addSortAsc(String field) {
        return addSort(field, SortDirection.ASC.getValue());
    }

    public PagingMeta addSortDesc(String field) {
        return addSort(field, SortDirection.DESC.getValue());
    }

    public PagingMeta clearSorts() {
        this.sortFields = null;
        return this;
    }

    // Remove duplicates and keep the first occurrence
    public PagingMeta deduplicateSorts() {
        if (this.sortFields != null && !this.sortFields.isEmpty()) {
            Set<String> seen = new HashSet<>();
            this.sortFields = this.sortFields.stream()
                    .filter(sort -> seen.add(sort.getKey()))
                    .collect(Collectors.toList());
        }
        return this;
    }

    // Backward compatibility methods
    @JsonIgnore
    public boolean hasSort() {
        return sortFields != null && !sortFields.isEmpty();
    }

    @JsonIgnore
    public boolean isPaged() {
        return pageNum != null && pageSize != null && pageNum > 0 && pageSize > 0;
    }

    @JsonIgnore
    public int getOffset() {
        if (!isPaged()) {
            return 0;
        }
        return pageSize * (pageNum - 1);
    }

    public void setTotalRows(long totalRows) {
        this.totalRows = totalRows;
        if (this.realTotal == 0) {
            this.setRealTotal(totalRows);
        }
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
        this.calculateFromTo();
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
        this.calculateFromTo();
    }

    private void calculateFromTo() {
        if (isPaged()) {
            this.from = pageSize * (pageNum - 1) + 1;
            this.to = from + pageSize - 1;
        } else {
            this.from = 0;
            this.to = 0;
        }
    }

    /**
     * Convert to Spring Pageable with multiple sorts
     */
    public Pageable toPageable() {
        // Handle un-paged cases
        if (!isPaged()) {
            return hasSort() ? Pageable.unpaged(buildSort()) : Pageable.unpaged();
        }

        // Handle paged cases
        if (hasSort()) {
            return PageRequest.of(pageNum - 1, pageSize, buildSort());
        } else {
            return PageRequest.of(pageNum - 1, pageSize);
        }
    }

    /**
     * Build Sort object from multiple sort fields
     */
    private Sort buildSort() {
        if (sortFields == null || sortFields.isEmpty()) {
            return Sort.unsorted();
        }

        List<Sort.Order> orders = sortFields.stream()
                .map(SortField::toOrder)
                .toList();

        return Sort.by(orders);
    }

    // Utility methods for common sorting patterns
    public PagingMeta sortByCreatedAtDesc() {
        return addSortDesc("createdAt");
    }
}
