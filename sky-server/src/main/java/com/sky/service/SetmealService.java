package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;

public interface SetmealService {
    PageResult page(SetmealPageQueryDTO queryDTO);

    void add(SetmealDTO setmealDTO);

    SetmealVO getById(Long id);

    void startOrStop(Long id, Integer status);

    void update(SetmealDTO setmealDTO);

    void delete(Long[] ids);
}
