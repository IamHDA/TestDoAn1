package com.vn.backend.unit;

import com.vn.backend.dto.request.common.SearchRequest;
import com.vn.backend.utils.MessageUtils;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

final class ServiceTestSupport {

    private ServiceTestSupport() {
    }

    static MessageUtils mockMessageUtils() {
        MessageUtils messageUtils = Mockito.mock(MessageUtils.class);
        lenient().when(messageUtils.getMessage(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(messageUtils.getMessage(anyString(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        return messageUtils;
    }

    static SearchRequest searchRequest(int pageNum, int pageSize) {
        SearchRequest request = new SearchRequest();
        request.setPageNum(String.valueOf(pageNum));
        request.setPageSize(String.valueOf(pageSize));
        return request;
    }
}
