package com.vn.backend.utils;

import com.vn.backend.dto.response.common.AppErrorResponse;
import com.vn.backend.dto.response.common.AppResponse;
import com.vn.backend.dto.response.common.ResponseListData;
import org.springframework.stereotype.Component;

@Component
public class ResponseUtils {

    /**
     * Return response success include data
     *
     * @param data
     * @return
     */
    public <T> AppResponse<T> success(T data) {
        return getResponse(true, data, null);
    }


    /**
     * Return response success include data
     *
     * @param data
     * @return
     */
    public <T> AppResponse<ResponseListData<T>> successListData(ResponseListData<T> data) {
        return getResponseListData(true, data, null);
    }

    /**
     * Return response error include errorCode and message
     *
     * @param success
     * @param data
     * @param errors
     * @param <T>
     * @return
     */
    private <T> AppResponse<T> getResponse(boolean success, T data, Object errors) {
        AppResponse response = AppResponse.builder().success(success).data(data).build();
        if (!success) {
            return new AppErrorResponse(response, errors);
        }
        return response;
    }

    /**
     * Return response error include errorCode and message
     *
     * @param success
     * @param data
     * @param errors
     * @param <T>
     * @return
     */
    private <T> AppResponse<ResponseListData<T>> getResponseListData(boolean success,
                                                                     ResponseListData<T> data, Object errors) {
        AppResponse response = AppResponse.builder().success(success).data(data.getContent()) // NOSONAR
                .paging(data.getPaging()).build(); // NOSONAR
        if (!success) {
            return new AppErrorResponse(response, errors); // NOSONAR
        }
        return response;
    }
}
