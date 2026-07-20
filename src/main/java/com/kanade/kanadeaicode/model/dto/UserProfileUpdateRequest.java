package com.kanade.kanadeaicode.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户个人信息更新请求（仅允许修改昵称和头像）
 */
@Data
public class UserProfileUpdateRequest implements Serializable {

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    private static final long serialVersionUID = 1L;
}
