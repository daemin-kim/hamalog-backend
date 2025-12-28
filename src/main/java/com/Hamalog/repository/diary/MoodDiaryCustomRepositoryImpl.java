package com.Hamalog.repository.diary;

import com.Hamalog.domain.diary.MoodType;
import com.Hamalog.domain.diary.QMoodDiary;
import com.Hamalog.dto.diary.projection.MoodDiaryProjection;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * MoodDiary QueryDSL Custom Repository 구현체
 * 복잡한 동적 쿼리를 위한 QueryDSL 기반 메서드 구현
 */
@Repository
@RequiredArgsConstructor
public class MoodDiaryCustomRepositoryImpl implements MoodDiaryCustomRepository {

    private final JPAQueryFactory queryFactory;

    private static final QMoodDiary moodDiary = QMoodDiary.moodDiary;

    @Override
    public Page<MoodDiaryProjection> searchWithConditions(
            Long memberId,
            String keyword,
            MoodType moodType,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        // 기본 쿼리 구성
        JPAQuery<MoodDiaryProjection> query = queryFactory
                .select(Projections.constructor(
                        MoodDiaryProjection.class,
                        moodDiary.moodDiaryId,
                        moodDiary.member.memberId,
                        moodDiary.diaryDate,
                        moodDiary.moodType,
                        moodDiary.diaryType,
                        moodDiary.createdAt
                ))
                .from(moodDiary)
                .where(
                        memberIdEq(memberId),
                        keywordContains(keyword),
                        moodTypeEq(moodType),
                        dateBetween(startDate, endDate)
                );

        // 전체 개수 조회 (페이징을 위해)
        long total = query.fetch().size();

        // 페이징 적용
        List<MoodDiaryProjection> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(moodDiary.diaryDate.desc())
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 회원 ID 조건
     */
    private BooleanExpression memberIdEq(Long memberId) {
        return memberId != null ? moodDiary.member.memberId.eq(memberId) : null;
    }

    /**
     * 키워드 검색 조건 (자유 형식 내용 또는 템플릿 답변에 포함)
     */
    private BooleanExpression keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return moodDiary.freeContent.containsIgnoreCase(keyword)
                .or(moodDiary.templateAnswer1.containsIgnoreCase(keyword))
                .or(moodDiary.templateAnswer2.containsIgnoreCase(keyword))
                .or(moodDiary.templateAnswer3.containsIgnoreCase(keyword))
                .or(moodDiary.templateAnswer4.containsIgnoreCase(keyword));
    }

    /**
     * 기분 타입 조건
     */
    private BooleanExpression moodTypeEq(MoodType moodType) {
        return moodType != null ? moodDiary.moodType.eq(moodType) : null;
    }

    /**
     * 날짜 범위 조건
     */
    private BooleanExpression dateBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            return moodDiary.diaryDate.between(startDate, endDate);
        } else if (startDate != null) {
            return moodDiary.diaryDate.goe(startDate);
        } else if (endDate != null) {
            return moodDiary.diaryDate.loe(endDate);
        }
        return null;
    }
}
