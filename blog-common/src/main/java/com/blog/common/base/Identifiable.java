package com.blog.common.base;

import java.io.Serializable;

public interface Identifiable<T extends Serializable> {
    T getId();
}