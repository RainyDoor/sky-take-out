package com.sky.controller.user;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("userSetmealController")
@RequestMapping("/user/setmeal")
public class SetmealController {

    @Autowired
    private SetmealService setMealService;

    /**
     * 分页查询
     * @param queryDTO
     * @return
     */
    @GetMapping("/list")
    public Result<List<SetmealVO>> list(SetmealPageQueryDTO queryDTO) {
        return Result.success(setMealService.list(queryDTO));
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
