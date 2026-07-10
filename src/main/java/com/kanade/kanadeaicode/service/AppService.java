package com.kanade.kanadeaicode.service;

import com.kanade.kanadeaicode.model.dto.AppQueryRequest;
import com.kanade.kanadeaicode.model.entity.User;
import com.kanade.kanadeaicode.model.vo.AppVo;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.kanade.kanadeaicode.model.entity.App;
import jakarta.servlet.http.HttpServletRequest;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author <a href="">Kanade</a>
 */
public interface AppService extends IService<App> {

    AppVo getAppVO(App app);

    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    List<AppVo> getAppVOList(List<App> appList);


    /**
     * 聊天生成代码
     * @param appId 应用ID
     * @param message 用户输入的消息
     * @param loginUser 登录用户
     * @return 生成的代码
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);


    /**
     * 网站部署
     * @param appId 应用ID
     * @param loginUser 登录用户
     * @return 部署结果url地址
     */
    String deploy(Long appId, User loginUser, HttpServletRequest request);

    void generateAppScreenshotAsync(Long appId, String appUrl , String sessionId);
}
