package com.kanade.kanadeaicode.service;

import com.kanade.kanadeaicode.model.dto.ChatHistoryQueryRequest;
import com.kanade.kanadeaicode.model.entity.User;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.kanade.kanadeaicode.model.entity.ChatHistory;

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
}
