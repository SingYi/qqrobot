package com.xinyue.robot;

import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.event.EventChannel;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.events.FriendMessageEvent;
import net.mamoe.mirai.event.events.GroupMessageEvent;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

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

    private static ArrayList<String> adminList; // 管理员列表
    private static ArrayList<String> sensitiveWordList; // 管理员列表

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
//        eventChannel.subscribeAlways(GroupMessageEvent.class, g -> {
//            //监听群消息
//            String message = g.getMessage().contentToString();
//            if (message == "哈哈") {
//
//            }
//            getLogger().info(g.getMessage().contentToString());
//            getLogger().info("收到消息");
//
//        });

    }

    /**
     * 敏感词检测
     * */
    private void detectSensitiveWords(@NotNull GroupMessageEvent event)
    {
        String message = event.getMessage().contentToString();
        if (SensitiveWordUtil.contains(message)) {
            // 包含敏感词
            Set<String> word = SensitiveWordUtil.getSensitiveWord(message);
            event.getGroup().sendMessage("包含敏感词 : " + word.toString());
        }
    }

    /**
     * 初始化配置文件
     */
    private void initConfig() {
        LogI("初始化配置文件");
        adminUserFile = XinyueRobotMain.INSTANCE.resolveDataFile("admin.txt");
        sensitiveWordFile = XinyueRobotMain.INSTANCE.resolveDataFile("sensitiveWord.txt");
        adminList = new ArrayList<>();
        sensitiveWordList = new ArrayList<>();
        try {
            LogI("读取管理员配置");
            readDataToList(adminUserFile, adminList);
            LogI("读取敏感词配置");
            readDataToList(sensitiveWordFile, sensitiveWordList);
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
