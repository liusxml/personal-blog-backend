package com.blog.frameworktest.dto;

import com.blog.common.base.Identifiable;
import lombok.Data;

@Data
public class TestDTO implements Identifiable<Long> {
    private Long id;
    private String name;

    @Override
    public Long getId() {
        return null;
    }
}