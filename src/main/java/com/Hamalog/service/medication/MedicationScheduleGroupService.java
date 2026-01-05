package com.Hamalog.service.medication;

import com.Hamalog.domain.medication.MedicationScheduleGroup;
import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.medication.request.MedicationScheduleGroupCreateRequest;
import com.Hamalog.dto.medication.request.MedicationScheduleGroupUpdateRequest;
import com.Hamalog.dto.medication.response.MedicationScheduleGroupResponse;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.repository.medication.MedicationScheduleGroupRepository;
import com.Hamalog.repository.member.MemberRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class MedicationScheduleGroupService {

    private final MedicationScheduleGroupRepository groupRepository;
    private final MemberRepository memberRepository;

    /**
     * 그룹 목록 조회
     */
    public List<MedicationScheduleGroupResponse> getGroups(Long memberId) {
        List<MedicationScheduleGroup> groups = groupRepository.findByMember_MemberIdOrderByCreatedAtDesc(memberId);
        return groups.stream()
                .map(MedicationScheduleGroupResponse::from)
                .toList();
    }

    /**
     * 그룹 상세 조회
     */
    public MedicationScheduleGroupResponse getGroup(Long memberId, Long groupId) {
        MedicationScheduleGroup group = groupRepository.findByMedicationScheduleGroupIdAndMember_MemberId(groupId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEDICATION_SCHEDULE_NOT_FOUND));
        return MedicationScheduleGroupResponse.from(group);
    }

    /**
     * 그룹 생성
     */
    @Transactional(rollbackFor = {Exception.class})
    public MedicationScheduleGroupResponse createGroup(Long memberId, MedicationScheduleGroupCreateRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 그룹명 중복 확인
        if (groupRepository.existsByMember_MemberIdAndName(memberId, request.name())) {
            throw new CustomException(ErrorCode.DUPLICATE_GROUP_NAME);
        }

        MedicationScheduleGroup group = MedicationScheduleGroup.builder()
                .member(member)
                .name(request.name())
                .description(request.description())
                .color(request.color())
                .build();

        MedicationScheduleGroup savedGroup = groupRepository.save(group);
        log.info("복약 그룹 생성 - memberId: {}, groupId: {}", memberId, savedGroup.getMedicationScheduleGroupId());

        return MedicationScheduleGroupResponse.from(savedGroup);
    }

    /**
     * 그룹 수정
     */
    @Transactional(rollbackFor = {Exception.class})
    public MedicationScheduleGroupResponse updateGroup(Long memberId, Long groupId, MedicationScheduleGroupUpdateRequest request) {
        MedicationScheduleGroup group = groupRepository.findByMedicationScheduleGroupIdAndMember_MemberId(groupId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEDICATION_SCHEDULE_NOT_FOUND));

        String newName = request.name() != null ? request.name() : group.getName();
        String newDescription = request.description() != null ? request.description() : group.getDescription();
        String newColor = request.color() != null ? request.color() : group.getColor();

        // 이름이 변경되었고, 새 이름이 이미 존재하는 경우 검증
        if (!newName.equals(group.getName()) && groupRepository.existsByMember_MemberIdAndName(memberId, newName)) {
            throw new CustomException(ErrorCode.DUPLICATE_MEMBER);
        }

        group.update(newName, newDescription, newColor);
        log.info("복약 그룹 수정 - memberId: {}, groupId: {}", memberId, groupId);

        return MedicationScheduleGroupResponse.from(group);
    }

    /**
     * 그룹 삭제
     */
    @Transactional(rollbackFor = {Exception.class})
    public void deleteGroup(Long memberId, Long groupId) {
        MedicationScheduleGroup group = groupRepository.findByMedicationScheduleGroupIdAndMember_MemberId(groupId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEDICATION_SCHEDULE_NOT_FOUND));

        groupRepository.delete(group);
        log.info("복약 그룹 삭제 - memberId: {}, groupId: {}", memberId, groupId);
    }
}
