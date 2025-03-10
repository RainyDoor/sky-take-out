package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {
    void add(DishDTO dishDTO);

    PageResult page(DishPageQueryDTO dishPageQueryDTO);

    void startOrStop(Integer status, Long id);

    DishVO getById(Long id);

    List<DishVO> list(Long categoryId);

    void update(DishDTO dishDTO);

    void delete(Long[] ids);
}
