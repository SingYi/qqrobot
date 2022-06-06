package com.xinyue.robot;

import net.mamoe.mirai.Bot;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.contact.PermissionDeniedException;
import net.mamoe.mirai.contact.Stranger;
import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.EventChannel;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.events.FriendMessageEvent;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageSource;
import net.mamoe.mirai.message.data.PlainText;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.xml.crypto.Data;

/**
 * 使用 Java 请把
 * {@code /src/main/resources/META-INF.services/net.mamoe.mirai.console.plugin.jvm.JvmPlugin}
 * 文件内容改成 {@code org.example.mirai.plugin.JavaPluginMain} <br/>
 * 也就是当前主类全类名
 * <p>
 * 使用 Java 可以把 kotlin 源集删除且不会对项目有影响
 * <p>
 * 在 {@code settings.gradle.kts} 里改构建的插件名称、依赖库和插件版本
 * <p>
 * 在该示例下的 {@link JvmPluginDescription} 修改插件名称，id 和版本等
 * <p>
 * 可以使用 {@code src/test/kotlin/RunMirai.kt} 在 IDE 里直接调试，
 * 不用复制到 mirai-console-loader 或其他启动器中调试
 */
public final class XinyueRobotMain extends JavaPlugin {
    public static final XinyueRobotMain INSTANCE = new XinyueRobotMain();

    private XinyueRobotMain() {
        // 构建机器人的一些说明
        super(new JvmPluginDescriptionBuilder("com.xinyue.sdk.robot", "1.0.0")
                .info("")
                .name("心悦机器人")
                .author("Sans")
                .build()
        );
    }

    private static File adminUserFile; // 管理员文件
    private static File sensitiveWordFile; // 敏感词文件
    private static File operationUserFile; // 运营人员文件

    private static ArrayList<String> adminList; // 管理员列表
    private static ArrayList<String> sensitiveWordList; // 管理员列表
    private static ArrayList<String> operationList; // 运营人员列表

    @Override
    public void onEnable() {
        LogI("onEnable");
        // 1. 初始化配置文件
        initConfig();
        // 2. 初始化敏感词
        initSensitiveWord();

        // 3. 监听群组消息
        EventChannel<Event> eventChannel = GlobalEventChannel.INSTANCE.parentScope(this);
        eventChannel.subscribeAlways(GroupMessageEvent.class, this::detectSensitiveWords);

    }

    /**
     * 敏感词检测
     */
    private void detectSensitiveWords(@NotNull GroupMessageEvent event) {
        String message = event.getMessage().contentToString();

        // 包含敏感词
        Set<String> word = SensitiveWordUtil.getSensitiveWord(message);
        if (word != null && !word.isEmpty()) {
            try {
                // 撤回消息
                MessageSource.recall(event.getSource());

                String sendMessage = String.format("用户 [%s](%s) 发送敏感词句 [%s] \n 敏感词为: %s",
                        event.getSender().getId(),
                        event.getSender().getNick(),
                        message,
                        word
                );

                sendOperationMessage(event, sendMessage);
            } catch (PermissionDeniedException e) {
                sendOperationMessage(event, "没有权限撤回消息");
            }

            // 禁言
            try {
                event.getSender().mute(3600);
                At at = new At(event.getSender().getId());
                PlainText text = new PlainText("请勿发送违规词汇,如有疑问或误封,请联系管理员.");
                MessageChain chain = at.plus(text);
                event.getGroup().sendMessage(chain);
            } catch (PermissionDeniedException e) {
                LogE("没有权限禁言");
            }


        }
    }

    /**
     * 向运营人员发送消息
     */
    private void sendOperationMessage(@NotNull GroupMessageEvent event, String message) {

        for (String operation : operationList) {
            if (operation.isEmpty()) {
                continue;
            }
            long qq = Long.valueOf(operation);

            Date date = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateString = formatter.format(date);
            String sendMessage = String.format("%s - 群 [%s](%s) : %s",
                    dateString,
                    event.getGroup().getId(),
                    event.getGroup().getName(),
                    message
            );

            try {
                Bot.getInstances().get(0).getFriend(qq).sendMessage(sendMessage);
            } catch (Exception e) {
                LogE(e.toString());
            }
        }

    }

    /**
     * 初始化配置文件
     */
    private void initConfig() {
        LogI("初始化配置文件");
        adminUserFile = XinyueRobotMain.INSTANCE.resolveDataFile("admin.txt");
        sensitiveWordFile = XinyueRobotMain.INSTANCE.resolveDataFile("sensitiveWord.txt");
        operationUserFile = XinyueRobotMain.INSTANCE.resolveDataFile("operation.txt");
        adminList = new ArrayList<>();
        sensitiveWordList = new ArrayList<>();
        operationList = new ArrayList<>();
        try {
            LogI("读取管理员配置");
            readDataToList(adminUserFile, adminList);
            LogI("读取敏感词配置");
            readDataToList(sensitiveWordFile, sensitiveWordList);
            LogI("读取管理员配置");
            readDataToList(operationUserFile, operationList);
        } catch (IOException e) {
            getLogger().info("读取数据失败！");
        }
        LogI("初始化配置文件完毕");
    }

    /**
     * 初始化敏感词
     */
    private void initSensitiveWord() {
        LogI("初始化敏感词");
        Set<String> tmp = new HashSet<>(sensitiveWordList);
        SensitiveWordUtil.init(tmp);
        LogI("初始化敏感词完毕");
    }

    /**
     * 读取文件数据到内存
     */
    private void readDataToList(File data, ArrayList<String> list) throws IOException {
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
    private void writeListToFile(ArrayList<String> list, File data) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(data);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
        for (String s : list)
            outputStreamWriter.write(s + "\n");
        outputStreamWriter.close();
        fileOutputStream.close();
    }

    void LogI(String message) {
        getLogger().info("[xinyue]:" + message);
    }

    void LogE(String message) {
        getLogger().error("[xinyue]:" + message);
    }
}
