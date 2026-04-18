package com.vn.backend.enums;

import com.vn.backend.constants.AppConst.FieldConst;
import com.vn.backend.constants.AppConst.MessageConst;
import com.vn.backend.exceptions.AppException;
import com.vn.backend.utils.BeanUtils;
import com.vn.backend.utils.MessageUtils;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum SortDirection {

    ASC("asc"),
    DESC("desc");

    private final String value;

    SortDirection(String value) {
        this.value = value;
    }

    public static SortDirection fromString(String direction) {
        if (direction == null) {
            return ASC;
        }
        return switch (direction.toLowerCase().trim()) {
            case "desc", "descending", "d" -> DESC;
            case "asc", "ascending", "a" -> ASC;
            default -> {
                MessageUtils messageUtils = BeanUtils.getBean(MessageUtils.class);
                throw new AppException(MessageConst.VALUE_OUT_OF_RANGE,
                        messageUtils.getMessage(MessageConst.VALUE_OUT_OF_RANGE,
                                messageUtils.getMessage(FieldConst.SORT_ORDER)), HttpStatus.BAD_REQUEST);
            }
        };
    }
}