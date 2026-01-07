package com.blog.file.api.service;

import com.blog.file.api.dto.PreSignedUrlRequest;
import com.blog.file.api.vo.FileVO;
import com.blog.file.api.vo.PreSignedUploadVO;

import java.util.Optional;

/**
 * 文件服务接口
 *
 * <p>
 * 定义文件管理的核心业务方法。
 * 遵循依赖倒置原则，Service 层实现此接口。
 * </p>
 *
 * @author liusxml
 * @since 1.0.0
 */
public interface IFileService {

    /**
     * 生成预签名上传 URL（官方推荐方案）
     *
     * <p>
     * 流程：
     * <ol>
     * <li>验证文件元数据</li>
     * <li>生成 fileKey</li>
     * <li>预创建文件记录（状态=待上传）</li>
     * <li>生成预签名 URL</li>
     * <li>返回给前端</li>
     * </ol>
     * </p>
     *
     * @param request 预签名 URL 请求
     * @return 预签名上传 VO
     */
    PreSignedUploadVO generateUploadUrl(PreSignedUrlRequest request);

    /**
     * 确认上传完成（前端回调）
     *
     * <p>
     * 前端上传成功后调用此接口，更新文件状态为"已完成"。
     * </p>
     *
     * @param fileId 文件 ID
     */
    void confirmUpload(Long fileId);

    /**
     * 根据 ID 获取文件 VO
     *
     * @param id 文件 ID
     * @return 文件 VO（Optional）
     */
    Optional<FileVO> getVoById(Long id);

    /**
     * 根据 ID 删除文件
     *
     * <p>
     * 软删除 + 调用存储删除。
     * </p>
     *
     * @param id 文件 ID
     * @return 是否成功
     */
    boolean deleteById(Long id);
}
