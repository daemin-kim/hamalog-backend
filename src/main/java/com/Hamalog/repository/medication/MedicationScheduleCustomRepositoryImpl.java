package com.Hamalog.repository.medication;

import com.Hamalog.domain.medication.QMedicationSchedule;
import com.Hamalog.dto.medication.projection.MedicationScheduleProjection;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * MedicationSchedule QueryDSL Custom Repository 구현체
 * 복잡한 동적 쿼리를 위한 QueryDSL 기반 메서드 구현
 */
@Repository
@RequiredArgsConstructor
public class MedicationScheduleCustomRepositoryImpl implements MedicationScheduleCustomRepository {

    private final JPAQueryFactory queryFactory;

    private static final QMedicationSchedule medicationSchedule = QMedicationSchedule.medicationSchedule;

    @Override
    public Page<MedicationScheduleProjection> searchWithConditions(
            Long memberId,
            String keyword,
            Boolean isActive,
            Pageable pageable
    ) {
        // 기본 쿼리 구성
        JPAQuery<MedicationScheduleProjection> query = queryFactory
                .select(Projections.constructor(
                        MedicationScheduleProjection.class,
                        medicationSchedule.medicationScheduleId,
                        medicationSchedule.member.memberId,
                        medicationSchedule.name,
                        medicationSchedule.hospitalName,
                        medicationSchedule.prescriptionDate,
                        medicationSchedule.memo,
                        medicationSchedule.startOfAd,
                        medicationSchedule.prescriptionDays,
                        medicationSchedule.perDay,
                        medicationSchedule.alarmType,
                        medicationSchedule.isActive
                ))
                .from(medicationSchedule)
                .where(
                        memberIdEq(memberId),
                        keywordContains(keyword),
                        isActiveEq(isActive)
                );

        // 전체 개수 조회 (페이징을 위해)
        long total = query.fetch().size();

        // 페이징 적용
        List<MedicationScheduleProjection> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(medicationSchedule.prescriptionDate.desc())
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 회원 ID 조건
     */
    private BooleanExpression memberIdEq(Long memberId) {
        return memberId != null ? medicationSchedule.member.memberId.eq(memberId) : null;
    }

    /**
     * 키워드 검색 조건 (약 이름 또는 병원 이름에 포함)
     */
    private BooleanExpression keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return medicationSchedule.name.containsIgnoreCase(keyword)
                .or(medicationSchedule.hospitalName.containsIgnoreCase(keyword));
    }

    /**
     * 활성 상태 조건
     */
    private BooleanExpression isActiveEq(Boolean isActive) {
        return isActive != null ? medicationSchedule.isActive.eq(isActive) : null;
    }
}
