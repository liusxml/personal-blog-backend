package com.blog.ai.service.impl;

import com.blog.ai.api.dto.OpsCiEventDTO;
import com.blog.ai.api.vo.OpsCiEventVO;
import com.blog.ai.domain.entity.OpsCiEventEntity;
import com.blog.ai.infrastructure.converter.OpsCiEventConverter;
import com.blog.ai.infrastructure.mapper.OpsCiEventMapper;
import com.blog.ai.service.IOpsCiEventService;
import com.blog.common.base.BaseServiceImpl;
import com.blog.common.exception.BusinessException;
import com.blog.common.exception.SystemErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Ops CI 事件服务实现
 *
 * @author liusxml
 * @since 1.3.0
 */
@Slf4j
@Service
public class OpsCiEventServiceImpl
        extends BaseServiceImpl<OpsCiEventMapper, OpsCiEventEntity, OpsCiEventVO, OpsCiEventDTO, OpsCiEventConverter>
        implements IOpsCiEventService {

    private final OpsCiEventMapper opsCiEventMapper;
    private final ObjectMapper objectMapper;

    public OpsCiEventServiceImpl(OpsCiEventConverter converter, OpsCiEventMapper mapper, ObjectMapper objectMapper) {
        super(converter);
        this.opsCiEventMapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void record(OpsCiEventEntity entity) {
        opsCiEventMapper.insert(entity);
        log.info("CI 事件已入库: repo=[{}] workflow=[{}] conclusion=[{}] branch=[{}]",
                entity.getRepoName(), entity.getWorkflowName(),
                entity.getConclusion(), entity.getHeadBranch());
    }

    /**
     * 解析 GitHub Webhook 原始 Payload 并入库。
     *
     * <p>
     * JSON 解析的受检异常（{@code JsonProcessingException}）在此封装为
     * {@link BusinessException}，使 Controller 保持零 try-catch（规则 6.2）。
     * </p>
     */
    @Override
    public void parseAndRecord(byte[] rawBody) {
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            JsonNode workflowRun = root.path("workflow_run");
            JsonNode repository = root.path("repository");

            // conclusion 未完成时为 null（不能用 asText 默认值，必须显式判断）
            String conclusion = workflowRun.path("conclusion").isNull()
                    ? null
                    : workflowRun.path("conclusion").asText();

            OpsCiEventEntity entity = new OpsCiEventEntity();
            entity.setRepoName(repository.path("full_name").asText());
            entity.setWorkflowName(workflowRun.path("name").asText());
            entity.setStatus(workflowRun.path("status").asText());
            entity.setConclusion(conclusion);
            entity.setHeadSha(workflowRun.path("head_sha").asText());
            entity.setHeadBranch(workflowRun.path("head_branch").asText(null));
            entity.setTriggerEvent(workflowRun.path("event").asText(null));

            record(entity);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("GitHub Webhook Payload 解析失败", e);
            throw new BusinessException(SystemErrorCode.PARAM_ERROR, "Webhook Payload 格式非法");
        }
    }
}
