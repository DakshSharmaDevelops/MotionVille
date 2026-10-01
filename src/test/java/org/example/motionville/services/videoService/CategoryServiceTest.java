package org.example.motionville.services.videoService;

import org.example.motionville.dto.CategoryRequest;
import org.example.motionville.dto.CategoryResponse;
import org.example.motionville.entity.video.Category;
import org.example.motionville.repo.video.CategoryRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class CategoryServiceTest {

    private CategoryRepository categories;
    private VideoRepository videos;
    private CategoryService service;
    private Category category;

    @BeforeEach
    void setUp() {
        categories = mock(CategoryRepository.class);
        videos = mock(VideoRepository.class);
        service = new CategoryService(categories, videos);
        category = new Category();
        category.setId(7L);
        category.setName("Technology");
        category.setSlug("technology");
        category.setDescription("Technology");
        when(categories.findById(7L)).thenReturn(Optional.of(category));
        when(categories.save(any(Category.class))).thenAnswer(invocation -> {
            Category saved = invocation.getArgument(0);
            if (saved.getId() == null) saved.setId(7L);
            return saved;
        });
    }

    @Test
    void createsTrimmedCategoryAndReturnsIdNameAndSlug() {
        when(categories.existsByNameIgnoreCase("Technology")).thenReturn(false);
        when(categories.existsBySlugIgnoreCase("technology")).thenReturn(false);

        var response = service.createCategory(new CategoryRequest(" Technology ", "Technology"));

        assertEquals(new CategoryResponse(7L, "Technology", "technology"), response);
        verify(categories).save(argThat(saved -> "Technology".equals(saved.getDescription())));
    }

    @Test
    void rejectsDuplicateNameOrSlug() {
        when(categories.existsByNameIgnoreCase("Technology")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createCategory(new CategoryRequest("Technology", "technology")));

        assertEquals(409, exception.getStatusCode().value());
        verify(categories, never()).save(any());
    }

    @Test
    void rejectsDuplicateSlug() {
        when(categories.existsByNameIgnoreCase("Other")).thenReturn(false);
        when(categories.existsBySlugIgnoreCase("technology")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createCategory(new CategoryRequest("Other", "technology")));

        assertEquals(409, exception.getStatusCode().value());
        verify(categories, never()).save(any());
    }

    @Test
    void updatesCategoryAndAllowsItsExistingNameAndSlug() {
        when(categories.existsByNameIgnoreCaseAndIdNot("Technology", 7L)).thenReturn(false);
        when(categories.existsBySlugIgnoreCaseAndIdNot("technology", 7L)).thenReturn(false);

        var response = service.updateCategory(7L, new CategoryRequest("Technology", "technology"));

        assertEquals(7L, response.id());
        assertEquals("technology", response.slug());
    }

    @Test
    void getsCategoryById() {
        var response = service.getCategory(7L);

        assertEquals(7L, response.id());
        assertEquals("Technology", response.name());
        assertEquals("technology", response.slug());
    }

    @Test
    void deletesUnusedCategory() {
        when(videos.existsByCategory_Id(7L)).thenReturn(false);

        service.deleteCategory(7L);

        verify(categories).delete(category);
    }

    @Test
    void refusesToDeleteCategoryUsedByVideos() {
        when(videos.existsByCategory_Id(7L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.deleteCategory(7L));

        assertEquals(409, exception.getStatusCode().value());
        verify(categories, never()).delete(category);
    }
}
