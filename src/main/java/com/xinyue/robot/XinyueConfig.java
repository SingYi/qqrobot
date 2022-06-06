package com.xinyue.robot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class XinyueConfig {

    private static File adminUserFile; // 管理员文件
    private static File sensitiveWordFile; // 敏感词文件
    private static File operationUserFile; // 运营人员文件

    public static ArrayList<String> adminList; // 管理员列表
    public static ArrayList<String> sensitiveWordList; // 管理员列表
    public static ArrayList<String> operationList; // 运营人员列表

    /**
     * 初始化管理员
     */
    public static void InitAdminList() {
        adminUserFile = XinyueRobotMain.INSTANCE.resolveDataFile("admin.txt");
        adminList = new ArrayList<>();
        try {
            LogI("读取管理员配置");
            readDataToList(adminUserFile, adminList);
        } catch (IOException e) {
            LogE("读取管理员配置失败");
        }
    }

    /**
     * 初始化运营人员配置
     */
    public static void InitOperationList() {
        operationUserFile = XinyueRobotMain.INSTANCE.resolveDataFile("operation.txt");
        operationList = new ArrayList<>();
        if (operationUserFile.exists() == false) {
            // 创建文件
            try {
                operationUserFile.createNewFile();
            } catch (IOException e) {
                LogE("创建运营人员列表失败!");
            }
        }
        try {
            LogI("读取运营人员配置");
            readDataToList(operationUserFile, operationList);
        } catch (IOException e) {
            LogE("读取运营人员配置失败");
        }
    }

    /**
     * 初始化运营人员配置
     */
    public static void InitSensitiveWordList() {
        sensitiveWordFile = XinyueRobotMain.INSTANCE.resolveDataFile("sensitiveWord.txt");
        sensitiveWordList = new ArrayList<>();
        if (sensitiveWordFile.exists() == false) {
            // 创建文件
            try {
                sensitiveWordFile.createNewFile();
            } catch (IOException e) {
                LogE("创建敏感词列表失败!");
            }
        }
        try {
            LogI("读取敏感词列表");
            readDataToList(sensitiveWordFile, sensitiveWordList);
        } catch (IOException e) {
            LogE("读取敏感词列表失败");
        }
    }

    /**
     * 添加运营人员
     */
    public static void AddOperation(String operation) {
        String pattern = "^-?[0-9]+$";
        if (Pattern.matches(pattern, operation) == false) {
            LogE("请输入纯数字");
            return;
        }
        operationList.add(operation);
        try {
            writeListToFile(operationList, operationUserFile);
        } catch (IOException e) {

        }
    }

    /**
     * 删除运营人员
     */
    public static void DeleteOperation(String operation) {
        String pattern = "^-?[0-9]+$";
        if (Pattern.matches(pattern, operation) == false) {
            LogE("请输入纯数字");
            return;
        }

        try {
            operationList.remove(operation);
            writeListToFile(operationList, operationUserFile);
        } catch (IOException e) {

        }
    }

    /**
     * 读取文件数据到内存
     */
    private static void readDataToList(File data, ArrayList<String> list) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(data);
        InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String text;
        while ((text = bufferedReader.readLine()) != null) {
            list.add(text);
        }
        bufferedReader.close();
        inputStreamReader.close();
        fileInputStream.close();
    }

    /**
     * 写入内存数据到文件
     */
    private static void writeListToFile(ArrayList<String> list, File data) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(data);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
        for (String s : list)
            outputStreamWriter.write(s + "\n");
        outputStreamWriter.close();
        fileOutputStream.close();
    }

    static void LogI(String message) {
        XinyueRobotMain.INSTANCE.getLogger().info("[xinyue]:" + message);
    }

    static void LogE(String message) {
        XinyueRobotMain.INSTANCE.getLogger().error("[xinyue]:" + message);
    }
}
