package org.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.server.entity.Address;
import org.server.dto.AddressDTO;
import org.server.vo.AddressVO;
import java.util.List;

public interface AddressService extends IService<Address> {
    // 1. 查询列表接口的返回值调整
    List<AddressVO.BaseVO> listAddressesByUserId(Long userId);

    // 2. 新增接口的形参和返回值调整
    AddressVO.BaseVO saveAddress(Long userId, AddressDTO.AddDTO dto);
    /**
     * 修改当前用户的指定收货地址
     * @param userId 当前登录用户 ID (用于越权校验)
     * @param addressId 路径传过来的地址 ID
     * @param dto 前端传入的修改数据
     * @return 修改成功后的 VO 对象
     */
    AddressVO.BaseVO updateAddress(Long userId, Long addressId, AddressDTO.UpdateDTO dto);
    /**
     * 删除当前用户的指定收货地址
     * @param userId 当前登录用户 ID (用于越权校验)
     * @param addressId 路径传过来的地址 ID
     */
    void deleteAddress(Long userId, Long addressId);
}