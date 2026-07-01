package com.kanade.kanadeaicode.core.handler;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.kanade.kanadeaicode.ai.model.message.*;
import com.kanade.kanadeaicode.constant.AppConstant;
import com.kanade.kanadeaicode.core.builder.VueProjectBuilder;
import com.kanade.kanadeaicode.model.entity.User;
import com.kanade.kanadeaicode.model.enums.ChatHistoryMessageTypeEnum;
import com.kanade.kanadeaicode.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * JSON 消息流处理器
 * 处理 VUE_PROJECT 类型的复杂流式响应，包含工具调用信息
 */
@Slf4j
@Component
public class JsonMessageStreamHandler {


    @Resource
    private VueProjectBuilder vueProjectBuilder;


    /**
     * 处理 TokenStream（VUE_PROJECT）
     * 解析 JSON 消息并重组为完整的响应格式
     * <p>
     * 使用 concatMap 替代 map，对于 TOOL_EXECUTED 类型的消息，
     * 将大块文件内容按行拆分为多个小块逐个发出，模拟流式输出效果。
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @return 处理后的流
     */
    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               long appId, User loginUser) {
        // 收集数据用于生成后端记忆格式
        StringBuilder chatHistoryStringBuilder = new StringBuilder();
        // 用于跟踪已经见过的工具ID，判断是否是第一次调用
        Set<String> seenToolIds = new HashSet<>();
        return originFlux
                // concatMap: 一个输入可以产出多个输出（一对多），保持顺序
                .concatMap(chunk -> {
                    // 解析每个 JSON 消息块，返回 Flux<String> 以支持拆分输出
                    return handleJsonMessageChunk(chunk, chatHistoryStringBuilder, seenToolIds);
                })
                .doOnComplete(() -> {
                    // 流式响应完成后，添加 AI 消息到对话历史
                    String aiResponse = chatHistoryStringBuilder.toString();
                    chatHistoryService.addChatHistory(appId, aiResponse, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                    String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + appId;
                    vueProjectBuilder.buildProjectAsync(projectPath);
                })
                .doOnError(error -> {
                    // 如果AI回复失败，也要记录错误消息
                    String errorMessage = "AI回复失败: " + error.getMessage();
                    chatHistoryService.addChatHistory(appId, errorMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                });
    }

    /**
     * 解析并收集 TokenStream 数据
     *
     * @param chunk                    原始 JSON 消息块
     * @param chatHistoryStringBuilder 对话历史收集器（完整文本一次性追加）
     * @param seenToolIds              已见过的工具 ID 集合
     * @return 拆分后的 Flux，对于普通消息返回 Mono，对于工具执行结果按行拆分返回
     */
    private Flux<String> handleJsonMessageChunk(String chunk, StringBuilder chatHistoryStringBuilder, Set<String> seenToolIds) {
        // 解析 JSON
        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        StreamMessageTypeEnum typeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        if (typeEnum == null) {
            log.error("无法识别的消息类型, chunk: {}", chunk);
            return Flux.empty();
        }
        switch (typeEnum) {
            case AI_RESPONSE -> {
                AiResponseMessage aiMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                String data = aiMessage.getData();
                // 直接拼接响应（文本流本身就是小块的，不需要再拆）
                chatHistoryStringBuilder.append(data);
                return StrUtil.isNotEmpty(data) ? Flux.just(data) : Flux.empty();
            }
            case TOOL_REQUEST -> {
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                String toolId = toolRequestMessage.getId();
                // 检查是否是第一次看到这个工具 ID
                if (toolId != null && !seenToolIds.contains(toolId)) {
                    // 第一次调用这个工具，记录 ID 并完整返回工具信息
                    seenToolIds.add(toolId);
                    return Flux.just("\n\n[选择工具] 写入文件\n\n");
                } else {
                    // 不是第一次调用这个工具，直接返回空
                    return Flux.empty();
                }
            }
            case TOOL_EXECUTED -> {
                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                JSONObject jsonObject = JSONUtil.parseObj(toolExecutedMessage.getArguments());
                String relativeFilePath = jsonObject.getStr("relativeFilePath");
                String suffix = FileUtil.getSuffix(relativeFilePath);
                String content = jsonObject.getStr("content");
                // 构建完整文本，一次性追加到对话历史记录
                String result = String.format("""
                        [工具调用] 写入文件 %s
                        ```%s
                        %s
                        ```
                        """, relativeFilePath, suffix, content);
                String fullOutput = String.format("\n\n%s\n\n", result);
                chatHistoryStringBuilder.append(fullOutput);

                // 随机分块 + 随机延迟，模拟真人打字的不均匀节奏
                List<String> chunks = splitIntoRandomChunks(fullOutput, 10, 30);
                if (chunks.isEmpty()) {
                    return Flux.empty();
                }
                return Flux.fromIterable(chunks)
                        .concatMap(token ->
                                Mono.just(token)
                                        .delayElement(Duration.ofMillis(
                                                ThreadLocalRandom.current().nextLong(20, 60)
                                        ))
                        );
            }
            default -> {
                log.error("不支持的消息类型: {}", typeEnum);
                return Flux.empty();
            }
        }
    }

    /**
     * 将文本按 Unicode 码点安全地随机拆分为大小不等的字符块
     * <p>
     * 每次随机取 [minSize, maxSize] 个码点组成一个块发出。
     * 使用 {@link String#codePointAt(int)} 逐个遍历码点，
     * 确保不会在 UTF-16 代理对中间切断。
     * 随机分块 + 上游的随机延迟一起模拟真人打字的不均匀节奏。
     *
     * @param text    完整文本
     * @param minSize 每块最少码点数
     * @param maxSize 每块最多码点数
     * @return 随机拆分后的字符块列表
     */
    private List<String> splitIntoRandomChunks(String text, int minSize, int maxSize) {
        List<String> result = new ArrayList<>();
        if (StrUtil.isEmpty(text)) {
            return result;
        }
        int totalLength = text.length();
        StringBuilder builder = new StringBuilder();
        int offset = 0;
        // 预先生成本轮的 chunk 目标大小
        int targetSize = ThreadLocalRandom.current().nextInt(minSize, maxSize + 1);
        int codePointCount = 0;

        while (offset < totalLength) {
            int codePoint = text.codePointAt(offset);
            builder.appendCodePoint(codePoint);
            codePointCount++;

            if (codePointCount >= targetSize) {
                result.add(builder.toString());
                builder.setLength(0);
                codePointCount = 0;
                // 下一轮随机目标大小
                targetSize = ThreadLocalRandom.current().nextInt(minSize, maxSize + 1);
            }
            offset += Character.charCount(codePoint);
        }
        // 末尾不足一轮的余量
        if (builder.length() > 0) {
            result.add(builder.toString());
        }
        return result;
    }
}

