package com.cinx.learning.catalog.application.mapper;

import com.cinx.common.mapper.BaseMapper;
import com.cinx.common.mapper.CreateMapper;
import com.cinx.common.mapper.UpdateMapper;
import com.cinx.learning.catalog.application.command.CreateCategoryCommand;
import com.cinx.learning.catalog.application.command.UpdateCategoryCommand;
import com.cinx.learning.catalog.application.query.CategoryView;
import com.cinx.learning.catalog.domain.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper extends BaseMapper<Category, CategoryView>, UpdateMapper<Category, UpdateCategoryCommand>, CreateMapper<Category, CreateCategoryCommand> {
}

