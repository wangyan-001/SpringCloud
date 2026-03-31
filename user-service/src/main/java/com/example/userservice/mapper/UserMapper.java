package com.example.userservice.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.example.userservice.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户Mapper接口：继承BaseMapper<User>后，无需写基础CRUD方法
 * @Repository 标识为持久层组件，让Spring扫描到
 */
@Repository
public interface UserMapper extends BaseMapper<User> {

    /**
     * 扣减用户余额的自定义方法
     * @param userId 用户ID
     * @param amount 要扣减的金额
     * @return 影响的行数（1=扣减成功，0=扣减失败）
     *  balance -
     */
    @Update("UPDATE t_user SET balance = #{amount} WHERE id = #{userId}")
    int updateBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);


    // 自定义SQL，接收Wrapper条件
    @Select("SELECT * FROM user ${ew.customSqlSegment}")
    List<User> selectByCustomSql(@Param(Constants.WRAPPER) QueryWrapper<User> wrapper);

    // ========== 基础CRUD（BaseMapper已提供，无需手写） ==========
    // insert(User user) → 新增
    // deleteById(Long id) → 根据ID删除
    // updateById(User user) → 根据ID更新
    // selectById(Long id) → 根据ID查询
    // selectList(Wrappers<User> queryWrapper) → 条件查询列表

    // ========== 自定义SQL（注解版） ==========
    /**
     * 根据手机号查询用户
     */
    @Select("SELECT * FROM user WHERE phone = #{phone} AND is_deleted = 0")
    User selectByPhone(String phone);

    /**
     * 批量更新用户状态（示例）
     */
    @Update("<script>" +
            "UPDATE user SET user_name = #{userName} WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    int batchUpdateUserName(List<Long> ids, String userName);
}