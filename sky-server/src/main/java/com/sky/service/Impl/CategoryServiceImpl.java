package com.sky.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.constant.StatusConstant;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.mapper.CategoryMapper;
import com.sky.result.PageResult;
import com.sky.service.CategoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public void addCategory(CategoryDTO categoryDTO) {
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);

        //默认设置禁用
        category.setStatus(StatusConstant.DISABLE);

        categoryMapper.insert(category);
    }

    @Override
    public PageResult page(CategoryPageQueryDTO categoryPageQueryDTO) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(categoryPageQueryDTO.getName() != null, Category::getName, categoryPageQueryDTO.getName());
        wrapper.eq(categoryPageQueryDTO.getType() != null, Category::getType, categoryPageQueryDTO.getType());
        wrapper.orderByAsc(Category::getSort);

        IPage<Category> iPage = new Page<>(categoryPageQueryDTO.getPage(), categoryPageQueryDTO.getPageSize());
        categoryMapper.selectPage(iPage, wrapper);

        return new PageResult(iPage.getTotal(), iPage.getRecords());
    }

    @Override
    public void update(CategoryDTO categoryDTO) {
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);

        categoryMapper.updateById(category);
    }

    @Override
    public void startOrStop(Integer status, Long id) {
        Category category = Category.builder().id(id).status(status).build();
        categoryMapper.updateById(category);
    }

    @Override
    public List<Category> list(Integer type) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(type != null, Category::getType, type);
        return categoryMapper.selectList(wrapper);
    }

    @Override
    public void delete(Long id) {
        categoryMapper.deleteById(id);
    }

}
