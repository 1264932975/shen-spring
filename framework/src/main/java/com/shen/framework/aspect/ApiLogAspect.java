package com.shen.framework.aspect;

import com.alibaba.fastjson2.JSON;
import com.shen.framework.entity.ApiLog;
import com.shen.framework.service.ApiLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 接口日志切面
 * 自动拦截所有 Controller 层的 public 方法，记录请求路径、入参、出参、耗时、操作用户等信息并落库
 *
 * 执行顺序：JwtAuthenticationTokenFilter(Filter) → ApiLogAspect(AOP) → Controller
 * 用户信息通过 Filter 中 request.setAttribute("userId") 传递，Aspect 直接 getAttribute 复用，避免重复解析 JWT
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiLogAspect {

    private final ApiLogService apiLogService;

    /**
     * 日志字段最大长度，防止超长请求体/响应体撑爆数据库
     */
    private static final int MAX_LENGTH = 3000;

    /**
     * 切点：拦截 com.shen 包下所有 controller 包中的 public 方法
     */
    @Pointcut("execution(public * com.shen..controller..*.*(..))")
    public void apiPointcut() {
    }

    /**
     * 环绕通知：在 Controller 方法执行前后采集数据
     */
    @Around("apiPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取当前请求对象
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        ApiLog apiLog = new ApiLog();
        apiLog.setUri(request.getRequestURI());
        apiLog.setMethod(request.getMethod());

        // 记录 Query 参数（URL 问号后面的参数）
        Map<String, String[]> parameterMap = request.getParameterMap();
        apiLog.setParams(
                CollectionUtils.isEmpty(parameterMap) ? null :
                        JSON.toJSONString(parameterMap)
        );

        // 记录 Request Body（POST/PUT 等请求体数据，只提取 @RequestBody 标注的参数）
        apiLog.setBody(
                truncateString(
                        extractRequestBody(joinPoint, request.getContentType())
                )
        );

        apiLog.setCreateTime(LocalDateTime.now());

        // 从 Filter 中 setAttribute 的操作人信息（未登录时为 null）
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj != null) {
            apiLog.setOperatorId(Long.valueOf(userIdObj.toString()));
        }

        // 从 Filter 中 setAttribute 的客户端信息
        Object clientVersionObj = request.getAttribute("clientVersion");
        if (clientVersionObj != null) {
            apiLog.setClientVersion(clientVersionObj.toString());
        }
        Object clientPlatformObj = request.getAttribute("clientPlatform");
        if (clientPlatformObj != null) {
            apiLog.setClientPlatform(clientPlatformObj.toString());
        }

        // 记录开始时间，用于计算耗时
        long startTime = System.currentTimeMillis();
        Object result = null;
        try {
            // 执行目标 Controller 方法
            result = joinPoint.proceed();
        } finally {
            // 计算耗时（毫秒）
            long costTime = System.currentTimeMillis() - startTime;
            apiLog.setTime(costTime);

            // 记录响应结果（无论成功还是异常都会执行）
            apiLog.setResult(
                    truncateString(
                            JSON.toJSONString(result)
                    )
            );

            // 落库
            apiLogService.save(apiLog);
        }
        return result;
    }

    /**
     * 截断超长字符串，防止日志字段过长
     */
    private String truncateString(String str) {
        if (!StringUtils.hasLength(str)) {
            return null;
        }
        return str.length() > MAX_LENGTH
                ? str.substring(0, MAX_LENGTH) + "...[TRUNCATED]"
                : str;
    }

    /**
     * 提取 Request Body
     * 只处理 @RequestBody 标注的参数，跳过文件上传等特殊类型
     */
    private String extractRequestBody(ProceedingJoinPoint joinPoint, String contentType) {
        if (contentType != null) {
            String lowerContentType = contentType.toLowerCase();
            // 文件上传不序列化，直接标记
            if (lowerContentType.contains("multipart/form-data")) {
                return "[File_Upload]";
            }
            // 二进制流不序列化
            if (lowerContentType.startsWith("application/octet-stream")) {
                return "[Binary_Data]";
            }
        }

        // 遍历方法参数，找到 @RequestBody 标注的参数并序列化
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(RequestBody.class)) {
                return args[i] != null ? JSON.toJSONString(args[i]) : null;
            }
        }
        return null;
    }
}