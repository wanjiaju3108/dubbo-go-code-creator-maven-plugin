package com.gsx.springboot.dubbo.go;

import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.gsx.springboot.dubbo.go.Commons.*;

/**
 * Goal which code-creator a timestamp file.
 *
 * @goal create
 * @phase process-sources
 * @requiresDependencyResolution runtime
 */

public class CodeCreator extends AbstractMojo {
    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject mavenProject;

    /**
     * The directory for compiled classes.
     *
     * @parameter default-value="${basedir}/src/main/resources/springboot-dubbo-go-code.yml"
     * @required
     * @readonly
     */
    private File configFile;

    /**
     * The directory for compiled classes.
     *
     * @parameter default-value="${project.artifactId}"
     * @required
     * @readonly
     */
    private String providerName;

    /**
     * The directory for compiled classes.
     *
     * @parameter default-value="${basedir}"
     * @required
     * @readonly
     */
    private File gitDir;

    @Override
    public void execute() {
        //编译
        try {
            clean();
            compile();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("开始生成go代码");
        Map<String, String> configMap;
        try {
            configMap = Utils.readConfig(configFile);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        String services = configMap.get("services");
        if (services == null) {
            System.out.println("没有配置services");
            return;
        }
        String goDirName = thirdDepositoryName + "/api/" + providerName;
        String packageName = configMap.get("package");
        String dtoPackageName = packageName + ".dto";
        String servicePackageName = packageName + ".service";
        String enumPackageName = packageName + ".enums";
        String dtoName = Utils.convertProviderName(providerName) + "_dto";
        String dtoDirName = goDirName + "/" + dtoName + "/";
        String serviceDirName = goDirName + "/service/";
        String enumDirName = goDirName + "/enums/";
        mkDir(goDirName, dtoDirName, serviceDirName, enumDirName);
        try {
            List<Class> allDtoClassList = getClasses(dtoPackageName);
            List<Class> serviceClassList = getClassesByName(servicePackageName, services);
            List<Class> enumClassList = getClasses(enumPackageName);
            dtoClassList = Lists.newArrayList();
            for (Class dtoClass : allDtoClassList) {
                writeFile = true;
                String fileName = dtoDirName + dtoClass.getSimpleName() + ".go";
                String content = writeDto(dtoClass, allDtoClassList, enumClassList, providerName, dtoName);
                if (writeFile) {
                    dtoClassList.add(dtoClass);
                    //写入文件
                    FileWriter out = new FileWriter(fileName);
                    out.write(content);
                    out.close();
                }
            }
            for (Class serviceClass : serviceClassList) {
                writeFile = true;
                String fileName = serviceDirName + serviceClass.getSimpleName() + ".go";
                String content = writeService(serviceClass, serviceClass.getMethods(), dtoClassList, enumClassList, packageName, dtoName);
                if (writeFile) {
                    FileWriter out = new FileWriter(fileName);
                    out.write(content);
                    out.close();
                }
            }
            for (Class enumClass : enumClassList) {
                mkEnumDir(enumDirName + enumClass.getSimpleName() + "Const/");
                String baseFileName = enumDirName + enumClass.getSimpleName() + ".go";
                writeEnumBase(new FileWriter(baseFileName), enumClass);
                String enumFileName = enumDirName + enumClass.getSimpleName() + "Const/" + enumClass.getSimpleName() + "Const.go";
                writeEnum(new FileWriter(enumFileName), enumClass);
            }
            String fileName = goDirName + ".go";
            writeBase(new FileWriter(fileName));
        } catch (ClassNotFoundException | IOException | DependencyResolutionRequiredException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * 从包package中获取所有的Class
     */
    private List<Class> getClasses(String packageName) throws ClassNotFoundException, IOException, DependencyResolutionRequiredException {
        // 第一个class类的集合
        List<Class> classes = new ArrayList<>();
        // 获取包的名字 并进行替换
        String packageDirName = packageName.replace('.', '/');
        Enumeration<URL> dirs;
        List<String> runtimeClasspathElements = mavenProject.getRuntimeClasspathElements();
        URL[] runtimeUrls = new URL[runtimeClasspathElements.size()];
        for (int i = 0; i < runtimeClasspathElements.size(); i++) {
            String element = runtimeClasspathElements.get(i);
            runtimeUrls[i] = new File(element).toURI().toURL();
        }
        URLClassLoader urlClassLoader = new URLClassLoader(runtimeUrls);
        dirs = urlClassLoader.getResources(packageDirName);
        // 循环迭代下去
        while (dirs.hasMoreElements()) {
            // 获取下一个元素
            URL url = dirs.nextElement();
            // 得到协议的名称
            String protocol = url.getProtocol();
            // 如果是以文件的形式保存在服务器上
            if ("file".equals(protocol)) {
                // 获取包的物理路径
                String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                // 以文件的方式扫描整个包下的文件 并添加到集合中
                findAndAddClassesInPackageByFile(packageName, filePath, urlClassLoader, classes, null);
            }
        }
        return classes;
    }

    private List<Class> getClassesByName(String packageName, String services) throws DependencyResolutionRequiredException, IOException, ClassNotFoundException {
        List<Class> classes = new ArrayList<>();
        // 获取包的名字 并进行替换
        String packageDirName = packageName.replace('.', '/');
        Enumeration<URL> dirs;
        Map<String, Boolean> serviceMaps = Arrays.stream(services.split(",")).collect(Collectors.toMap(Function.identity(), v -> true));
        List<String> runtimeClasspathElements = mavenProject.getRuntimeClasspathElements();
        URL[] runtimeUrls = new URL[runtimeClasspathElements.size()];
        for (int i = 0; i < runtimeClasspathElements.size(); i++) {
            String element = runtimeClasspathElements.get(i);
            runtimeUrls[i] = new File(element).toURI().toURL();
        }
        URLClassLoader urlClassLoader = new URLClassLoader(runtimeUrls);
        dirs = urlClassLoader.getResources(packageDirName);
        // 循环迭代下去
        while (dirs.hasMoreElements()) {
            // 获取下一个元素
            URL url = dirs.nextElement();
            // 得到协议的名称
            String protocol = url.getProtocol();
            // 如果是以文件的形式保存在服务器上
            if ("file".equals(protocol)) {
                // 获取包的物理路径
                String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                // 以文件的方式扫描整个包下的文件 并添加到集合中
                findAndAddClassesInPackageByFile(packageName, filePath, urlClassLoader, classes, serviceMaps);
            }
        }
        return classes;
    }

    /**
     * 以文件的形式来获取包下的所有Class
     */
    private void findAndAddClassesInPackageByFile(String packageName, String packagePath, URLClassLoader urlClassLoader, List<Class> classes, Map<String, Boolean> serviceMaps) throws ClassNotFoundException {
        // 获取此包的目录 建立一个File
        File dir = new File(packagePath);
        // 如果不存在或者 也不是目录就直接返回
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        // 如果存在 就获取包下的所有文件 包括目录
        // 自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
        File[] dirfiles = dir.listFiles(file -> file.getName().endsWith(".class"));
        // 循环所有文件
        assert dirfiles != null;
        for (File file : dirfiles) {
            String className = file.getName().substring(0, file.getName().length() - 6);
            if (serviceMaps == null || serviceMaps.getOrDefault(className, false)) {
                classes.add(urlClassLoader.loadClass(packageName + '.' + className));
            }
        }
    }

    private void mkDir(String goDirName, String dtoDirName, String serviceDirName, String enumsDirName) {
        File goFile = new File(goDirName);
        if (!goFile.exists()) {
            if (!goFile.mkdir()) {
                System.out.println("创建根文件夹失败");
                System.out.println(goDirName);
            }
        }
        File dtoFile = new File(dtoDirName);
        if (!dtoFile.exists()) {
            if (!dtoFile.mkdir()) {
                System.out.println("创建dto文件夹失败");
                System.out.println(dtoDirName);
            }
        }
        File serviceFile = new File(serviceDirName);
        if (!serviceFile.exists()) {
            if (!serviceFile.mkdir()) {
                System.out.println("创建service文件夹失败");
                System.out.println(serviceDirName);
            }
        }
        File enumsFile = new File(enumsDirName);
        if (!enumsFile.exists()) {
            if (!enumsFile.mkdir()) {
                System.out.println("创建enums文件夹失败");
                System.out.println(serviceDirName);
            }
        }
    }

    private void mkEnumDir(String enumDirName) {
        File file = new File(enumDirName);
        if (!file.exists()) {
            if (!file.mkdir()) {
                System.out.println("创建枚举文件夹失败");
                System.out.println(enumDirName);
            }
        }
    }

    private String writeDto(Class dtoClass, List<Class> dtoClassList, List<Class> enumClassList, String providerName, String dtoName) {
        hasTime = false;
        hasThirdClassResult = false;
        hasEnum = false;
        //设置package
        String packageStr = DtoFileCreator.setPackage(dtoName);
        //设置import
        String importStr = DtoFileCreator.setDtoImport();
        //设置init方法
        String initStr = DtoFileCreator.setDtoInit(dtoClass.getSimpleName());
        //设置struct类
        String struct = DtoFileCreator.setDtoStruct(dtoClass, dtoClassList, enumClassList);
        //设置java完整类名
        String javaClass = DtoFileCreator.setJavaClassName(dtoClass);
        if (hasTime) {
            importStr += "\t\"time\"" + System.getProperty("line.separator");
        }
        if (hasEnum) {
            importStr += "\t\"" + thirdDepositoryUrl + "/api/" + providerName + "/enums\"" + System.getProperty("line.separator");
        }

        importStr += "\thessian \"github.com/apache/dubbo-go-hessian2\"" + System.getProperty("line.separator") +
                ")";
        return packageStr + importStr + initStr + struct + javaClass;
    }

    private String writeService(Class serviceClass, Method[] methods, List<Class> dtoClassList, List<Class> enumClassList, String packageName, String dtoName) {
        hasTime = false;
        hasThirdClassResult = false;
        hasEnum = false;
        String providerObject = ServiceFileCreator.getProviderObject(serviceClass.getSimpleName());
        //设置package
        String packageStr = ServiceFileCreator.setPackage("service");
        //设置import
        String importStr = ServiceFileCreator.setServiceImport();
        String serviceStr = ServiceFileCreator.setServiceVar(serviceClass, providerObject);
        String serviceReferenctStr = ServiceFileCreator.setServiceReference(serviceClass, packageName);
        String serviceInitStr = ServiceFileCreator.setServiceInit(providerObject);
        String rpcServiceStr = ServiceFileCreator.setRpcService(serviceClass, methods, dtoClassList, enumClassList, dtoName);
        String methodStr = ServiceFileCreator.setMethod(serviceClass, providerObject, dtoClassList, enumClassList, dtoName);
        if (hasThirdClassResult) {
            importStr += "\t\"" + thirdDepositoryUrl + "/api/" + providerName + "/" + dtoName + "\"" + System.getProperty("line.separator");
        }
        if (hasTime) {
            importStr += "\t\"time\"" + System.getProperty("line.separator");
        }
        if (hasEnum) {
            importStr += "\t\"" + thirdDepositoryUrl + "/api/" + providerName + "/enums\"" + System.getProperty("line.separator");
        }
        importStr += ")";
        return packageStr + importStr + serviceStr + serviceReferenctStr + serviceInitStr + rpcServiceStr + methodStr;
    }

    private void writeEnumBase(FileWriter out, Class enumClass) throws IOException {
        String className = enumClass.getSimpleName();
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append("package enums").append(System.getProperty("line.separator"))
                .append(System.getProperty("line.separator"))
                .append("import (").append(System.getProperty("line.separator"))
                .append("\t\"" + thirdDepositoryUrl + "/api/").append(providerName).append("/enums/").append(className).append("Const\"").append(System.getProperty("line.separator"))
                .append("\thessian \"github.com/apache/dubbo-go-hessian2\"").append(System.getProperty("line.separator"))
                .append("\t\"strconv\"").append(System.getProperty("line.separator"))
                .append(")").append(System.getProperty("line.separator"))
                .append(System.getProperty("line.separator"))
                .append("func init() {").append(System.getProperty("line.separator"))
                .append("\thessian.RegisterJavaEnum(").append(className).append("(0))").append(System.getProperty("line.separator"))
                .append("}").append(System.getProperty("line.separator"))
                .append(System.getProperty("line.separator"))
                .append("type ").append(className).append(" hessian.JavaEnum").append(System.getProperty("line.separator"))
                .append(System.getProperty("line.separator"))
                .append("var ").append(StrUtil.lowerFirst(className)).append("Name = map[hessian.JavaEnum]string{").append(System.getProperty("line.separator"));
        for (Field field : enumClass.getFields()) {
            String filedName = field.getName();
            contentBuilder.append("\t").append(className).append("Const.").append(filedName).append(":\t").append("\"").append(filedName).append("\"").append(",").append(System.getProperty("line.separator"));
        }
        contentBuilder.append("}").append(System.getProperty("line.separator"))
                .append(System.getProperty("line.separator"))
                .append("var ").append(StrUtil.lowerFirst(className)).append("Value = map[string]hessian.JavaEnum{").append(System.getProperty("line.separator"));
        for (Field field : enumClass.getFields()) {
            String filedName = field.getName();
            contentBuilder.append("\t").append("\"").append(filedName).append("\"").append(":\t").append(className).append("Const.").append(filedName).append(",").append(System.getProperty("line.separator"));
        }
        contentBuilder.append("}").append(System.getProperty("line.separator"))
                .append(System.getProperty("line.separator"))
                .append("func (").append(className).append(") JavaClassName() string {").append(System.getProperty("line.separator"))
                .append("\treturn \"").append(enumClass.getName()).append("\"").append(System.getProperty("line.separator"))
                .append("}").append(System.getProperty("line.separator"))
                .append(System.getProperty("line.separator"))
                .append("func (e ").append(className).append(") String() string {").append(System.getProperty("line.separator"))
                .append("\ts, ok := ").append(StrUtil.lowerFirst(className)).append("Name[hessian.JavaEnum(e)]").append(System.getProperty("line.separator"))
                .append("\tif ok {").append(System.getProperty("line.separator"))
                .append("\t\treturn s").append(System.getProperty("line.separator"))
                .append("\t}").append(System.getProperty("line.separator"))
                .append(System.getProperty("line.separator"))
                .append("\treturn strconv.Itoa(int(e))").append(System.getProperty("line.separator"))
                .append("}").append(System.getProperty("line.separator"))
                .append(System.getProperty("line.separator"))
                .append("func (").append(className).append(") EnumValue(s string) hessian.JavaEnum {").append(System.getProperty("line.separator"))
                .append("\tv, ok := ").append(StrUtil.lowerFirst(className)).append("Value[s]").append(System.getProperty("line.separator"))
                .append("\tif ok {").append(System.getProperty("line.separator"))
                .append("\t\treturn v").append(System.getProperty("line.separator"))
                .append("\t}").append(System.getProperty("line.separator"))
                .append(System.getProperty("line.separator"))
                .append("\treturn hessian.InvalidJavaEnum").append(System.getProperty("line.separator"))
                .append("}").append(System.getProperty("line.separator"));
        out.write(contentBuilder.toString());
        out.close();
    }

    private void writeEnum(FileWriter out, Class enumClass) throws IOException {
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append("package ").append(enumClass.getSimpleName()).append("Const").append(System.getProperty("line.separator"))
                .append(System.getProperty("line.separator"))
                .append("import hessian \"github.com/apache/dubbo-go-hessian2\"").append(System.getProperty("line.separator"))
                .append(System.getProperty("line.separator"))
                .append("const (").append(System.getProperty("line.separator"));
        for (int i = 0; i < enumClass.getFields().length; i++) {
            contentBuilder.append("\t").append(enumClass.getFields()[i].getName());
            if (i == 0) {
                contentBuilder.append("\thessian.JavaEnum = iota");
            }
            contentBuilder.append(System.getProperty("line.separator"));
        }
        contentBuilder.append(")");
        out.write(contentBuilder.toString());
        out.close();
    }

    private void writeBase(FileWriter out) throws IOException {
        String contentBuilder = "package api" +
                System.getProperty("line.separator") +
                System.getProperty("line.separator") +
                "import (" +
                System.getProperty("line.separator") +
                "\t_ \"" + thirdDepositoryUrl + "/api/" + providerName + "/service\"" +
                System.getProperty("line.separator") +
                ")";
        out.write(contentBuilder);
        out.close();
    }

    private void compile() throws IOException, InterruptedException {
        System.out.println("开始compile");
        Utils.runShell("mvn compile").forEach(System.out::println);
    }

    private void clean() throws IOException, InterruptedException {
        System.out.println("开始clean");
        Utils.runShell("mvn clean").forEach(System.out::println);
    }

}
