package com.vn.backend.dto.request.common;

import com.vn.backend.annotation.AllowFormat;
import com.vn.backend.annotation.AllowLength;
import com.vn.backend.annotation.NotAllowBlank;
import com.vn.backend.constants.AppConst;
import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.constants.AppConst.RegexConst;
import com.vn.backend.enums.SortDirection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.data.domain.Sort;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SortField implements Serializable {

    @AllowLength(max = 50, message = MessageConst.VALUE_TOO_LONG, fieldName = FieldConst.SORT_KEY)
    @NotAllowBlank(message = MessageConst.REQUIRED_FIELD_EMPTY, fieldName = AppConst.FieldConst.SORT_KEY)
    @AllowFormat(regex = RegexConst.ALPHANUMERIC, message = MessageConst.INVALID_CHARACTER, fieldName = FieldConst.SORT_KEY)
    @Schema(description = "Field name to sort by", example = "createdAt")
    private String key;

    @NotAllowBlank(message = MessageConst.REQUIRED_FIELD_EMPTY, fieldName = FieldConst.SORT_ORDER)
    @Schema(description = "Sort order", example = "desc", allowableValues = {"asc", "desc"})
    private String order;

    public Sort.Order toOrder() {
        return new Sort.Order(
                SortDirection.fromString(order) == SortDirection.DESC ? Sort.Direction.DESC
                        : Sort.Direction.ASC, key
        );
    }
}
