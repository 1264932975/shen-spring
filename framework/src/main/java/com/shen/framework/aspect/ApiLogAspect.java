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
import org.springframework.scheduling.annotation.Async;
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
     * 注意：日志采集失败不影响业务接口正常运行
     */
    @Around("apiPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable exception = null;

        try {
            // 执行目标 Controller 方法
            result = joinPoint.proceed();
        } catch (Throwable e) {
            exception = e;
        }

        // 提取日志数据（同步提取，避免异步线程拿不到 RequestContextHolder）
        try {
            ApiLog apiLog = extractLogData(joinPoint, startTime, result);
            // 异步落库
            saveLogAsync(apiLog);
        } catch (Exception e) {
            log.error("接口日志记录失败", e);
        }

        // 如果 Controller 方法抛异常，继续向上抛出
        if (exception != null) {
            throw exception;
        }

        return result;
    }

    /**
     * 提取日志数据（同步执行，从当前请求上下文获取）
     */
    private ApiLog extractLogData(ProceedingJoinPoint joinPoint, long startTime, Object result) {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        ApiLog apiLog = new ApiLog();
        apiLog.setUri(request.getRequestURI());
        apiLog.setMethod(request.getMethod());

        // 记录 Query 参数
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (!CollectionUtils.isEmpty(parameterMap)) {
            apiLog.setParams(JSON.toJSONString(parameterMap));
        }

        // 记录 Request Body
        apiLog.setBody(truncateString(extractRequestBody(joinPoint, request.getContentType())));

        apiLog.setCreateTime(LocalDateTime.now());

        // 操作人信息（可能为 null 或非 Long 类型，安全转换）
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj != null) {
            try {
                apiLog.setOperatorId(Long.valueOf(userIdObj.toString()));
            } catch (NumberFormatException e) {
                log.warn("userId 转换失败: {}", userIdObj);
            }
        }

        // 客户端信息
        Object clientVersionObj = request.getAttribute("clientVersion");
        if (clientVersionObj != null) {
            apiLog.setClientVersion(clientVersionObj.toString());
        }
        Object clientPlatformObj = request.getAttribute("clientPlatform");
        if (clientPlatformObj != null) {
            apiLog.setClientPlatform(clientPlatformObj.toString());
        }

        // 计算耗时
        long costTime = System.currentTimeMillis() - startTime;
        apiLog.setTime(costTime);

        // 记录响应结果
        apiLog.setResult(truncateString(JSON.toJSONString(result)));

        return apiLog;
    }

    /**
     * 异步落库（不阻塞接口响应）
     */
    @Async
    public void saveLogAsync(ApiLog apiLog) {
        try {
            apiLogService.save(apiLog);
        } catch (Exception e) {
            log.error("接口日志落库失败，URI: {}", apiLog.getUri(), e);
        }
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