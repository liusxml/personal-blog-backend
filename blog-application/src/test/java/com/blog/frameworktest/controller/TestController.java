package com.blog.frameworktest.controller;

import com.blog.common.exception.BusinessException;
import com.blog.common.exception.SystemErrorCode;
import com.blog.common.model.Result;
import com.blog.frameworktest.dto.TestDTO;
import com.blog.frameworktest.dto.ValidationTestDTO;
import com.blog.frameworktest.service.ITestService;
import com.blog.frameworktest.vo.TestVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/framework-test")
public class TestController {
    @Resource
    private ITestService testService;

    @PostMapping
    public Result<Long> create(@RequestBody TestDTO dto) {
        return Result.success((Long) testService.saveByDto(dto));
    }

    @GetMapping("/{id}")
    public Result<TestVO> get(@PathVariable Long id) {
        return Result.success(testService.getVoById(id).orElse(null));
    }

    @GetMapping("/error")
    public void throwError() {
        // 固定抛出一个业务异常，用于测试 GlobalExceptionHandler
        throw new BusinessException(SystemErrorCode.SYSTEM_ERROR);
    }

    /**
     * 接收带校验的 DTO，用于测试 testValidationExceptionHandler_RobustnessForNullMessage
     */
    @PostMapping("/validation")
    public Result<?> testValidation(@Valid @RequestBody ValidationTestDTO dto) {
        // 这个端点仅用于触发校验，如果校验通过则会进入这里
        return Result.success();
    }
}