package com.Hamalog.service.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.Hamalog.domain.medication.MedicationScheduleGroup;
import com.Hamalog.domain.member.Member;
import com.Hamalog.dto.medication.request.MedicationScheduleGroupCreateRequest;
import com.Hamalog.dto.medication.request.MedicationScheduleGroupUpdateRequest;
import com.Hamalog.dto.medication.response.MedicationScheduleGroupResponse;
import com.Hamalog.exception.CustomException;
import com.Hamalog.exception.ErrorCode;
import com.Hamalog.repository.medication.MedicationScheduleGroupRepository;
import com.Hamalog.repository.member.MemberRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("MedicationScheduleGroupService 테스트")
class MedicationScheduleGroupServiceTest {

    @Mock
    private MedicationScheduleGroupRepository groupRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MedicationScheduleGroupService groupService;

    private Member mockMember;
    private MedicationScheduleGroup mockGroup;

    @BeforeEach
    void setUp() {
        // Mock Member 설정
        mockMember = mock(Member.class);
        lenient().when(mockMember.getMemberId()).thenReturn(1L);

        // Mock Group 설정
        mockGroup = mock(MedicationScheduleGroup.class);
        lenient().when(mockGroup.getMedicationScheduleGroupId()).thenReturn(1L);
        lenient().when(mockGroup.getMember()).thenReturn(mockMember);
        lenient().when(mockGroup.getName()).thenReturn("아침약");
        lenient().when(mockGroup.getDescription()).thenReturn("아침에 먹는 약들");
        lenient().when(mockGroup.getColor()).thenReturn("#FF5733");
        lenient().when(mockGroup.getCreatedAt()).thenReturn(LocalDateTime.now());
    }

    @Nested
    @DisplayName("그룹 목록 조회")
    class GetGroups {

        @Test
        @DisplayName("성공: 그룹 목록 조회")
        void success() {
            // given
            Long memberId = 1L;
            MedicationScheduleGroup group2 = mock(MedicationScheduleGroup.class);
            when(group2.getMedicationScheduleGroupId()).thenReturn(2L);
            when(group2.getMember()).thenReturn(mockMember);
            when(group2.getName()).thenReturn("저녁약");
            when(group2.getDescription()).thenReturn("저녁에 먹는 약들");
            when(group2.getColor()).thenReturn("#5733FF");
            when(group2.getCreatedAt()).thenReturn(LocalDateTime.now());

            when(groupRepository.findByMember_MemberIdOrderByCreatedAtDesc(memberId))
                    .thenReturn(Arrays.asList(mockGroup, group2));

            // when
            List<MedicationScheduleGroupResponse> result = groupService.getGroups(memberId);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("아침약");
            verify(groupRepository).findByMember_MemberIdOrderByCreatedAtDesc(memberId);
        }

