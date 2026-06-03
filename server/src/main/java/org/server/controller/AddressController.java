package org.server.controller;

import org.server.common.Result;
import org.server.dto.AddressDTO;
import org.server.vo.AddressVO;
import org.server.service.AddressService;
import org.server.utils.BaseContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/address")
public class AddressController {

    @Autowired
    private AddressService addressService;

    /**
     * 获取当前用户所有地址
     * GET /address/list
     */
    @GetMapping("/list")
    public Result<List<AddressVO.BaseVO>> list() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("用户未登录或登录已过期");
        }

        List<AddressVO.BaseVO> addressVOList = addressService.listAddressesByUserId(userId);
        return Result.success("获取地址成功", addressVOList);
    }

    /**
     * 新增当前用户地址
     * POST /address
     */
    @PostMapping
    public Result<AddressVO.BaseVO> add(@RequestBody @Validated AddressDTO.AddDTO addDTO) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("用户未登录或登录已过期");
        }

        AddressVO.BaseVO addressVO = addressService.saveAddress(userId, addDTO);
        return Result.success("地址新增成功", addressVO);
    }
    /**
     * 修改用户地址
     * PUT /address/{id}
     */
    @PutMapping("/{id}")
    public Result<AddressVO.BaseVO> update(
            @PathVariable("id") Long addressId,                       // 🎯 自动抓取路径里的 id
            @RequestBody @Validated AddressDTO.UpdateDTO updateDTO    // 🎯 接收请求体并启动拦截安检
    ) {
        // 1. 从线程口袋里拿到真实可靠的用户 ID
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("用户未登录或登录已过期");
        }

        // 2. 调用 Service 进行越权校验并执行修改
        AddressVO.BaseVO addressVO = addressService.updateAddress(userId, addressId, updateDTO);

        // 3. 返回响应，把错误的 "地址新增成功" 优雅修正为 "地址修改成功"
        return Result.success("地址修改成功", addressVO);
    }
    /**
     * 删除用户地址
     * DELETE /address/{id}
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable("id") Long addressId) {
        // 1. 从线程口袋里拿到真实可靠的用户 ID
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            return Result.error("用户未登录或登录已过期");
        }

        // 2. 调用 Service 进行安全校验并执行删除
        addressService.deleteAddress(userId, addressId);

        // 3. 返回标准的成功提示
        return Result.success("地址删除成功");
    }
}