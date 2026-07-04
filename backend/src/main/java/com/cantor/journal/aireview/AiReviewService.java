package com.cantor.journal.aireview;

import com.cantor.journal.aireview.AiReviewGenerator.PaperSnapshot;
import com.cantor.journal.common.ApiException;
import com.cantor.journal.paper.Paper;
import com.cantor.journal.paper.PaperService;
import com.cantor.journal.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiReviewService {

    /** 이 시간을 넘긴 PENDING은 중단된 작업으로 보고 재요청을 허용한다. */
    private static final Duration PENDING_TIMEOUT = Duration.ofMinutes(10);

    private final AiReviewRepository repository;
    private final AiReviewGenerator generator;
    private final PaperService paperService;

    /**
     * AI 리뷰 생성을 요청한다. PENDING 행을 먼저 커밋한 뒤 비동기 생성을 시작해야
     * 생성 스레드가 행을 못 찾는 레이스가 없으므로 의도적으로 트랜잭션을 걸지 않는다.
     */
    public AiReview request(Long paperId, User editor) {
        if (!generator.isConfigured()) {
            throw ApiException.badRequest("AI 리뷰가 설정되지 않았습니다. 서버에 GEMINI_API_KEY를 설정하세요.");
        }
        Paper paper = paperService.get(paperId);
        if (paper.getStoredPath() == null) {
            throw ApiException.badRequest("논문 PDF가 없어 AI 리뷰를 생성할 수 없습니다.");
        }
        Instant staleBefore = Instant.now().minus(PENDING_TIMEOUT);
        if (repository.existsByPaperIdAndStatusAndCreatedAtAfter(paperId, AiReviewStatus.PENDING, staleBefore)) {
            throw ApiException.conflict("이미 AI 리뷰를 생성하는 중입니다. 완료 후 다시 시도하세요.");
        }

        AiReview review = repository.save(AiReview.builder()
                .paper(paper)
                .requestedBy(editor)
                .model(generator.modelId())
                .status(AiReviewStatus.PENDING)
                .build());

        PaperSnapshot snapshot = new PaperSnapshot(
                paper.getTitle(), paper.getAuthorsText(), paper.getAbstractText(),
                paper.getCategory(), paper.getArticleType(), paper.getStoredPath());
        generator.generate(review.getId(), snapshot);
        return review;
    }

    @Transactional(readOnly = true)
    public List<AiReview> listForPaper(Long paperId) {
        paperService.get(paperId); // 존재 확인
        return repository.findByPaperIdOrderByCreatedAtDesc(paperId);
    }
}
