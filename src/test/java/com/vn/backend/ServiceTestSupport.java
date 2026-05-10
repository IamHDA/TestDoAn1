package com.vn.backend;

import com.vn.backend.dto.request.common.BaseFilterSearchRequest;
import com.vn.backend.dto.request.common.SearchRequest;
import com.vn.backend.utils.MessageUtils;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

public final class ServiceTestSupport {

    private ServiceTestSupport() {
    }

    public static MessageUtils mockMessageUtils() {
        MessageUtils messageUtils = Mockito.mock(MessageUtils.class);
        lenient().when(messageUtils.getMessage(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(messageUtils.getMessage(anyString(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        return messageUtils;
    }

    public static SearchRequest searchRequest(int pageNum, int pageSize) {
        SearchRequest request = new SearchRequest();
        request.setPageNum(String.valueOf(pageNum));
        request.setPageSize(String.valueOf(pageSize));
        return request;
    }

    public static <T> BaseFilterSearchRequest<T> filterRequest(T filters, int pageNum, int pageSize) {
        BaseFilterSearchRequest<T> request = new BaseFilterSearchRequest<>();
        request.setFilters(filters);
        request.setPagination(searchRequest(pageNum, pageSize));
        return request;
    }
}
