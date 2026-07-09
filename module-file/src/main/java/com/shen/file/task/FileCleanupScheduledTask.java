package com.shen.file.task;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shen.file.entity.FileResource;
import com.shen.file.service.FileResourceService;
import com.shen.file.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Date;

/**
 * 文件清理定时任务
 * 每天凌晨1点执行，清理未被引用且超过配置天数的文件
 */
@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class FileCleanupScheduledTask {

    private final FileResourceService fileResourceService;
    private final FileStorageService fileStorageService;

    @Value("${files.upload.cleanup-buffer-days:3}")
    private int cleanupBufferDays;

    /**
     * 清理未被引用的文件
     * 每天凌晨1点执行
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @SchedulerLock(name = "fileCleanupTask", lockAtMostFor = "1h")
    public void cleanupUnreferencedFiles() {
        log.info("开始清理未被引用的文件...");

        Date bufferDate = DateUtil.offsetDay(new Date(), -cleanupBufferDays);

        int pageSize = 1000;
        int totalDeleted = 0;
        int totalFailed = 0;

        Page<FileResource> page = fileResourceService.pageRefZero(1, pageSize, bufferDate);

        while (!CollectionUtils.isEmpty(page.getRecords())) {
            for (FileResource fileResource : page.getRecords()) {
                try {
                    // 删除主文件
                    try {
                        fileStorageService.delete(fileResource.getPath());
                    } catch (Exception e) {
                        log.warn("删除主文件失败: {}", fileResource.getPath(), e);
                    }

                    // 删除缩略图（如果有）
                    if (fileResource.getThumbPath() != null) {
                        try {
                            fileStorageService.delete(fileResource.getThumbPath());
                        } catch (Exception e) {
                            log.warn("删除缩略图失败: {}", fileResource.getThumbPath(), e);
                        }
                    }

                    // 删除数据库记录
                    fileResourceService.removeById(fileResource.getId());
                    totalDeleted++;

                } catch (Exception e) {
                    log.error("清理文件时发生异常，文件ID: {}", fileResource.getId(), e);
                    totalFailed++;
                }
            }

            // 查询下一页
            page = fileResourceService.pageRefZero(1, pageSize, bufferDate);
        }

        log.info("未引用文件清理完成，共清理: {} 个，失败: {} 个", totalDeleted, totalFailed);
    }
}