package com.gsx.springboot.dubbo.go;

import lombok.Data;
import lombok.experimental.Accessors;

import java.lang.reflect.Type;

/**
 * @author wanjiaju
 * @date 2020-10-30
 */
@Data
@Accessors(chain = true)
class Param {
    Type paramType;
    Class paramClass;
}
