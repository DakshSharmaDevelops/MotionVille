package org.example.motionville.services.video;

import org.example.motionville.dto.video.CategoryResponse;
import org.example.motionville.dto.video.CategoryRequest;
import org.example.motionville.entity.video.Category;
import org.example.motionville.repo.video.CategoryRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final VideoRepository videoRepository;

    public CategoryService(CategoryRepository categoryRepository, VideoRepository videoRepository) {
        this.categoryRepository = categoryRepository;
        this.videoRepository = videoRepository;
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String name = request.name().trim();
        String slug = normalizeSlug(request.slug());
        ensureUnique(name, slug, null);

        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        return toResponse(categoryRepository.save(category));
    }

    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public CategoryResponse getCategory(Long id) {
        return toResponse(findCategory(id));
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = findCategory(id);
        String name = request.name().trim();
        String slug = normalizeSlug(request.slug());
        ensureUnique(name, slug, id);

        category.setName(name);
        category.setSlug(slug);
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = findCategory(id);
        if (videoRepository.existsByCategory_Id(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Category is assigned to one or more videos"
            );
        }
        categoryRepository.delete(category);
    }

    private void ensureUnique(String name, String slug, Long currentId) {
        boolean duplicateName = currentId == null
                ? categoryRepository.existsByNameIgnoreCase(name)
                : categoryRepository.existsByNameIgnoreCaseAndIdNot(name, currentId);
        boolean duplicateSlug = currentId == null
                ? categoryRepository.existsBySlugIgnoreCase(slug)
                : categoryRepository.existsBySlugIgnoreCaseAndIdNot(slug, currentId);

        if (duplicateName || duplicateSlug) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    duplicateName ? "Category name already exists" : "Category slug already exists"
            );
        }
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    private String normalizeSlug(String slug) {
        return slug.trim().toLowerCase(Locale.ROOT);
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug());
    }
}
