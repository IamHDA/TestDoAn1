package com.vn.backend.utils;


import com.vn.backend.enums.QuestionOrderMode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;

public class SearchUtils {

    private SearchUtils() {
    }

    public static String getLikeValue(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        return "%" + value + "%";
    }

    public static String getSearchValue(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        return value;
    }

    public static Sort getSortQuestion(QuestionOrderMode orderMode) {
        if (orderMode == null) {
            return Sort.unsorted();
        }

        return switch (orderMode) {
            case RANDOM -> Sort.unsorted();
            case SEQUENTIAL -> Sort.by(Sort.Direction.ASC, "orderIndex");
            case DIFFICULTY_ASC -> Sort.by(Sort.Direction.ASC, "difficultyLevel");
            case DIFFICULTY_DESC -> Sort.by(Sort.Direction.DESC, "difficultyLevel");
        };
    }
}
