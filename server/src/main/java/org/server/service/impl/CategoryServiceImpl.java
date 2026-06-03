package org.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.server.dto.CategoryDTO;
import org.server.entity.Product;
import org.server.mapper.ProductMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.server.entity.Category;
import org.server.mapper.CategoryMapper;
import org.server.service.CategoryService;
import org.server.vo.CategoryVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ProductMapper productMapper;

    @Override
    public List<CategoryVO> getCategoryTree() {
        // 1. 一次性从数据库查询出【所有】分类数据（MyBatis-Plus 会自动在此处过滤掉 is_deleted = 1 的垃圾数据）
        List<Category> allCategories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort)
        );

        // 2. 将普通的 Entity 集合转换成 VO 集合
        List<CategoryVO> allVOList = allCategories.stream().map(category -> {
            CategoryVO vo = new CategoryVO();
            BeanUtils.copyProperties(category, vo);
            return vo;
        }).collect(Collectors.toList());

        // 3. 找出所有的一级分类（parentId == 0），并递归设置它们的子分类
        List<CategoryVO> treeNodes = allVOList.stream()
                .filter(vo -> vo.getParentId() != null && vo.getParentId() == 0L)
                .map(rootNode -> {
                    // 递归寻找当前一级分类的子孙
                    rootNode.setChildren(getChildrenNodes(rootNode, allVOList));
                    return rootNode;
                })
                .collect(Collectors.toList());

        return treeNodes;
    }

    /**
     * 辅助递归方法：在全量数据中，寻找某个节点的子分类节点
     */
    private List<CategoryVO> getChildrenNodes(CategoryVO currentNode, List<CategoryVO> allNodes) {
        return allNodes.stream()
                .filter(node -> node.getParentId() != null && node.getParentId().equals(currentNode.getId()))
                .map(childNode -> {
                    // 递归向下寻找（支持三级、四级等无限层级）
                    childNode.setChildren(getChildrenNodes(childNode, allNodes));
                    return childNode;
                })
                .collect(Collectors.toList());
    }

    /**
     * 实现新增分类方法
     */
    @Override
    @Transactional(rollbackFor = Exception.class) // 涉及写操作，开启事务保障
    public CategoryVO.Simple createCategory(CategoryDTO.Create createDTO) {
        Category category = new Category();
        BeanUtils.copyProperties(createDTO, category);

        // 执行完 insert 后，MyBatis-Plus 会自动将自增 id 回填
        categoryMapper.insert(category);

        CategoryVO.Simple resultVO = new CategoryVO.Simple();
        BeanUtils.copyProperties(category, resultVO);

        return resultVO;
    }

    /**
     * 实现安全修改分类
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryVO.Simple updateCategory(CategoryDTO.Update updateDTO) {
        Long currentId = updateDTO.getId();
        Long targetParentId = updateDTO.getParentId();

        // 1. 基础校验：不能把自己设为自己的父分类
        if (Objects.equals(currentId, targetParentId)) {
            throw new IllegalArgumentException("不能将分类的父级设置为它本身");
        }

        // 2. 核心校验：防止循环父子关系（不能变成自己子孙的子分类）
        if (targetParentId != null && targetParentId != 0L) {

            Long currentParentCheckId = targetParentId;
            int safetyCount = 0;

            while (currentParentCheckId != 0L) {
                if (Objects.equals(currentParentCheckId, currentId)) {
                    throw new IllegalArgumentException("修改失败：不能将当前分类移动到其子分类下，这将导致循环依赖！");
                }

                Category parentCategory = categoryMapper.selectById(currentParentCheckId);
                if (parentCategory == null) {
                    break;
                }

                currentParentCheckId = parentCategory.getParentId();

                if (++safetyCount > 20) {
                    throw new IllegalStateException("检测到系统存在历史循环依赖漏洞，拒绝修改！");
                }
            }
        }

        // 3. 校验通过，锁定当前分类数据并更新
        Category category = categoryMapper.selectById(currentId);
        if (category == null) {
            throw new IllegalArgumentException("要修改的分类不存在");
        }

        BeanUtils.copyProperties(updateDTO, category);
        categoryMapper.updateById(category);

        CategoryVO.Simple resultVO = new CategoryVO.Simple();
        BeanUtils.copyProperties(category, resultVO);
        return resultVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 涉及写操作，开启事务保证
    public void deleteCategoryById(Long id) {
        // 1. 先验证当前分类在数据库里是否存在（若被逻辑删除，此处理解查出为 null）
        Category currentCategory = categoryMapper.selectById(id);
        if (currentCategory == null) {
            throw new IllegalArgumentException("该分类不存在或已被删除");
        }

        // 2. 【核心拦截一】校验是否存在子分类
        Long childCount = categoryMapper.selectCount(
                new LambdaQueryWrapper<Category>().eq(Category::getParentId, id)
        );
        if (childCount != null && childCount > 0) {
            throw new IllegalArgumentException("存在子分类，无法删除");
        }

        // 3. 【核心拦截二】校验该分类下是否存在【未删除】的商品
        // 移除了 .eq(Product::getDelFlag, 0)。因为 Product 实体类配了 @TableLogic，
        // MyBatis-Plus 在执行 selectCount 时会自动在末尾加上 `AND is_deleted = 0`，完美做到动态盘点！
        Long productCount = productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getCategoryId, id)
        );
        if (productCount != null && productCount > 0) {
            throw new IllegalArgumentException("该分类下存在商品，禁止删除");
        }
        categoryMapper.deleteById(id);
    }
}