package com.cinx.learning.catalog.application;

import com.cinx.learning.catalog.application.command.CreateCategoryCommand;
import com.cinx.learning.catalog.application.command.UpdateCategoryCommand;
import com.cinx.learning.catalog.application.query.CategoryView;
import com.cinx.learning.catalog.application.query.CategorySummaryView;

import java.util.List;

public interface ICategoryService {
    List<CategorySummaryView> getAllCategories();
    CategoryView getCategoryById(String id);
    void createCategory(CreateCategoryCommand request);
    void updateCategory(String id, UpdateCategoryCommand request);
    void deleteCategory(String id);
}

