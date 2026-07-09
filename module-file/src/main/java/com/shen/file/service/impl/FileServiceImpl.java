package com.shen.file.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.shen.file.dto.FileResourceRes;
import com.shen.file.entity.FileResource;
import com.shen.file.service.FileResourceService;
import com.shen.file.service.FileService;
import com.shen.file.storage.FileStorageService;
import com.xqlee.image.png.PngCompressor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Iterator;
import java.util.Set;

/**
 * 文件上传服务实现
 * <p>
 * 核心逻辑：
 * 1. 上传文件按日期分目录，雪花ID命名
 * 2. 图片按格式差异化压缩（PNG用pngquant，JPEG用Thumbnailator动态质量）
 * 3. 压缩后体积未减小则保留原文件
 * 4. 同时生成缩略图（_thumb后缀）
 * 5. 写入 file_resource 索引表，ref_count 初始为 1
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {

    @Value("${files.upload.max-image-dimension:2000}")
    private int maxImageDimension;

    @Value("${files.upload.thumb-size:200}")
    private int thumbSize;

    private static final Set<String> SUPPORTED_IMAGE_FORMATS =
            Set.of("png", "jpg", "jpeg", "bmp", "gif", "webp");

    private final FileStorageService fileStorageService;
    private final FileResourceService fileResourceService;
    private final IdentifierGenerator identifierGenerator;

    @Override
    public FileResourceRes upload(MultipartFile file) throws IOException {
        String fileId = identifierGenerator.nextId(null).toString();
        String extName = FileUtil.extName(file.getOriginalFilename());

        // 按日期分目录：/2026/07/09/
        String datePath = StrUtil.SLASH
                + DateUtil.format(LocalDateTime.now(ZoneOffset.UTC), "yyyy/MM/dd")
                + StrUtil.SLASH;

        String mainFileName = fileId + (StringUtils.hasLength(extName) ? StrUtil.DOT + extName : "");
        String thumbFileName = fileId + "_thumb" + (StringUtils.hasLength(extName) ? StrUtil.DOT + extName : "");

        String mainFilePath = datePath + mainFileName;
        String thumbFilePath = null;

        if (isImageFormat(extName)) {
            String lowerExt = extName.toLowerCase();
            File finalMainFile = null;

            try {
                // 图片压缩处理
                File processedFile = compressImage(file, lowerExt, extName);

                // 确定最终使用的主图文件
                if (processedFile != null && processedFile.length() < file.getSize()) {
                    finalMainFile = processedFile;
                    log.info("图片压缩成功，格式：{}，原大小：{}KB，压缩后：{}KB，压缩率：{}%",
                            extName, file.getSize() / 1024, processedFile.length() / 1024,
                            (file.getSize() - processedFile.length()) * 100 / file.getSize());
                } else {
                    // 压缩效果不佳，保存原文件到临时文件
                    finalMainFile = File.createTempFile("main_", "." + lowerExt);
                    FileUtil.writeFromStream(file.getInputStream(), finalMainFile);
                    if (processedFile != null) {
                        log.warn("图片压缩效果不佳，使用原文件");
                    }
                }

                // 上传主图
                try (FileInputStream fis = new FileInputStream(finalMainFile)) {
                    fileStorageService.upload(fis, mainFilePath);
                }

                // 基于最终主图生成缩略图
                File thumbTempFile = generateThumbnailFromFile(finalMainFile, lowerExt);
                if (thumbTempFile != null) {
                    try (FileInputStream fis = new FileInputStream(thumbTempFile)) {
                        fileStorageService.upload(fis, datePath + thumbFileName);
                    }
                    thumbFilePath = datePath + thumbFileName;
                    log.info("缩略图生成成功，格式：{}，尺寸：{}x{}", lowerExt, thumbSize, thumbSize);
                    FileUtil.del(thumbTempFile);
                }

            } catch (Exception e) {
                log.warn("图片处理失败，使用原文件，格式：{}，异常：{}", extName, e.getMessage(), e);
                // 异常降级：直接上传原文件
                fileStorageService.upload(file.getInputStream(), mainFilePath);
            } finally {
                // 清理临时文件
                if (finalMainFile != null && finalMainFile.exists()) {
                    FileUtil.del(finalMainFile);
                }
            }
        } else {
            // 非图片文件直接上传
            fileStorageService.upload(file.getInputStream(), mainFilePath);
        }

        // 写入索引表
        FileResource fileResource = fileResourceService.saveFileResource(mainFilePath, thumbFilePath);

        // 构建返回结果
        FileResourceRes res = new FileResourceRes();
        res.setId(fileResource.getId());
        res.setUrl(fileStorageService.getUrl(mainFilePath));
        if (thumbFilePath != null) {
            res.setThumbUrl(fileStorageService.getUrl(thumbFilePath));
        }
        return res;
    }

    @Override
    public void delete(Long fileId) {
        if (fileId != null) {
            fileResourceService.decrementRefCount(fileId);
        }
    }

    @Override
    public void add(Long fileId) {
        if (fileId != null) {
            fileResourceService.incrementRefCount(fileId);
        }
    }

    @Override
    public String getUrlById(Long id) {
        if (id == null) {
            return null;
        }
        FileResource fileResource = fileResourceService.getById(id);
        if (fileResource == null) {
            return null;
        }
        return fileStorageService.getUrl(fileResource.getPath());
    }

    @Override
    public String getThumbUrlById(Long id) {
        if (id == null) {
            return null;
        }
        FileResource fileResource = fileResourceService.getById(id);
        if (fileResource == null || fileResource.getThumbPath() == null) {
            return null;
        }
        return fileStorageService.getUrl(fileResource.getThumbPath());
    }

    /**
     * 图片压缩统一入口
     */
    private File compressImage(MultipartFile file, String lowerExt, String extName) throws IOException {
        if ("png".equals(lowerExt)) {
            return compressPng(file);
        } else if ("jpg".equals(lowerExt) || "jpeg".equals(lowerExt)) {
            return compressJpeg(file);
        } else if ("webp".equals(lowerExt)) {
            return compressWebp(file);
        } else if ("bmp".equals(lowerExt) || "gif".equals(lowerExt)) {
            return scaleImageOnly(file, lowerExt);
        }
        return null;
    }

    /**
     * 基于文件生成缩略图
     */
    private File generateThumbnailFromFile(File sourceFile, String format) throws IOException {
        File tempOutput = null;
        try {
            tempOutput = File.createTempFile("thumb_", "." + format);
            Thumbnails.of(sourceFile)
                    .size(thumbSize, thumbSize)
                    .outputFormat(format)
                    .toFile(tempOutput);

            File result = tempOutput;
            tempOutput = null;
            return result;

        } catch (Exception e) {
            throw new IOException("缩略图生成失败: " + e.getMessage(), e);
        } finally {
            if (tempOutput != null && tempOutput.exists()) FileUtil.del(tempOutput);
        }
    }

    /**
     * 使用 pngquant 压缩 PNG 文件
     */
    private File compressPng(MultipartFile file) throws IOException {
        File tempInput = null;
        File tempScaled = null;
        File tempOutput = null;

        try {
            Dimension dim = getImageDimension(file.getInputStream());
            boolean needScale = dim != null && (dim.width > maxImageDimension || dim.height > maxImageDimension);

            File sourceFileForCompression;
            if (needScale) {
                tempScaled = File.createTempFile("scaled_", ".png");
                Thumbnails.of(file.getInputStream())
                        .size(maxImageDimension, maxImageDimension)
                        .outputFormat("png")
                        .toFile(tempScaled);
                sourceFileForCompression = tempScaled;
            } else {
                tempInput = File.createTempFile("upload_", ".png");
                FileUtil.writeFromStream(file.getInputStream(), tempInput);
                sourceFileForCompression = tempInput;
            }

            tempOutput = File.createTempFile("compressed_", ".png");
            PngCompressor.compress(sourceFileForCompression, tempOutput);

            File result = tempOutput;
            tempOutput = null;
            return result;

        } catch (Exception e) {
            throw new IOException("PNG压缩失败: " + e.getMessage(), e);
        } finally {
            if (tempInput != null && tempInput.exists()) FileUtil.del(tempInput);
            if (tempScaled != null && tempScaled.exists()) FileUtil.del(tempScaled);
            if (tempOutput != null && tempOutput.exists()) FileUtil.del(tempOutput);
        }
    }

    /**
     * 压缩 JPEG 文件
     */
    private File compressJpeg(MultipartFile file) throws IOException {
        Dimension dim = getImageDimension(file.getInputStream());
        boolean needScale = dim != null && (dim.width > maxImageDimension || dim.height > maxImageDimension);

        if (!needScale) {
            return null;
        }

        File tempOutput = null;
        try {
            long fileSize = file.getSize();
            float quality = fileSize > 2 * 1024 * 1024 ? 0.75f
                    : fileSize > 500 * 1024 ? 0.85f : 0.92f;

            tempOutput = File.createTempFile("compressed_", ".jpg");
            Thumbnails.of(file.getInputStream())
                    .size(maxImageDimension, maxImageDimension)
                    .outputFormat("jpg")
                    .outputQuality(quality)
                    .toFile(tempOutput);

            File result = tempOutput;
            tempOutput = null;
            return result;

        } catch (Exception e) {
            throw new IOException("JPEG压缩失败: " + e.getMessage(), e);
        } finally {
            if (tempOutput != null && tempOutput.exists()) FileUtil.del(tempOutput);
        }
    }

    /**
     * 压缩 WebP 文件
     */
    private File compressWebp(MultipartFile file) throws IOException {
        Dimension dim = getImageDimension(file.getInputStream());
        boolean needScale = dim != null && (dim.width > maxImageDimension || dim.height > maxImageDimension);

        if (!needScale) {
            return null;
        }

        File tempOutput = null;
        try {
            tempOutput = File.createTempFile("compressed_", ".webp");
            Thumbnails.of(file.getInputStream())
                    .size(maxImageDimension, maxImageDimension)
                    .outputFormat("webp")
                    .outputQuality(0.85f)
                    .toFile(tempOutput);

            File result = tempOutput;
            tempOutput = null;
            return result;

        } catch (Exception e) {
            throw new IOException("WebP压缩失败: " + e.getMessage(), e);
        } finally {
            if (tempOutput != null && tempOutput.exists()) FileUtil.del(tempOutput);
        }
    }

    /**
     * 仅缩放图片（BMP、GIF 等格式）
     */
    private File scaleImageOnly(MultipartFile file, String format) throws IOException {
        Dimension dim = getImageDimension(file.getInputStream());
        boolean needScale = dim != null && (dim.width > maxImageDimension || dim.height > maxImageDimension);

        if (!needScale) {
            return null;
        }

        File tempOutput = null;
        try {
            tempOutput = File.createTempFile("scaled_", "." + format);
            Thumbnails.of(file.getInputStream())
                    .size(maxImageDimension, maxImageDimension)
                    .outputFormat(format)
                    .toFile(tempOutput);

            File result = tempOutput;
            tempOutput = null;
            return result;

        } catch (Exception e) {
            throw new IOException(format.toUpperCase() + "缩放失败: " + e.getMessage(), e);
        } finally {
            if (tempOutput != null && tempOutput.exists()) FileUtil.del(tempOutput);
        }
    }

    /**
     * 生成缩略图
     */
    private File generateThumbnail(MultipartFile file, String format) throws IOException {
        File tempOutput = null;
        try {
            tempOutput = File.createTempFile("thumb_", "." + format);
            Thumbnails.of(file.getInputStream())
                    .size(thumbSize, thumbSize)
                    .outputFormat(format)
                    .toFile(tempOutput);

            File result = tempOutput;
            tempOutput = null;
            return result;

        } catch (Exception e) {
            throw new IOException("缩略图生成失败: " + e.getMessage(), e);
        } finally {
            if (tempOutput != null && tempOutput.exists()) FileUtil.del(tempOutput);
        }
    }

    /**
     * 获取图片尺寸
     */
    private Dimension getImageDimension(InputStream in) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(in)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(iis, true, true);
                    return new Dimension(reader.getWidth(0), reader.getHeight(0));
                } finally {
                    reader.dispose();
                }
            }
        } catch (IOException e) {
            log.debug("读取图片尺寸失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 判断是否为支持的图片格式
     */
    private boolean isImageFormat(String extName) {
        return extName != null && SUPPORTED_IMAGE_FORMATS.contains(extName.toLowerCase());
    }
}