package com.cosx.knowengine.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(long page, long size, long total, long pages, List<T> records) {

    public static <S, T> PageResponse<T> from(IPage<S> source, Function<S, T> mapper) {
        return new PageResponse<>(
                source.getCurrent(),
                source.getSize(),
                source.getTotal(),
                source.getPages(),
                source.getRecords().stream().map(mapper).toList()
        );
    }
}
