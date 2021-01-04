package com.gsx.springboot.dubbo.go;

import cn.hutool.core.util.StrUtil;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author wanjiaju
 * @date 2020-11-03
 */
class ServiceFileCreator {

    static String getProviderObject(String parent) {
        String[] array = parent.split("RpcService");
        return array[0] + "Provider";
    }

    static String setPackage(String packageName) {
        return "package " + packageName;
    }

    static String setServiceImport() {
        return System.getProperty("line.separator") +
                System.getProperty("line.separator") +
                "import (" +
                System.getProperty("line.separator") +
                "\t\"context\"" +
                System.getProperty("line.separator") +
                "\t\"github.com/apache/dubbo-go/config\"" +
                System.getProperty("line.separator");
    }

    static String setServiceVar(Class serviceClass, String providerObject) {
        return System.getProperty("line.separator") +
                System.getProperty("line.separator") +
                "var " + providerObject + " = new(" + serviceClass.getSimpleName() + ")";
    }

    static String setServiceReference(Class serviceClass, String packageName) {
        return System.getProperty("line.separator") +
                System.getProperty("line.separator") +
                "func (u *" + serviceClass.getSimpleName() + ") Reference() string {" +
                System.getProperty("line.separator") +
                "\treturn \"" + packageName + ".service." + serviceClass.getSimpleName() + "\"" +
                System.getProperty("line.separator") +
                "}";
    }

    static String setServiceInit(String providerObject) {
        return System.getProperty("line.separator") +
                System.getProperty("line.separator") +
                "func init() {" +
                System.getProperty("line.separator") +
                "\tconfig.SetConsumerService(" + providerObject + ")" +
                System.getProperty("line.separator") +
                "}";
    }

    static String setRpcService(Class serviceClass, Method[] methods, List<Class> dtoClassList, List<Class> enumClassList, String dtoName) {
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append(System.getProperty("line.separator"));
        contentBuilder.append(System.getProperty("line.separator"));
        contentBuilder.append("type ").append(serviceClass.getSimpleName()).append(" struct {");
        contentBuilder.append(System.getProperty("line.separator"));
        for (Method method : methods) {
            contentBuilder.append("\tGo").append(StrUtil.upperFirst(method.getName())).append(" func(ctx context.Context, req []interface{}) (").append(getReturnType(method, dtoClassList, enumClassList, serviceClass, dtoName)).append(", error) `dubbo:\"").append(method.getName()).append("\"`");
            contentBuilder.append(System.getProperty("line.separator"));
        }
        contentBuilder.append("}");
        return contentBuilder.toString();
    }

    static String getReturnType(Method method, List<Class> dtoClassList, List<Class> enumClassList, Class serviceClass, String dtoName) {
        String type = Utils.getType(method.getGenericReturnType(), dtoClassList, enumClassList, serviceClass, TypeEnum.RESULT, dtoName);
        if ("@".equals(type)) {
            System.out.println("无法转换方法返回值类型:" + serviceClass.getName() + "-" + method.getReturnType().getName());
            return "@err";
        }
        return type;
    }

