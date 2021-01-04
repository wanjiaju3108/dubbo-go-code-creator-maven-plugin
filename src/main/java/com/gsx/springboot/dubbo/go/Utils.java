package com.gsx.springboot.dubbo.go;

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author wanjiaju
 * @date 2020-10-27
 */
class Utils {

    static Map<String, String> readConfig(File configFile) throws FileNotFoundException {
        if (configFile == null) {
            System.out.println("没有配置文件");
        }
        assert configFile != null;
        InputStream in = new BufferedInputStream(new FileInputStream(configFile));
        Yaml props = new Yaml();
        Object config = props.loadAs(in, Map.class);
        return (Map<String, String>) config;
    }

    static List<String> runShell(String shStr) throws InterruptedException, IOException {
        List<String> strList = new ArrayList<>();
        Process process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", shStr}, null, null);
        InputStreamReader ir = new InputStreamReader(process.getInputStream());
        LineNumberReader input = new LineNumberReader(ir);
        String line;
        process.waitFor();
        while ((line = input.readLine()) != null) {
            strList.add(line);
        }
        return strList;
    }

    static String getType(Type paramType, List<Class> dtoClassList, List<Class> enumClassList, Class superClass, TypeEnum typeEnum, String dtoName) {
        String type;
        String typeName = "";
        if (paramType instanceof Class) {
            typeName = ((Class) paramType).getName();
        } else if (paramType instanceof ParameterizedType) {
            typeName = ((ParameterizedType) paramType).getRawType().getTypeName();
        }
        List<String> dtoClassNameList = dtoClassList.stream().map(Class::getName).collect(Collectors.toList());
        List<String> enumClassNameList = enumClassList.stream().map(Class::getName).collect(Collectors.toList());
        if (typeName.equals(List.class.getName())) {
            if (paramType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) paramType;
                type = "[]" + getType(parameterizedType.getActualTypeArguments()[0], dtoClassList, enumClassList, superClass, typeEnum, dtoName);
                return type;
            }
        } else if (typeName.equals(Map.class.getName())) {
            if (paramType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) paramType;
                type = "map[" + getType(parameterizedType.getActualTypeArguments()[0], dtoClassList, enumClassList, superClass, typeEnum, dtoName)
                        + "]" + getType(parameterizedType.getActualTypeArguments()[1], dtoClassList, enumClassList, superClass, typeEnum, dtoName);
                return type;
            }
        } else if (Commons.typeMap.get(getSimpleName(typeName)) != null) {
            type = Commons.typeMap.get(getSimpleName(typeName));
            return type;
        } else if (dtoClassNameList.contains(typeName)) {
            switch (typeEnum) {
                case DTO:
                    type = getSimpleName(typeName);
                    break;
                case PARAM:
                case RESULT:
                default:
                    type = "*" + dtoName + "." + getSimpleName(typeName);
            }
            Commons.hasThirdClassResult = true;
            return type;
        } else if (typeName.equals(Date.class.getName())) {
            type = "time.Time";
            Commons.hasTime = true;
            return type;
        } else if (enumClassNameList.contains(typeName)) {
            Commons.hasEnum = true;
            return "enums." + getSimpleName(typeName);
        } else {
            switch (typeEnum) {
                case DTO:
                    System.out.println("无法转换dto类:" + superClass + "-" + paramType);
                    break;
                case PARAM:
                    System.out.println("无法转换service方法:" + superClass + "-" + paramType);
                    break;
                case RESULT:
                    System.out.println("无法转换return类型:" + superClass + "-" + paramType);
                    break;
                default:
            }
            Commons.writeFile = false;
        }
        return "@err";
    }

    static String getSimpleName(String typeName) {
        return typeName.substring(typeName.lastIndexOf(".") + 1);
    }

    static String convertProviderName(String providerName) {
        StringBuilder newProvider = new StringBuilder();
        for (int i = 0; i < providerName.length(); i++) {
            char ch = providerName.charAt(i);
            if (ch >= 'a' && ch <= 'z') {
                newProvider.append(ch);
            } else if (ch >= 'A' && ch <= 'Z') {
                newProvider.append(ch);
            } else if (ch >= '0' && ch <= '9') {
                newProvider.append(ch);
            } else {
                newProvider.append('_');
            }
        }
        return newProvider.toString();
    }
}
