package com.cinx.learning.catalog.application;

import com.cinx.common.exception.NotFoundException;
import com.cinx.learning.catalog.application.command.CreateCategoryCommand;
import com.cinx.learning.catalog.application.command.UpdateCategoryCommand;
import com.cinx.learning.catalog.application.query.CategoryView;
import com.cinx.learning.catalog.application.query.CategorySummaryView;
import com.cinx.learning.catalog.application.mapper.CategoryMapper;
import com.cinx.learning.catalog.domain.Category;
import com.cinx.learning.catalog.domain.CategoryRepository;
import com.cinx.learning.catalog.domain.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService implements ICategoryService {
    private final CategoryRepository categoryRepository;
    private final CourseRepository courseRepository;
    private final CategoryMapper categoryMapper;

    private Category getCategoryEntityById(String id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategorySummaryView> getAllCategories() {
        Map<String, Long> publishedCounts = courseRepository.countPublishedByCategory().stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> ((Number) row[1]).longValue()));
        return categoryRepository.findAll()
                .stream()
                .map(category -> new CategorySummaryView(
                        category.getId(),
                        category.getName(),
                        publishedCounts.getOrDefault(category.getId(), 0L)))
                .toList();
    }

    @Override
    public CategoryView getCategoryById(String id) {
        return categoryMapper.toDto(getCategoryEntityById(id));
    }

    @Override
    public void createCategory(CreateCategoryCommand request) {
        categoryRepository.save(categoryMapper.toModel(request));
    }

    @Override
    public void updateCategory(String id, UpdateCategoryCommand request) {
        Category category = getCategoryEntityById(id);
        categoryMapper.partialUpdate(category, request);
        categoryRepository.save(category);
    }

    @Override
    public void deleteCategory(String id) {
        Category category = getCategoryEntityById(id);
        categoryRepository.delete(category);
    }
}

