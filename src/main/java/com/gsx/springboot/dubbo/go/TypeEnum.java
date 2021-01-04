package com.gsx.springboot.dubbo.go;

import lombok.Getter;

/**
 * @author wanjiaju
 * @date 2020-11-23
 */
public enum TypeEnum {

    DTO(0),
    PARAM(1),
    RESULT(2),;

    @Getter
    private int code;

    TypeEnum(int code) {
        this.code = code;
    }
}
