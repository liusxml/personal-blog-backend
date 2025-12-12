package com.blog.frameworktest.dto;

import com.blog.common.base.Identifiable;
import lombok.Data;

import java.io.Serializable;

@Data
public class TestDTO implements Identifiable<Long>, Serializable {
    private Long id;
    private String name;

    @Override
    public Long getId() {
        return null;
    }
}