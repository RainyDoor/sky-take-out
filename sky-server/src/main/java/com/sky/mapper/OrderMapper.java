package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Mapper
public interface OrderMapper extends BaseMapper<Orders> {

    List<OrderDetail> selectTop10(LocalDateTime begin, LocalDateTime end);

}
