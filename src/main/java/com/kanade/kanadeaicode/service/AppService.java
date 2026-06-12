package com.kanade.kanadeaicode.service;

import com.kanade.kanadeaicode.model.dto.AppQueryRequest;
import com.kanade.kanadeaicode.model.vo.AppVo;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.kanade.kanadeaicode.model.entity.App;

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
}
