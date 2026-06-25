package com.cantor.journal.paper;

import com.cantor.journal.common.ApiException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.storage.upload-dir}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(uploadRoot);
        } catch (IOException e) {
            throw new IllegalStateException("업로드 디렉터리를 생성할 수 없습니다: " + uploadRoot, e);
        }
    }

    /**
     * PDF 파일을 저장하고 저장된 상대 경로를 반환한다.
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("논문 파일(PDF)을 첨부해야 합니다.");
        }
        String contentType = file.getContentType();
        String original = file.getOriginalFilename() == null ? "paper.pdf" : file.getOriginalFilename();
        boolean isPdf = "application/pdf".equals(contentType)
                || original.toLowerCase().endsWith(".pdf");
        if (!isPdf) {
            throw ApiException.badRequest("PDF 형식의 파일만 업로드할 수 있습니다.");
        }

        String storedName = UUID.randomUUID() + ".pdf";
        Path target = uploadRoot.resolve(storedName).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw ApiException.badRequest("잘못된 파일 경로입니다.");
        }
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장에 실패했습니다.");
        }
        return storedName;
    }

    /** 저장된 파일의 절대 경로(참고문헌 추출 등 내부 처리용). */
    public Path pathFor(String storedName) {
        return uploadRoot.resolve(storedName).normalize();
    }

    public void delete(String storedName) {
        if (storedName == null || storedName.isBlank()) {
            return;
        }
        try {
            Path file = uploadRoot.resolve(storedName).normalize();
            if (file.startsWith(uploadRoot)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            log.warn("업로드 파일 삭제 실패: {}", storedName, e);
        }
    }

    public Resource loadAsResource(String storedName) {
        try {
            Path file = uploadRoot.resolve(storedName).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw ApiException.notFound("파일을 찾을 수 없습니다.");
        } catch (Exception e) {
            throw ApiException.notFound("파일을 찾을 수 없습니다.");
        }
    }
}
