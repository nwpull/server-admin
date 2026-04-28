package com.serveradmin.controller;

import com.serveradmin.common.Result;
import com.serveradmin.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @GetMapping
    public Result<List<Map<String, Object>>> listFiles(
            @RequestParam String serverId,
            @RequestParam(defaultValue = "/") String path) {
        try {
            List<Map<String, Object>> files = fileService.listFiles(serverId, path);
            return Result.success(files);
        } catch (Exception e) {
            return Result.error("读取目录失败: " + e.getMessage());
        }
    }

    @PostMapping("/upload")
    public Result<Void> uploadFile(
            @RequestParam String serverId,
            @RequestParam(defaultValue = "/") String path,
            @RequestParam("file") MultipartFile file) {
        try {
            fileService.uploadFile(serverId, path, file.getOriginalFilename(), file.getBytes());
            return Result.success();
        } catch (Exception e) {
            return Result.error("上传文件失败: " + e.getMessage());
        }
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadFile(
            @RequestParam String serverId,
            @RequestParam String path) {
        try {
            byte[] content = fileService.downloadFile(serverId, path);
            String filename = path.substring(path.lastIndexOf("/") + 1);
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(content);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/mkdir")
    public Result<Void> mkdir(@RequestParam String serverId, @RequestParam String path) {
        try {
            fileService.mkdir(serverId, path);
            return Result.success();
        } catch (Exception e) {
            return Result.error("创建目录失败: " + e.getMessage());
        }
    }

    @PostMapping("/delete")
    public Result<Void> delete(@RequestParam String serverId, @RequestParam String path) {
        try {
            fileService.delete(serverId, path);
            return Result.success();
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    @PutMapping("/rename")
    public Result<Void> rename(
            @RequestParam String serverId,
            @RequestParam String oldPath,
            @RequestParam String newPath) {
        try {
            fileService.rename(serverId, oldPath, newPath);
            return Result.success();
        } catch (Exception e) {
            return Result.error("重命名失败: " + e.getMessage());
        }
    }

    @GetMapping("/read")
    public Result<String> readFile(@RequestParam String serverId, @RequestParam String path) {
        try {
            String content = fileService.readFile(serverId, path);
            return Result.success(content);
        } catch (Exception e) {
            return Result.error("读取文件失败: " + e.getMessage());
        }
    }

    @PostMapping("/write")
    public Result<Void> writeFile(
            @RequestParam String serverId,
            @RequestParam String path,
            @RequestBody String content) {
        try {
            fileService.writeFile(serverId, path, content);
            return Result.success();
        } catch (Exception e) {
            return Result.error("写入文件失败: " + e.getMessage());
        }
    }
}
