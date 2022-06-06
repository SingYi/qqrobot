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

    @Override
    public void onEnable() {
        LogI("onEnable");
        // 1. 初始化配置文件
        initConfig();
        // 2. 初始化敏感词工具
        initSensitiveWord();

        // 3. 监听群组消息
        EventChannel<Event> eventChannel = GlobalEventChannel.INSTANCE.parentScope(this);
        // 检测敏感词
        eventChannel.subscribeAlways(GroupMessageEvent.class, this::detectSensitiveWords);
        // 检测指令
        eventChannel.subscribeAlways(FriendMessageEvent.class, this::command);
    }

    /**
     * 检测指令
     */
    private void command(@NotNull FriendMessageEvent event) {
        String message = event.getMessage().contentToString();
        String senderId = String.valueOf(event.getSender().getId());
        LogI("发送人: ? " + senderId);
        if (!message.startsWith("/")) {
            return;
        }

        String[] commands = message.split(" ");
        if (commands == null || commands.length < 1) {
            return;
        }

        String command = commands[0];
        switch (command) {
            case "/addOperation":
            case "/添加运营人员":
                if (commands.length < 2)
                    break;
                if (!iSAdmin(senderId)) {
                    LogE("非管理员触发指令.");
                    break;
                }
                XinyueConfig.AddOperation(commands[1]);
                event.getUser().sendMessage("添加运营人员完毕.");
                break;
            case "/reloadAdmin":
            case "/重载管理员":
                if (!iSAdmin(senderId)) {
                    LogE("非管理员触发指令.");
                    break;
                }
                XinyueConfig.InitAdminList();
                event.getUser().sendMessage("重新加载运营人员完毕.");
                break;
            case "/showOperation":
            case "/查看运营人员":
                if (!iSAdmin(senderId)) {
                    LogE("非管理员触发指令.");
                    break;
                }
                event.getUser().sendMessage(XinyueConfig.operationList.toString());
                break;
            case "/deleteOperation":
            case "/删除运营人员":
                if (!iSAdmin(senderId)) {
                    LogE("非管理员触发指令.");
                    break;
                }
                XinyueConfig.DeleteOperation(commands[1]);
                event.getUser().sendMessage("删除运营人员完毕.");
                break;
            case "/reloadSensitiveWord":
            case "/重载屏蔽词":
            case "/重载敏感词":
                if (iSAdmin(senderId)) {
                    XinyueConfig.InitSensitiveWordList();
                    initSensitiveWord();
                    event.getUser().sendMessage("重新加载屏蔽词完毕.");
                } else  {
                    LogE("非管理员触发指令.");
                }
                break;
            default:
                event.getUser().sendMessage("未知指令 :" + message);
                break;
        }
    }

    boolean iSAdmin(String adminId) {
        return XinyueConfig.adminList.contains(adminId);
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
            mute(event);
        }
    }

    /**
     * 向运营人员发送消息
     */
    private void sendOperationMessage(@NotNull GroupMessageEvent event, String message) {
        for (String operation : XinyueConfig.operationList) {
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
     * 禁言
     */
    private void mute(@NotNull GroupMessageEvent event) {
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

    /**
     * 初始化配置文件
     */
    private void initConfig() {
        LogI("初始化配置文件");
        // 初始化管理员
        XinyueConfig.InitAdminList();
        XinyueConfig.InitOperationList();
        XinyueConfig.InitSensitiveWordList();
        LogI("初始化配置文件完毕");
    }

    /**
     * 初始化敏感词
     */
    private void initSensitiveWord() {
        LogI("初始化敏感词");
        Set<String> tmp = new HashSet<>(XinyueConfig.sensitiveWordList);
        SensitiveWordUtil.init(tmp);
        LogI("初始化敏感词完毕");
    }

    void LogI(String message) {
        getLogger().info("[xinyue]:" + message);
    }

    void LogE(String message) {
        getLogger().error("[xinyue]:" + message);
    }
}
