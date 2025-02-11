package com.example.rule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.rule.model.FmPolicyRules;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
@Mapper
public interface FmPolicyRuleMapper extends BaseMapper<FmPolicyRules> {
    @Select("SELECT * FROM fm_policy_rules_new WHERE event_id = #{eventID} AND title = #{title}")
    List<FmPolicyRules> findByEventIDAndTitle(@Param("eventID") String eventID, @Param("title") String title);
}
