package com.xuecheng.content.service.impl;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseCategoryServiceImplTest {

    @Mock
    private CourseCategoryMapper mockCategoryMapper;

    @InjectMocks
    private CourseCategoryServiceImpl courseCategoryServiceImplUnderTest;

    @Test
    void testQueryTreeNodes() {
        // Setup
        // Configure CourseCategoryMapper.selectTreeNodes(...).
        final CourseCategoryTreeDto courseCategoryTreeDto = new CourseCategoryTreeDto();
        courseCategoryTreeDto.setId("id");
        courseCategoryTreeDto.setChildrenTreeNodes(Arrays.asList(new CourseCategoryTreeDto()));
        final List<CourseCategoryTreeDto> courseCategoryTreeDtos = Arrays.asList(courseCategoryTreeDto);
        when(mockCategoryMapper.selectTreeNodes("id")).thenReturn(courseCategoryTreeDtos);

        // Run the test
        final List<CourseCategoryTreeDto> result = courseCategoryServiceImplUnderTest.queryTreeNodes("id");

        // Verify the results
        assertThat(result).isNull();
    }

    @Test
    void testQueryTreeNodes_CourseCategoryMapperReturnsNoItems() {
        // Setup
        when(mockCategoryMapper.selectTreeNodes("id")).thenReturn(Collections.emptyList());

        // Run the test
        final List<CourseCategoryTreeDto> result = courseCategoryServiceImplUnderTest.queryTreeNodes("id");

        // Verify the results
        assertThat(result).isNull();
    }
}
