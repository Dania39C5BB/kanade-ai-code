package com.kanade.kanadeaicode.service;

import com.kanade.kanadeaicode.model.dto.ChatHistoryQueryRequest;
import com.kanade.kanadeaicode.model.entity.User;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.kanade.kanadeaicode.model.entity.ChatHistory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author <a href="">Kanade</a>
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    boolean addChatHistory(Long appId , String message , String messageType , Long userId);

    boolean deleteByAppId(Long appId);

    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);


    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);

    /**
     * 加载对话历史到内存
     * @param appId 应用ID
     * @param chatMemory
     * @param maxCount 最大多少条
     * @return
     */
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);
}
