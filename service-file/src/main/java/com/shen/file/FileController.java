package com.shen.file;

import com.shen.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/admin/file")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;


    @PostMapping("/upload")
    public ResponseEntity<Object> upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("请上传文件");
        }
        return ResponseEntity.ok(
                fileService.upload(file)
        );
    }
}