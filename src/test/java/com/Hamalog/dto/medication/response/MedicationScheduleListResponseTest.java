package com.Hamalog.dto.medication.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.Hamalog.domain.medication.AlarmType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;

@DisplayName("MedicationScheduleListResponse Tests")
class MedicationScheduleListResponseTest {

    @Mock
    private Page<MedicationScheduleResponse> mockPage;

    private List<MedicationScheduleResponse> sampleSchedules;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        sampleSchedules = Arrays.asList(
                new MedicationScheduleResponse(
                        1L, 1L, "Medicine A", "Hospital A",
                        LocalDate.of(2023, 10, 14), "Take with food",
                        LocalDate.of(2023, 10, 15), 7, 2, AlarmType.SOUND
                ),
                new MedicationScheduleResponse(
                        2L, 1L, "Medicine B", "Hospital B",
                        LocalDate.of(2023, 10, 13), "Before meals",
                        LocalDate.of(2023, 10, 14), 14, 1, AlarmType.VIBE
                )
        );
    }

    @Test
    @DisplayName("Should create MedicationScheduleListResponse with valid data")
    void constructor_WithValidData_ShouldCreateValidResponse() {
        // given
        List<MedicationScheduleResponse> schedules = sampleSchedules;
        long totalCount = 2L;
        int currentPage = 0;
        int pageSize = 10;
        boolean hasNext = false;
        boolean hasPrevious = false;

        // when
        MedicationScheduleListResponse response = new MedicationScheduleListResponse(
                schedules, totalCount, currentPage, pageSize, hasNext, hasPrevious
        );

        // then
        assertThat(response.schedules()).hasSize(2);
        assertThat(response.schedules()).isEqualTo(schedules);
        assertThat(response.totalCount()).isEqualTo(2L);
        assertThat(response.currentPage()).isEqualTo(0);
        assertThat(response.pageSize()).isEqualTo(10);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("Should create MedicationScheduleListResponse with empty schedules")
    void constructor_WithEmptySchedules_ShouldCreateResponse() {
        // given
        List<MedicationScheduleResponse> schedules = Collections.emptyList();

        // when
        MedicationScheduleListResponse response = new MedicationScheduleListResponse(
                schedules, 0L, 0, 10, false, false
        );

        // then
        assertThat(response.schedules()).isEmpty();
        assertThat(response.totalCount()).isEqualTo(0L);
        assertThat(response.currentPage()).isEqualTo(0);
        assertThat(response.pageSize()).isEqualTo(10);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("Should create MedicationScheduleListResponse with pagination flags")
    void constructor_WithPaginationFlags_ShouldCreateResponse() {
        // given
        List<MedicationScheduleResponse> schedules = sampleSchedules;

        // when
        MedicationScheduleListResponse response = new MedicationScheduleListResponse(
                schedules, 20L, 2, 10, true, true
        );

        // then
        assertThat(response.schedules()).hasSize(2);
        assertThat(response.totalCount()).isEqualTo(20L);
        assertThat(response.currentPage()).isEqualTo(2);
        assertThat(response.pageSize()).isEqualTo(10);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.hasPrevious()).isTrue();
    }

    @Test
    @DisplayName("Should create from Spring Data Page object")
    void from_WithPageObject_ShouldCreateResponse() {
        // given
        when(mockPage.getContent()).thenReturn(sampleSchedules);
        when(mockPage.getTotalElements()).thenReturn(2L);
        when(mockPage.getNumber()).thenReturn(0);
        when(mockPage.getSize()).thenReturn(10);
        when(mockPage.hasNext()).thenReturn(false);
        when(mockPage.hasPrevious()).thenReturn(false);

        // when
        MedicationScheduleListResponse response = MedicationScheduleListResponse.from(mockPage);

        // then
        assertThat(response.schedules()).hasSize(2);
        assertThat(response.schedules()).isEqualTo(sampleSchedules);
        assertThat(response.totalCount()).isEqualTo(2L);
        assertThat(response.currentPage()).isEqualTo(0);
        assertThat(response.pageSize()).isEqualTo(10);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("Should create from Spring Data Page with pagination")
    void from_WithPageObjectHavingPagination_ShouldCreateResponse() {
        // given
        when(mockPage.getContent()).thenReturn(sampleSchedules);
        when(mockPage.getTotalElements()).thenReturn(25L);
        when(mockPage.getNumber()).thenReturn(2);
        when(mockPage.getSize()).thenReturn(10);
        when(mockPage.hasNext()).thenReturn(true);
        when(mockPage.hasPrevious()).thenReturn(true);

        // when
        MedicationScheduleListResponse response = MedicationScheduleListResponse.from(mockPage);

        // then
        assertThat(response.schedules()).hasSize(2);
        assertThat(response.totalCount()).isEqualTo(25L);
        assertThat(response.currentPage()).isEqualTo(2);
        assertThat(response.pageSize()).isEqualTo(10);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.hasPrevious()).isTrue();
    }

    @Test
    @DisplayName("Should create from empty Page object")
    void from_WithEmptyPageObject_ShouldCreateResponse() {
        // given
        when(mockPage.getContent()).thenReturn(Collections.emptyList());
        when(mockPage.getTotalElements()).thenReturn(0L);
        when(mockPage.getNumber()).thenReturn(0);
        when(mockPage.getSize()).thenReturn(10);
        when(mockPage.hasNext()).thenReturn(false);
        when(mockPage.hasPrevious()).thenReturn(false);

        // when
        MedicationScheduleListResponse response = MedicationScheduleListResponse.from(mockPage);

        // then
        assertThat(response.schedules()).isEmpty();
        assertThat(response.totalCount()).isEqualTo(0L);
        assertThat(response.currentPage()).isEqualTo(0);
        assertThat(response.pageSize()).isEqualTo(10);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("Should create from list without pagination")
    void fromList_WithSchedulesList_ShouldCreateResponseWithoutPagination() {
        // given
        List<MedicationScheduleResponse> schedules = sampleSchedules;

        // when
        MedicationScheduleListResponse response = MedicationScheduleListResponse.fromList(schedules);

        // then
        assertThat(response.schedules()).hasSize(2);
        assertThat(response.schedules()).isEqualTo(schedules);
        assertThat(response.totalCount()).isEqualTo(2L);
        assertThat(response.currentPage()).isEqualTo(0);
        assertThat(response.pageSize()).isEqualTo(2);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("Should create from empty list")
    void fromList_WithEmptyList_ShouldCreateResponse() {
        // given
        List<MedicationScheduleResponse> schedules = Collections.emptyList();

        // when
        MedicationScheduleListResponse response = MedicationScheduleListResponse.fromList(schedules);

        // then
        assertThat(response.schedules()).isEmpty();
        assertThat(response.totalCount()).isEqualTo(0L);
        assertThat(response.currentPage()).isEqualTo(0);
        assertThat(response.pageSize()).isEqualTo(0);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("Should create from large list")
    void fromList_WithLargeList_ShouldCreateResponse() {
        // given
        List<MedicationScheduleResponse> largeList = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            largeList.add(new MedicationScheduleResponse(
                    (long) i, 1L, "Medicine " + i, "Hospital " + i,
                    LocalDate.now(), "Memo " + i,
                    LocalDate.now().plusDays(1), 7, 1, AlarmType.SOUND
            ));
        }

        // when
        MedicationScheduleListResponse response = MedicationScheduleListResponse.fromList(largeList);

        // then
        assertThat(response.schedules()).hasSize(100);
        assertThat(response.totalCount()).isEqualTo(100L);
        assertThat(response.currentPage()).isEqualTo(0);
        assertThat(response.pageSize()).isEqualTo(100);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("Should maintain equality and hashCode contract")
    void record_ShouldMaintainEqualityAndHashCode() {
        // given
        List<MedicationScheduleResponse> schedules = sampleSchedules;
        
        MedicationScheduleListResponse response1 = new MedicationScheduleListResponse(
                schedules, 2L, 0, 10, false, false
        );
        MedicationScheduleListResponse response2 = new MedicationScheduleListResponse(
                schedules, 2L, 0, 10, false, false
        );

        // then
        assertThat(response1).isEqualTo(response2);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        assertThat(response1).isEqualTo(response1); // reflexive
    }

    @Test
    @DisplayName("Should not be equal when fields differ")
    void record_WithDifferentFields_ShouldNotBeEqual() {
        // given
        List<MedicationScheduleResponse> schedules = sampleSchedules;
        
        MedicationScheduleListResponse response1 = new MedicationScheduleListResponse(
                schedules, 2L, 0, 10, false, false
        );
        MedicationScheduleListResponse response2 = new MedicationScheduleListResponse(
                schedules, 3L, 0, 10, false, false
        );

        // then
        assertThat(response1).isNotEqualTo(response2);
        assertThat(response1.hashCode()).isNotEqualTo(response2.hashCode());
    }

    @Test
    @DisplayName("Should contain field information in toString")
    void toString_ShouldContainFieldInformation() {
        // given
        List<MedicationScheduleResponse> schedules = sampleSchedules;
        MedicationScheduleListResponse response = new MedicationScheduleListResponse(
                schedules, 2L, 1, 10, true, false
        );

        // when
        String toStringResult = response.toString();

        // then
        assertThat(toStringResult).contains("MedicationScheduleListResponse");
        assertThat(toStringResult).contains("schedules=");
        assertThat(toStringResult).contains("totalCount=2");
        assertThat(toStringResult).contains("currentPage=1");
        assertThat(toStringResult).contains("pageSize=10");
        assertThat(toStringResult).contains("hasNext=true");
        assertThat(toStringResult).contains("hasPrevious=false");
    }

    @Test
    @DisplayName("Should handle null schedules list")
    void constructor_WithNullSchedules_ShouldCreateResponse() {
        // given
        List<MedicationScheduleResponse> schedules = null;

        // when
        MedicationScheduleListResponse response = new MedicationScheduleListResponse(
                schedules, 0L, 0, 10, false, false
        );

        // then
        assertThat(response.schedules()).isNull();
        assertThat(response.totalCount()).isEqualTo(0L);
        assertThat(response.currentPage()).isEqualTo(0);
        assertThat(response.pageSize()).isEqualTo(10);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("Should create from single item list")
    void fromList_WithSingleItem_ShouldCreateResponse() {
        // given
        List<MedicationScheduleResponse> singleItemList = Arrays.asList(sampleSchedules.get(0));

        // when
        MedicationScheduleListResponse response = MedicationScheduleListResponse.fromList(singleItemList);

        // then
        assertThat(response.schedules()).hasSize(1);
        assertThat(response.schedules().get(0)).isEqualTo(sampleSchedules.get(0));
        assertThat(response.totalCount()).isEqualTo(1L);
        assertThat(response.currentPage()).isEqualTo(0);
        assertThat(response.pageSize()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("Should handle extreme pagination values")
    void constructor_WithExtremePaginationValues_ShouldCreateResponse() {
        // given
        List<MedicationScheduleResponse> schedules = sampleSchedules;

        // when
        MedicationScheduleListResponse response = new MedicationScheduleListResponse(
                schedules, Long.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, true, true
        );

        // then
        assertThat(response.schedules()).hasSize(2);
        assertThat(response.totalCount()).isEqualTo(Long.MAX_VALUE);
        assertThat(response.currentPage()).isEqualTo(Integer.MAX_VALUE);
        assertThat(response.pageSize()).isEqualTo(Integer.MAX_VALUE);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.hasPrevious()).isTrue();
    }
}