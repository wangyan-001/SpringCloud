package com.example.order_service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.order_service.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单Mapper接口，继承MyBatis-Plus的BaseMapper，无需手写CRUD方法
 */
@Mapper // 关键注解：让Spring识别为MyBatis映射器，解决注入问题
public interface OrderMapper extends BaseMapper<Order> {

    // BaseMapper已经内置了insert/delete/update/select等方法，比如：
    // insert(Order entity) → 新增订单
    // selectById(Long id) → 根据ID查订单
    // 你不需要额外写方法，直接用即可
}