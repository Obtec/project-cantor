package com.cantor.journal.web;

import com.cantor.journal.paper.Paper;
import com.cantor.journal.paper.PaperService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

/**
 * 논문 상세 페이지(/papers/{id})를 서버에서 렌더할 때 Google Scholar용 메타데이터
 * (Highwire citation_* 태그)와 Open Graph 태그를 &lt;head&gt;에 주입한다.
 * Scholar 크롤러는 JS를 실행하지 않으므로 정적 HTML에 메타가 있어야 색인된다.
 * 브라우저는 그대로 SPA로 부팅되므로 동작에 영향이 없다.
 */
@Controller
@RequiredArgsConstructor
public class ArticleHtmlController {

    private final PaperService paperService;
    private volatile String template;

    @GetMapping(value = "/papers/{id:\\d+}", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String article(@PathVariable Long id) {
        String html = loadTemplate();
        try {
            Paper p = paperService.get(id);
            html = html.replace("</head>", buildMeta(p) + "</head>");
        } catch (Exception ignore) {
            // 논문이 없으면 기본 index.html 반환
        }
        return html;
    }

    private String buildMeta(Paper p) {
        String base = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        int year = (p.getPublishedAt() != null ? p.getPublishedAt() : p.getCreatedAt())
                .atZone(ZoneOffset.UTC).getYear();
        StringBuilder sb = new StringBuilder("\n");
        meta(sb, "citation_title", p.getTitle());
        for (String a : authors(p)) {
            meta(sb, "citation_author", a);
        }
        meta(sb, "citation_publication_date", String.valueOf(year));
        meta(sb, "citation_journal_title", "Cantor Journal");
        if (p.getIssue() != null) {
            meta(sb, "citation_volume", String.valueOf(p.getIssue().getVolume()));
            meta(sb, "citation_issue", String.valueOf(p.getIssue().getNumber()));
        }
        if (p.getPageStart() != null) meta(sb, "citation_firstpage", String.valueOf(p.getPageStart()));
        if (p.getPageEnd() != null) meta(sb, "citation_lastpage", String.valueOf(p.getPageEnd()));
        if (p.getKeywords() != null) meta(sb, "citation_keywords", p.getKeywords());
        if (p.getArticleCode() != null) meta(sb, "citation_doi", p.getArticleCode());
        meta(sb, "citation_abstract_html_url", base + "/papers/" + p.getId());
        meta(sb, "citation_pdf_url", base + "/api/papers/" + p.getId() + "/file");
        // Open Graph / Twitter
        ogMeta(sb, "og:type", "article");
        ogMeta(sb, "og:title", p.getTitle());
        if (p.getAbstractText() != null) {
            ogMeta(sb, "og:description", truncate(p.getAbstractText(), 200));
        }
        return sb.toString();
    }

    private void meta(StringBuilder sb, String name, String content) {
        sb.append("    <meta name=\"").append(name).append("\" content=\"")
                .append(escape(content)).append("\">\n");
    }

    private void ogMeta(StringBuilder sb, String property, String content) {
        sb.append("    <meta property=\"").append(property).append("\" content=\"")
                .append(escape(content)).append("\">\n");
    }

    private List<String> authors(Paper p) {
        String src = p.getAuthorsText() != null && !p.getAuthorsText().isBlank()
                ? p.getAuthorsText()
                : (p.getSubmitter() != null ? p.getSubmitter().getName() : "");
        return Arrays.stream(src.split("[,;]")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replaceAll("\\s+", " ").trim();
    }

    private String truncate(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n) + "…";
    }

    private String loadTemplate() {
        String t = template;
        if (t == null) {
            try {
                t = new String(new ClassPathResource("static/index.html")
                        .getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                t = "<!doctype html><html><head></head><body><div id=\"root\"></div></body></html>";
            }
            template = t;
        }
        return t;
    }
}
