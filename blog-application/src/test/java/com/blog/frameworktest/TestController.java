package com.blog.frameworktest;

import com.blog.common.enums.SystemErrorCode;
import com.blog.common.exception.BusinessException;
import com.blog.common.model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/framework-test")
public class TestController {
    @Autowired
    private ITestService testService;

    @PostMapping
    public Result<Boolean> create(@RequestBody TestDTO dto) {
        return Result.success(testService.saveByDto(dto));
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
}