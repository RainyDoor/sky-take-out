package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService {
    PageResult page(SetmealPageQueryDTO queryDTO);

    void add(SetmealDTO setmealDTO);

    SetmealVO getById(Long id);

    void startOrStop(Long id, Integer status);

    void update(SetmealDTO setmealDTO);

    void delete(Long[] ids);

    List<SetmealVO> list(SetmealPageQueryDTO queryDTO);

    List<DishItemVO> getDishItemsById(Long id);
}
