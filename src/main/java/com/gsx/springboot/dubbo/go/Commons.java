package com.gsx.springboot.dubbo.go;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wanjiaju
 * @date 2020-11-03
 */
class Commons {

    static boolean hasTime;

    static boolean hasThirdClassResult;

    static boolean hasEnum;

    static List<Class> dtoClassList;

    static boolean writeFile;

    static String thirdDepositoryName = "springboot-dubbo-go";

    static String thirdDepositoryUrl = "git.xxx.com/springboot-dubbo-go";

    static String thirdDepositoryGitUrl = "git@git.xxx.com:springboot-dubbo-go.git";

    static Map<String, String> typeMap = new HashMap<String, String>() {{
        put(Byte.class.getSimpleName(), "int8");
        put(byte.class.getSimpleName(), "int8");
        put(Short.class.getSimpleName(), "int16");
        put(short.class.getSimpleName(), "int16");
        put(Integer.class.getSimpleName(), "int32");
        put(int.class.getSimpleName(), "int32");
        put(Long.class.getSimpleName(), "int64");
        put(long.class.getSimpleName(), "int64");
        put(Boolean.class.getSimpleName(), "bool");
        put(boolean.class.getSimpleName(), "bool");
        put(Float.class.getSimpleName(), "float32");
        put(float.class.getSimpleName(), "float32");
        put(Double.class.getSimpleName(), "float64");
        put(double.class.getSimpleName(), "float64");
        put(String.class.getSimpleName(), "string");
        put(List.class.getSimpleName(), "[]interface{}");
        put(Map.class.getSimpleName(), "[]interface{}");
    }};

    static Map<String, String> envBranchMap = new HashMap<String, String>() {{
        put("test", "test");
        put("prod", "master");
    }};
}