        @Test
        @DisplayName("성공: 그룹이 없는 경우 빈 목록 반환")
        void success_emptyList() {
            // given
            Long memberId = 1L;
            when(groupRepository.findByMember_MemberIdOrderByCreatedAtDesc(memberId))
                    .thenReturn(List.of());

            // when
            List<MedicationScheduleGroupResponse> result = groupService.getGroups(memberId);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("그룹 상세 조회")
    class GetGroup {

        @Test
        @DisplayName("성공: 그룹 상세 조회")
        void success() {
            // given
            Long memberId = 1L;
            Long groupId = 1L;

            when(groupRepository.findByMedicationScheduleGroupIdAndMember_MemberId(groupId, memberId))
                    .thenReturn(Optional.of(mockGroup));

            // when
            MedicationScheduleGroupResponse result = groupService.getGroup(memberId, groupId);

            // then
            assertThat(result.groupId()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("아침약");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 그룹")
        void fail_notFound() {
            // given
            Long memberId = 1L;
            Long groupId = 999L;

            when(groupRepository.findByMedicationScheduleGroupIdAndMember_MemberId(groupId, memberId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> groupService.getGroup(memberId, groupId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEDICATION_SCHEDULE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("그룹 생성")
    class CreateGroup {

        @Test
        @DisplayName("성공: 그룹 생성")
        void success() {
            // given
            Long memberId = 1L;
            MedicationScheduleGroupCreateRequest request = new MedicationScheduleGroupCreateRequest(
                    "점심약", "점심에 먹는 약들", "#33FF57"
            );

            MedicationScheduleGroup savedGroup = mock(MedicationScheduleGroup.class);
            when(savedGroup.getMedicationScheduleGroupId()).thenReturn(2L);
            when(savedGroup.getMember()).thenReturn(mockMember);
            when(savedGroup.getName()).thenReturn("점심약");
            when(savedGroup.getDescription()).thenReturn("점심에 먹는 약들");
            when(savedGroup.getColor()).thenReturn("#33FF57");
            when(savedGroup.getCreatedAt()).thenReturn(LocalDateTime.now());

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(mockMember));
            when(groupRepository.existsByMember_MemberIdAndName(memberId, "점심약")).thenReturn(false);
            when(groupRepository.save(any(MedicationScheduleGroup.class))).thenReturn(savedGroup);

            // when
            MedicationScheduleGroupResponse result = groupService.createGroup(memberId, request);

            // then
            assertThat(result.name()).isEqualTo("점심약");
            assertThat(result.color()).isEqualTo("#33FF57");
            verify(groupRepository).save(any(MedicationScheduleGroup.class));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 회원")
        void fail_memberNotFound() {
            // given
            Long memberId = 999L;
            MedicationScheduleGroupCreateRequest request = new MedicationScheduleGroupCreateRequest(
                    "점심약", "점심에 먹는 약들", "#33FF57"
            );

            when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> groupService.createGroup(memberId, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 중복된 그룹명")
        void fail_duplicateName() {
            // given
            Long memberId = 1L;
            MedicationScheduleGroupCreateRequest request = new MedicationScheduleGroupCreateRequest(
                    "아침약", "중복된 이름", "#33FF57"
            );

            when(memberRepository.findById(memberId)).thenReturn(Optional.of(mockMember));
            when(groupRepository.existsByMember_MemberIdAndName(memberId, "아침약")).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> groupService.createGroup(memberId, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_GROUP_NAME);
        }
    }

    @Nested
    @DisplayName("그룹 수정")
    class UpdateGroup {

        @Test
        @DisplayName("성공: 그룹 수정")
        void success() {
            // given
            Long memberId = 1L;
            Long groupId = 1L;
            MedicationScheduleGroupUpdateRequest request = new MedicationScheduleGroupUpdateRequest(
                    "수정된 아침약", "수정된 설명", "#00FF00"
            );

            when(groupRepository.findByMedicationScheduleGroupIdAndMember_MemberId(groupId, memberId))
                    .thenReturn(Optional.of(mockGroup));
            lenient().when(groupRepository.existsByMember_MemberIdAndName(memberId, "수정된 아침약")).thenReturn(false);
            lenient().when(mockGroup.getName()).thenReturn("수정된 아침약");
            lenient().when(mockGroup.getDescription()).thenReturn("수정된 설명");
            lenient().when(mockGroup.getColor()).thenReturn("#00FF00");

            // when
            MedicationScheduleGroupResponse result = groupService.updateGroup(memberId, groupId, request);

            // then
            assertThat(result).isNotNull();
            verify(mockGroup).update("수정된 아침약", "수정된 설명", "#00FF00");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 그룹")
        void fail_notFound() {
            // given
            Long memberId = 1L;
            Long groupId = 999L;
            MedicationScheduleGroupUpdateRequest request = new MedicationScheduleGroupUpdateRequest(
                    "수정된 이름", null, null
            );

            when(groupRepository.findByMedicationScheduleGroupIdAndMember_MemberId(groupId, memberId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> groupService.updateGroup(memberId, groupId, request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEDICATION_SCHEDULE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("그룹 삭제")
    class DeleteGroup {

        @Test
        @DisplayName("성공: 그룹 삭제")
        void success() {
            // given
            Long memberId = 1L;
            Long groupId = 1L;

            when(groupRepository.findByMedicationScheduleGroupIdAndMember_MemberId(groupId, memberId))
                    .thenReturn(Optional.of(mockGroup));

            // when
            groupService.deleteGroup(memberId, groupId);

            // then
            verify(groupRepository).delete(mockGroup);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 그룹")
        void fail_notFound() {
            // given
            Long memberId = 1L;
            Long groupId = 999L;

            when(groupRepository.findByMedicationScheduleGroupIdAndMember_MemberId(groupId, memberId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> groupService.deleteGroup(memberId, groupId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEDICATION_SCHEDULE_NOT_FOUND);
        }
    }
}
