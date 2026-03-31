package com.blog.ai.service;

import com.blog.ai.api.dto.OpsCiEventDTO;
import com.blog.ai.api.vo.OpsCiEventVO;
import com.blog.ai.domain.entity.OpsCiEventEntity;
import com.blog.common.base.IBaseService;

/**
 * Ops CI 事件服务接口
 *
 * <p>提供 CI 事件的持久化和查询能力（继承 IBaseService 标准 CRUD）。</p>
 *
 * @author liusxml
 * @since 1.3.0
 */
public interface IOpsCiEventService extends IBaseService<OpsCiEventEntity, OpsCiEventVO, OpsCiEventDTO> {

    /**
     * 记录一条来自 GitHub Webhook 的 CI 事件。
     *
     * <p>Controller 调用此方法，无需手动操作 Mapper。</p>
     *
     * @param entity 已从 Webhook Payload 解析完成的实体
     */
    void record(OpsCiEventEntity entity);

    /**
     * 解析 GitHub Webhook 原始 Payload 并持久化 CI 事件。
     *
     * <p>
     * 将 JSON 解析逻辑封装在 Service 层，使 Controller 无需 try-catch（遵循规则 6.2）。
     * 解析失败时抛出 {@code BusinessException(SystemErrorCode.PARAM_ERROR)}。
     * </p>
     *
     * @param rawBody GitHub Webhook 请求的原始字节数组
     * @throws com.blog.common.exception.BusinessException 当 Payload JSON 格式非法时
     */
    void parseAndRecord(byte[] rawBody);
}
