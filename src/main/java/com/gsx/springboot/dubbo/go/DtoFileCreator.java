package com.gsx.springboot.dubbo.go;

import cn.hutool.core.util.StrUtil;

import java.lang.reflect.Field;
import java.util.List;

/**
 * @author wanjiaju
 * @date 2020-11-03
 */
class DtoFileCreator {

    static String setPackage(String packageName) {
        return "package " + packageName;
    }

    static String setDtoImport() {
        return System.getProperty("line.separator") +
                System.getProperty("line.separator") +
                "import (" + System.getProperty("line.separator");
    }

    static String setDtoInit(String dtoName) {
        return System.getProperty("line.separator") +
                System.getProperty("line.separator") +
                "func init() {" +
                System.getProperty("line.separator") +
                "\thessian.RegisterPOJO(&" + dtoName + "{})" +
                System.getProperty("line.separator") +
                "}";
    }

    static String setDtoStruct(Class dtoClass, List<Class> dtoClassList, List<Class> enumClassList) {
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append(System.getProperty("line.separator"));
        contentBuilder.append(System.getProperty("line.separator"));
        contentBuilder.append("type ").append(dtoClass.getSimpleName()).append(" struct {");
        setDtoParam(contentBuilder, dtoClass, dtoClassList, enumClassList);
        contentBuilder.append(System.getProperty("line.separator"));
        contentBuilder.append("}");
        return contentBuilder.toString();
    }

    static void setDtoParam(StringBuilder contentBuilder, Class dtoClass, List<Class> dtoClassList, List<Class> enumClassList) {
        for (Field field : dtoClass.getDeclaredFields()) {
            if (!field.getName().equals("serialVersionUID")) {
                contentBuilder.append(System.getProperty("line.separator"));
                contentBuilder.append("\t").append(StrUtil.upperFirst(field.getName())).append("\t").append(Utils.getType(field.getGenericType(), dtoClassList, enumClassList, dtoClass, TypeEnum.DTO, null));
            }
        }
    }

    static String setJavaClassName(Class dtoClass) {
        return System.getProperty("line.separator") +
                System.getProperty("line.separator") +
                "func (" + dtoClass.getSimpleName() + ") JavaClassName() string {" +
                System.getProperty("line.separator") +
                "\treturn \"" + dtoClass.getName() + "\"" +
                System.getProperty("line.separator") +
                "}";
    }
}
