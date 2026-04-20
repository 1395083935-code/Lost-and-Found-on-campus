package com.campuslostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campuslostfound.entity.LostFoundItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LostAndFoundItemMapper extends BaseMapper<LostFoundItem> {
}