package com.vn.backend.dto.request.common;


import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.annotation.NotAllowBlank;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.constants.AppConst.RegexConst;
import com.vn.backend.dto.response.common.PagingMeta;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.utils.BeanUtils;
import com.vn.backend.utils.MessageUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
@Setter
public class SearchRequest {

    @NotAllowBlank(message = MessageConst.REQUIRED_FIELD_EMPTY, fieldName = FieldConst.PAGE_NUM)
    @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.PAGE_NUM)
    private String pageNum;

    @AllowFormat(regex = RegexConst.INTEGER, message = MessageConst.INVALID_NUMBER_FORMAT, fieldName = FieldConst.PAGE_SIZE)
    private String pageSize;

    @Valid
    private List<SortField> sort;

    @Schema(hidden = true)
    public PagingMeta getPagingMeta() {
        // If pageSize is null, return PagingMeta without pagination but with sort
        if (pageSize == null || pageSize.trim().isEmpty()) {
            PagingMeta pagingMeta = new PagingMeta();

            // Handle multi-field sorting
            if (sort != null && !sort.isEmpty()) {
                pagingMeta.setSortFields(sort);
            }

            return pagingMeta;
        }

        // Normal pagination flow
        int size = Integer.parseInt(this.pageSize);
        if (size > 1000) {
            MessageUtils messageUtils = BeanUtils.getBean(MessageUtils.class);
            throw new AppException(MessageConst.VALUE_OUT_OF_RANGE,
                    messageUtils.getMessage(MessageConst.VALUE_OUT_OF_RANGE,
                            messageUtils.getMessage(FieldConst.PAGE_SIZE)), HttpStatus.BAD_REQUEST);
        }

        PagingMeta pagingMeta = new PagingMeta(Integer.valueOf(pageNum), size);

        // Handle multi-field sorting
        if (sort != null && !sort.isEmpty()) {
            pagingMeta.setSortFields(sort);
        }

        return pagingMeta;
    }
}