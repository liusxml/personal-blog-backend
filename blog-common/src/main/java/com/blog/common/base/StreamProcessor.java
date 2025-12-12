package com.blog.common.base;

import java.util.stream.Stream;

@FunctionalInterface
public interface StreamProcessor<T> {
    void process(Stream<T> stream);
}