    static String setMethod(Class serviceClass, String providerObject, List<Class> dtoClassList, List<Class> enumClassList, String dtoName) {
        StringBuilder contentBuilder = new StringBuilder();
        if (!checkRepeat(serviceClass.getMethods())) {
            Commons.writeFile = false;
            System.out.println(serviceClass + "中有重复方法名，golang不支持重载，无法生成.go文件");
        } else {
            for (Method method : serviceClass.getMethods()) {
                contentBuilder.append(System.getProperty("line.separator"));
                contentBuilder.append(System.getProperty("line.separator"));
                contentBuilder.append("func (").append(serviceClass.getSimpleName()).append(") ").append(StrUtil.upperFirst(method.getName())).append("(");
                setParamTypes(contentBuilder, method, dtoClassList, enumClassList, serviceClass, dtoName);
                contentBuilder.append(") ").append(getReturnType(method, dtoClassList, enumClassList, serviceClass, dtoName)).append(" {");
                contentBuilder.append(System.getProperty("line.separator"));
                contentBuilder.append("\t").append(getResult(method, dtoClassList, enumClassList, serviceClass)).append(", err := ").append(providerObject).append(".Go").append(StrUtil.upperFirst(method.getName())).append("(context.TODO(), []interface{}{");
                setParams(contentBuilder, method);
                contentBuilder.append("})");
                contentBuilder.append(System.getProperty("line.separator"));
                contentBuilder.append("\tif err != nil {");
                contentBuilder.append(System.getProperty("line.separator"));
                contentBuilder.append("\t}");
                contentBuilder.append(System.getProperty("line.separator"));
                contentBuilder.append("\treturn ").append(getResult(method, dtoClassList, enumClassList, serviceClass));
                contentBuilder.append(System.getProperty("line.separator"));
                contentBuilder.append("}");
            }
        }
        return contentBuilder.toString();
    }

    static boolean checkRepeat(Method[] array) {
        Set<String> set = Arrays.stream(array).map(Method::getName).collect(Collectors.toSet());
        return set.size() == array.length;
    }

    static void setParamTypes(StringBuilder contentBuilder, Method method, List<Class> dtoClassList, List<Class> enumClassList, Class serviceClass, String dtoName) {
        List<Param> paramList = Arrays.stream(method.getGenericParameterTypes()).map(type -> new Param().setParamType(type)).collect(Collectors.toList());
        for (int i = 0; i < method.getParameterTypes().length; i++) {
            paramList.get(i).setParamClass(method.getParameterTypes()[i]);
        }
        int i = 0;
        for (Param param : paramList) {
            if (i > 0) {
                contentBuilder.append(", ");
            }
            contentBuilder.append("arg").append(i++).append(" ").append(getMethodParamType(param, dtoClassList, enumClassList, serviceClass, dtoName));
        }
    }

    static String getMethodParamType(Param param, List<Class> dtoClassList, List<Class> enumClassList, Class serviceClass, String dtoName) {
        String type = Utils.getType(param.getParamType(), dtoClassList, enumClassList, serviceClass, TypeEnum.PARAM, dtoName);
        if ("@".equals(type)) {
            System.out.println("无法转换方法参数类型:" + serviceClass.getName() + "-" + param.getParamClass().getName());
            return "@err";
        }
        return type;
    }

    static void setParams(StringBuilder contentBuilder, Method method) {
        contentBuilder.append(StringUtils.join(Arrays.stream(method.getParameters()).map(Parameter::getName).collect(Collectors.toList()), ","));
    }

    static String getResult(Method method, List<Class> dtoClassList, List<Class> enumClassList, Class serviceClass) {
        Class returnClass = method.getReturnType();
        List<String> dtoClassNameList = dtoClassList.stream().map(Class::getName).collect(Collectors.toList());
        List<String> enumClassNameList = enumClassList.stream().map(Class::getSimpleName).collect(Collectors.toList());
        if (returnClass.equals(List.class)) {
            return "result";
        } else if (returnClass.equals(Map.class)) {
            return "result";
        } else if (Commons.typeMap.get(returnClass.getSimpleName()) != null) {
            return "result";
        } else if (dtoClassNameList.contains(returnClass.getName())) {
            Commons.hasThirdClassResult = true;
            return StrUtil.lowerFirst(method.getReturnType().getSimpleName());
        } else if (returnClass.equals(Date.class)) {
            Commons.hasTime = true;
            return "result";
        } else if (enumClassNameList.contains(returnClass.getName())) {
            Commons.hasEnum = true;
            return "result";
        } else {
            Commons.writeFile = false;
            System.out.println("无法转换结果类型:" + serviceClass + "-" + returnClass);
        }
        return "@err";
    }
}
