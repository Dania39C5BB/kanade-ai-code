package com.kanade.kanadeaicode.controller;

import cn.hutool.core.bean.BeanUtil;
import com.kanade.kanadeaicode.annotation.AuthCheck;
import com.kanade.kanadeaicode.common.BaseResponse;
import com.kanade.kanadeaicode.common.DeleteRequest;
import com.kanade.kanadeaicode.common.ResultUtils;
import com.kanade.kanadeaicode.constant.UserConstant;
import com.kanade.kanadeaicode.exception.BusinessException;
import com.kanade.kanadeaicode.exception.ErrorCode;
import com.kanade.kanadeaicode.exception.ThrowUtils;
import com.kanade.kanadeaicode.manager.CosManager;
import com.kanade.kanadeaicode.model.dto.*;
import com.kanade.kanadeaicode.model.vo.LoginUserVo;
import com.kanade.kanadeaicode.model.vo.UserVo;
import com.mybatisflex.core.paginate.Page;
import com.qcloud.cos.model.ObjectMetadata;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Autowired;
import com.kanade.kanadeaicode.model.entity.User;
import com.kanade.kanadeaicode.service.UserService;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 用户 控制层。
 *
 * @author <a href="">Kanade</a>
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private CosManager cosManager;

    /**
     * 用户注册
     *
     * @param userRegisterRequest 用户注册请求
     * @return 注册结果
     */
    @PostMapping("register")
    public BaseResponse<Long> register(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    /**
     * 用户注册
     *
     * @param userLoginRequest 用户登录请求
     * @return 注册结果
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVo> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVo loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }


    @GetMapping("/get/login")
    public BaseResponse<LoginUserVo> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }


    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }


    /**
     * 创建用户
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        // 默认密码 12345678
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVo> getUserVOById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVo(user));
    }

    /**
     * 删除用户
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     */
    @PostMapping("/updateUser")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页获取用户封装列表（仅管理员）
     *
     * @param userQueryRequest 查询请求参数
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVo>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = userQueryRequest.getPageNum();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(Page.of(pageNum, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        // 数据脱敏
        Page<UserVo> userVOPage = new Page<>(pageNum, pageSize, userPage.getTotalRow());
        List<UserVo> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

    /**
     * 当前登录用户更新个人信息（昵称、头像）
     *
     * @param request 个人信息更新请求
     * @param httpRequest HTTP 请求
     * @return 更新后的用户信息
     */
    @PostMapping("/update/profile")
    public BaseResponse<LoginUserVo> updateProfile(@RequestBody UserProfileUpdateRequest request,
                                                    HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);
        // 更新字段
        if (request.getUserName() != null) {
            loginUser.setUserName(request.getUserName());
        }
        if (request.getUserAvatar() != null) {
            loginUser.setUserAvatar(request.getUserAvatar());
        }
        loginUser.setEditTime(LocalDateTime.now());
        boolean result = userService.updateById(loginUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 更新 session 中的用户信息
        httpRequest.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, loginUser);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 上传头像到腾讯云 COS
     *
     * @param file 头像图片文件
     * @return 头像的 COS 访问 URL
     */
    @PostMapping("/upload/avatar")
    public BaseResponse<String> uploadAvatar(@RequestParam("file") MultipartFile file,
                                              HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "头像文件不能为空");

        String contentType = file.getContentType();
        ThrowUtils.throwIf(contentType == null
                || (!contentType.equals("image/jpeg")
                    && !contentType.equals("image/png")
                    && !contentType.equals("image/gif")
                    && !contentType.equals("image/webp")),
                ErrorCode.PARAMS_ERROR, "仅支持 jpg/png/gif/webp 格式的图片");

        ThrowUtils.throwIf(file.getSize() > 2 * 1024 * 1024,
                ErrorCode.PARAMS_ERROR, "头像文件不能超过 2MB");

        String originalFilename = file.getOriginalFilename();
        String suffix = ".jpg";
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        try {
            String cosKey = generateAvatarKey(suffix);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(contentType);

            String avatarUrl = cosManager.uploadFile(cosKey, file.getInputStream(), metadata);
            ThrowUtils.throwIf(avatarUrl == null, ErrorCode.OPERATION_ERROR, "头像上传失败");

            // 更新当前登录用户的头像字段
//            User loginUser = userService.getLoginUser(httpRequest);
//            loginUser.setUserAvatar(avatarUrl);
//            loginUser.setEditTime(LocalDateTime.now());
//            boolean result = userService.updateById(loginUser);
//            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
//            // 同步更新 session
//            httpRequest.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, loginUser);

            return ResultUtils.success(avatarUrl);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "头像上传失败: " + e.getMessage());
        }
    }

    /**
     * 生成头像的对象存储键
     * 格式：/avatar/2026/07/13/uuid.jpg
     */
    private String generateAvatarKey(String suffix) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = UUID.randomUUID().toString().substring(0, 8) + suffix;
        return String.format("/avatar/%s/%s", datePath, fileName);
    }

}
