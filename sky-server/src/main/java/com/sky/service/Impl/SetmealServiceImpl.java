package com.sky.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private DishMapper dishMapper;

    @Override
    public PageResult page(SetmealPageQueryDTO queryDTO) {
        PageHelper.startPage(queryDTO.getPage(), queryDTO.getPageSize());

        Page<SetmealVO> page =  setmealMapper.page(queryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.insert(setmeal);
        for (SetmealDish setmealDish : setmealDTO.getSetmealDishes()) {
            setmealDish.setSetmealId(setmeal.getId());
            setmealDishMapper.insert(setmealDish);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public SetmealVO getById(Long id) {
        SetmealVO setmealVO = new SetmealVO();
        Setmeal setmeal = setmealMapper.selectById(id);
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishMapper.selectList(new LambdaQueryWrapper<SetmealDish>().eq(SetmealDish::getSetmealId, id)));
        log.info("setmealVO: {}", setmealVO);

        return setmealVO;
    }

    @Override
    public void startOrStop(Long id, Integer status) {
        Setmeal setmeal = Setmeal.builder().id(id).status(status).build();
        setmealMapper.updateById(setmeal);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(SetmealDTO setmealDTO) {
        setmealDishMapper.delete(new LambdaQueryWrapper<SetmealDish>().eq(SetmealDish::getSetmealId, setmealDTO.getId()));
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.updateById(setmeal);
        for (SetmealDish setmealDish : setmealDTO.getSetmealDishes()) {
            setmealDish.setSetmealId(setmeal.getId());
            setmealDishMapper.insert(setmealDish);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return;
        }
        setmealMapper.delete(new LambdaQueryWrapper<Setmeal>().in(Setmeal::getId, ids));
        for (Long id : ids) {
            setmealDishMapper.delete(new LambdaQueryWrapper<SetmealDish>().in(SetmealDish::getSetmealId, id));
        }
    }

    @Override
    public List<SetmealVO> list(Setmeal setmeal) {
        SetmealPageQueryDTO queryDTO = new SetmealPageQueryDTO();
        BeanUtils.copyProperties(setmeal, queryDTO);
        return setmealMapper.page(queryDTO);
    }

    @Override
    public List<DishItemVO> getDishItemsById(Long id) {
        return setmealMapper.getDishItemById(id);
    }
}
