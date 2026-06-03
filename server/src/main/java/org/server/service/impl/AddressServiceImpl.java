package org.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.server.common.exception.BusinessException;
import org.server.entity.Address;
import org.server.mapper.AddressMapper;
import org.server.service.AddressService;
import org.server.dto.AddressDTO;
import org.server.vo.AddressVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements AddressService {

    @Override
    public List<AddressVO.BaseVO> listAddressesByUserId(Long userId) {
        List<Address> addressList = this.lambdaQuery()
                .eq(Address::getUserId, userId)
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getUpdatedAt)
                .list();

        List<AddressVO.BaseVO> voList = new ArrayList<>();
        for (Address address : addressList) {
            AddressVO.BaseVO vo = new AddressVO.BaseVO(); // 🎯 实例化内部类
            BeanUtils.copyProperties(address, vo);
            voList.add(vo);
        }
        return voList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO.BaseVO saveAddress(Long userId, AddressDTO.AddDTO dto) {
        // 如果设置为默认地址，先把以往的默认地址全部置为 0
        if (dto.getIsDefault() != null && dto.getIsDefault() == 1) {
            this.lambdaUpdate()
                    .eq(Address::getUserId, userId)
                    .eq(Address::getIsDefault, 1)
                    .set(Address::getIsDefault, 0)
                    .update();
        }

        Address address = new Address();
        BeanUtils.copyProperties(dto, address);
        address.setUserId(userId);

        this.save(address);

        AddressVO.BaseVO vo = new AddressVO.BaseVO();
        BeanUtils.copyProperties(address, vo);
        return vo;
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO.BaseVO updateAddress(Long userId, Long addressId, AddressDTO.UpdateDTO dto) {
        // 1. 安全第一：先查出数据库原本的地址记录
        Address oldAddress = this.getById(addressId);
        if (oldAddress == null) {
            throw new BusinessException(404, "该地址不存在");
        }

        // 2. 防越权核心：判断这个地址的 user_id 是否等于当前登录的 userId
        if (!oldAddress.getUserId().equals(userId)) {
            throw new BusinessException(403, "你无权修改他人的收货地址！");
        }

        // 3. 核心业务：如果当前地址被改成了默认地址 (isDefault == 1)
        if (dto.getIsDefault() != null && dto.getIsDefault() == 1) {
            // 将该用户旗下原本所有的默认地址全部更新为 0
            this.lambdaUpdate()
                    .eq(Address::getUserId, userId)
                    .eq(Address::getIsDefault, 1)
                    .set(Address::getIsDefault, 0)
                    .update();
        }

        // 4. 将 DTO 的新属性拷贝覆盖到旧的实体中
        BeanUtils.copyProperties(dto, oldAddress);
        // 确保 id 和 userId 依然是这条数据的原始信息，防止被覆盖
        oldAddress.setId(addressId);
        oldAddress.setUserId(userId);

        // 5. 更新数据库
        this.updateById(oldAddress);

        // 6. 转化并返回 VO
        AddressVO.BaseVO vo = new AddressVO.BaseVO();
        BeanUtils.copyProperties(oldAddress, vo);
        return vo;
    }
    @Override
    @Transactional(rollbackFor = Exception.class) // 涉及删除和潜在的自动更新，必须加事务
    public void deleteAddress(Long userId, Long addressId) {
        // 1. 先查出数据库原本的地址记录
        Address address = this.getById(addressId);
        if (address == null) {
            throw new BusinessException(404, "该地址不存在");
        }

        // 2.  判断这个地址到底是不是当前登录人的
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException(403, "你无权删除他人的收货地址！");
        }

        // 3. 执行删除
        this.removeById(addressId);

        // 4. 如果刚刚删掉的是默认地址 (isDefault == 1)
        if (address.getIsDefault() == 1) {
            // 查出该用户剩下的所有地址中，最新修改的那一条
            Address newestAddress = this.lambdaQuery()
                    .eq(Address::getUserId, userId)
                    .orderByDesc(Address::getUpdatedAt)
                    .last("LIMIT 1") // 只取一条
                    .one();

            // 如果还有剩余地址，把这条最新地址自动顶替为默认地址
            if (newestAddress != null) {
                newestAddress.setIsDefault(1);
                this.updateById(newestAddress);
            }
        }
    }
}