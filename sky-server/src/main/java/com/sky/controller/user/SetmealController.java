package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("userSetmealController")
@RequestMapping("/user/setmeal")
public class SetmealController {

    @Autowired
    private SetmealService setMealService;

    /**
     * 根据分类id查询套餐
     * @param categoryId
     * @return
     */
    @Cacheable(cacheNames = "setmealCache", key = "#categoryId")
    @GetMapping("/list")
    @ApiOperation("根据分类id查询套餐")
    public Result<List<SetmealVO>> list(Long categoryId) {

        Setmeal setmeal = new Setmeal();
        setmeal.setCategoryId(categoryId);
        setmeal.setStatus(StatusConstant.ENABLE);

        return Result.success(setMealService.list(setmeal));
    }


    /**
     * 根据id查询套餐
     * @param id
     * @return
     */
    @GetMapping("/dish/{id}")
    public Result<List<DishItemVO>> getDishItemsById(@PathVariable Long id) {
        return Result.success(setMealService.getDishItemsById(id));
    }


}
