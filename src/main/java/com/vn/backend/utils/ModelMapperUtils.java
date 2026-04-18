package com.vn.backend.utils;


import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

import java.util.Date;

@Slf4j
public class ModelMapperUtils {

    private static final ModelMapper MODEL_MAPPER;

    static {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        MODEL_MAPPER = modelMapper;
    }

    private ModelMapperUtils() {
    }

    public static <S, D> D mapTo(final S source, final Class<D> destClass) {
        long startDate = new Date().getTime();
        log.info("Convert source: {}", source);
        D dest = MODEL_MAPPER.map(source, destClass);
        long endDate = new Date().getTime();
        log.info("Convert destination: {}", dest);
        log.info("convert time: {} ms", (endDate - startDate));
        return dest;
    }
}

