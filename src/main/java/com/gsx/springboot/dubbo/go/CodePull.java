package com.gsx.springboot.dubbo.go;

import org.apache.maven.plugin.AbstractMojo;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static com.gsx.springboot.dubbo.go.Commons.*;

/**
 * Goal which code-pull a timestamp file.
 *
 * @goal pull
 * @phase process-sources
 */

public class CodePull extends AbstractMojo {

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
     * @parameter default-value="${basedir}"
     * @required
     * @readonly
     */
    private File gitDir;

    @Override
    public void execute() {
        try {
            Map<String, String> configMap;
            try {
                configMap = Utils.readConfig(configFile);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            String env = configMap.get("env");
            System.out.println("环境：" + env);
            if (checkSubModule()) {
                System.out.println("有submodule checkout并pull");
            } else {
                System.out.println("无submodule clone并init");
                shellClone();
                shellInit();
            }
            shellPull(env);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    /**
     * 校验是否有submodule 有则返回true 否则返回false
     *
     * @return
     */
    private boolean checkSubModule() {
        File subDirFile = new File(gitDir.getAbsolutePath() + "/" + thirdDepositoryName, ".git");
        return subDirFile.exists();
    }

    private void shellClone() throws IOException, InterruptedException {
        Utils.runShell("git submodule add " + thirdDepositoryGitUrl).forEach(System.out::println);
    }

    private void shellInit() throws IOException, InterruptedException {
        Utils.runShell("git submodule update --init").forEach(System.out::println);
    }

    private void shellPull(String env) throws IOException, InterruptedException {
        String branch = envBranchMap.get(env);
        if (branch == null) {
            System.out.println("环境配置错误");
        }
        String command = "cd " + thirdDepositoryName + "\n"
                + "git checkout -f " + branch + "\n"
                + "git pull";
        Utils.runShell(command).forEach(System.out::println);
    }
}
