package com.cantor.journal.citation;

import com.cantor.journal.paper.FileStorageService;
import com.cantor.journal.paper.Paper;
import com.cantor.journal.paper.PaperRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 업로드된 PDF에서 텍스트를 추출하여, 저널 내 기존 논문 제목과 매칭되는
 * 참고문헌(인용 대상)을 자동으로 찾아낸다.
 *
 * <p>전략: PDF 전체 텍스트에서 "References/Bibliography/참고문헌" 섹션을 우선 분리한 뒤,
 * 공백을 제거·소문자화하여 각 기존 논문의 제목이 그 안에 등장하는지 부분 문자열로 검사한다.
 * (PDF 줄바꿈으로 제목이 끊기는 경우를 위해 공백을 모두 제거하고 비교한다.)</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReferenceExtractionService {

    private static final List<String> REF_HEADERS =
            List.of("references", "bibliography", "works cited", "literature cited", "참고문헌", "참고 문헌", "인용문헌");

    /** 너무 짧은 제목은 오탐 위험이 커서 제외(공백 제거 후 길이 기준). */
    private static final int MIN_TITLE_LENGTH = 6;

    private final FileStorageService fileStorageService;
    private final PaperRepository paperRepository;

    /**
     * 저장된 PDF에서 참고문헌을 추출해 매칭되는 논문 ID 목록을 반환한다.
     * 어떤 이유로든 실패하면 빈 목록을 반환하여 논문 제출 자체를 막지 않는다.
     *
     * @param storedName 저장된 PDF 파일명
     * @param selfId     자기 자신(자기 인용 제외), 없으면 null
     */
    @Transactional(readOnly = true)
    public List<Long> extractCitedPaperIds(String storedName, Long selfId) {
        String text;
        try {
            text = extractText(storedName);
        } catch (Exception e) {
            log.warn("PDF 참고문헌 추출 실패: {}", storedName, e);
            return List.of();
        }
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String haystack = stripWhitespace(referencesSection(text).toLowerCase());
        List<Long> matched = new ArrayList<>();
        for (Paper p : paperRepository.findAll()) {
            if (selfId != null && selfId.equals(p.getId())) {
                continue;
            }
            if (p.getTitle() == null) {
                continue;
            }
            String needle = stripWhitespace(p.getTitle().toLowerCase());
            if (needle.length() < MIN_TITLE_LENGTH) {
                continue;
            }
            if (haystack.contains(needle)) {
                matched.add(p.getId());
            }
        }
        log.info("PDF 참고문헌 추출: {} -> {}편 매칭", storedName, matched.size());
        return matched;
    }

    private String extractText(String storedName) throws IOException {
        File file = fileStorageService.pathFor(storedName).toFile();
        if (!file.exists()) {
            return "";
        }
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    /** 참고문헌 섹션 헤더의 마지막 출현 위치부터 끝까지를 반환(없으면 전체). */
    private String referencesSection(String text) {
        String lower = text.toLowerCase();
        int idx = -1;
        for (String header : REF_HEADERS) {
            int i = lower.lastIndexOf(header);
            if (i > idx) {
                idx = i;
            }
        }
        return idx >= 0 ? text.substring(idx) : text;
    }

    private String stripWhitespace(String s) {
        return s.replaceAll("\\s+", "");
    }
}
