package com.gsx.springboot.dubbo.go;

import org.apache.maven.plugin.AbstractMojo;

import java.io.File;
import java.io.IOException;

import static com.gsx.springboot.dubbo.go.Commons.thirdDepositoryName;

/**
 * Goal which code-commit a timestamp file.
 *
 * @goal commit
 * @phase process-sources
 */

public class CodeCommit extends AbstractMojo {

    /**
     * The directory for compiled classes.
     *
     * @parameter default-value="${basedir}"
     * @required
     * @readonly
     */
    private File gitDir;

    /**
     * The directory for compiled classes.
     *
     * @parameter default-value="${project.artifactId}"
     * @required
     * @readonly
     */
    private String providerName;

    @Override
    public void execute() {
        try {
            commitSubmodule();
            pushSubmodule();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void commitSubmodule() throws IOException, InterruptedException {
        System.out.println("开始commit submodule");
        Utils.runShell("cd " + gitDir.getPath() + "/" + thirdDepositoryName + " \n" +
                "git add .\n" +
                "git commit -m \"" + providerName + "dubbo-go代码生成\"\n" +
                "git push").forEach(System.out::println);
    }

    private void pushSubmodule() throws IOException, InterruptedException {
        System.out.println("开始push submodule");
        Utils.runShell("cd " + gitDir.getPath() + "/" + thirdDepositoryName + " \n" +
                "git push").forEach(System.out::println);
    }
}
