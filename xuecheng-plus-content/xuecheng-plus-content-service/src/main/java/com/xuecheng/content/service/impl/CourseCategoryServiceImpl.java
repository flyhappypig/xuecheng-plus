package com.xuecheng.content.service.impl;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author gushouye
 * @description
 **/
@Service
@Slf4j
public class CourseCategoryServiceImpl implements CourseCategoryService {


    @Autowired
    private CourseCategoryMapper categoryMapper;

    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {
        // 调用mapper递归查询出分类信息
        List<CourseCategoryTreeDto> treeDtos = categoryMapper.selectTreeNodes(id);
        // 找到每个节点的子节点，封装成List<CourseCategoryTreeDto>
        // 将list转成map，key-->节点id，value-->为CourseCategoryTreeDto对象，目的就是为了从map获取节点
        // filter(item -> !id.equals(item.getId()))--->排除根节点
        Map<String, CourseCategoryTreeDto> mapTemp = treeDtos.stream().filter(item -> !id.equals(item.getId())).collect(Collectors.toMap(key -> key.getId(), value -> value, (key1, key2) -> key2));
        // 定义一个list，作为最终返回的list
        List<CourseCategoryTreeDto> categoryTreeDtoArrayList = new ArrayList<>();
        // 从头遍历 treeDtos，一遍遍历，一边找子节点，放在childrenTreeNodes中
        treeDtos.stream().filter(item -> !id.equals(item.getId())).forEach(item -> {
            if (item.getParentid().equals(id)) {
                categoryTreeDtoArrayList.add(item);
            }
            // 找到节点的父节点
            CourseCategoryTreeDto courseCategoryTreeParent = mapTemp.get(item.getParentid());
            if (courseCategoryTreeParent != null) {
                if (courseCategoryTreeParent.getChildrenTreeNodes() == null) {
                    // 如果该父节点的ChildrenTreeNodes属性为空要new一个集合
                    // 因为要向该集合中放它的子节点
                    courseCategoryTreeParent.setChildrenTreeNodes(new ArrayList<CourseCategoryTreeDto>());
                }
                // 找到每个节点的子节点放到childrenTreeNodes属性中
                courseCategoryTreeParent.getChildrenTreeNodes().add(item);
            }
        });
        return categoryTreeDtoArrayList;
    }
}
