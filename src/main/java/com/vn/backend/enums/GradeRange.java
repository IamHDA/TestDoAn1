package com.vn.backend.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GradeRange {
    RANGE_0_1(0, 1),
    RANGE_1_2(1, 2),
    RANGE_2_3(2, 3),
    RANGE_3_4(3, 4),
    RANGE_4_5(4, 5),
    RANGE_5_6(5, 6),
    RANGE_6_7(6, 7),
    RANGE_7_8(7, 8),
    RANGE_8_9(8, 9),
    RANGE_9_10(9, 10);

    private final int min;
    private final int max;

    public static GradeRange fromGrade(Double grade) {
        if (grade == null) return null;
        for (GradeRange gr : values()) {
            if (grade >= gr.min && grade < gr.max) {
                return gr;
            }
        }
        return RANGE_9_10;
    }
}
