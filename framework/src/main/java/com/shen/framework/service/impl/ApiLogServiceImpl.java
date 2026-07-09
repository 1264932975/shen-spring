package com.shen.framework.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shen.framework.entity.ApiLog;
import com.shen.framework.service.ApiLogService;
import com.shen.framework.mapper.ApiLogMapper;
import org.springframework.stereotype.Service;

/**
* @author shield
* @description 针对表【api_log(接口日志表)】的数据库操作Service实现
* @createDate 2026-07-09 10:51:29
*/
@Service
public class ApiLogServiceImpl extends ServiceImpl<ApiLogMapper, ApiLog>
    implements ApiLogService{

}




