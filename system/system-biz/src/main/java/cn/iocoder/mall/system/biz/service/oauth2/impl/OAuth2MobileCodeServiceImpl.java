package cn.iocoder.mall.system.biz.service.oauth2.impl;

import cn.iocoder.common.framework.constant.SysErrorCodeEnum;
import cn.iocoder.common.framework.util.ServiceExceptionUtil;
import cn.iocoder.common.framework.util.ValidationUtil;
import cn.iocoder.mall.system.biz.constant.SystemErrorCodeEnum;
import cn.iocoder.mall.system.biz.dao.oauth2.OAuth2MobileCodeMapper;
import cn.iocoder.mall.system.biz.dataobject.oauth2.OAuth2MobileCodeDO;
import cn.iocoder.mall.system.biz.dto.oatuh2.OAuth2MobileCodeSendDTO;
import cn.iocoder.mall.system.biz.service.oauth2.OAuth2MobileCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class OAuth2MobileCodeServiceImpl implements OAuth2MobileCodeService {

    /**
     * 每条验证码的过期时间，单位：毫秒
     */
    @Value("${modules.oauth2-mobile-code-service.code-expire-time-millis}")
    private int codeExpireTimes;
    /**
     * 每日发送最大数量
     */
    @Value("${modules.oauth2-mobile-code-service.send-maximum-quantity-per-day}")
    private int sendMaximumQuantityPerDay;
    /**
     * 短信发送频率，单位：毫秒
     */
    @Value("${modules.oauth2-mobile-code-service.send-frequency}")
    private int sendFrequency;

    @Autowired
    private OAuth2MobileCodeMapper oauth2MobileCodeMapper;

    @Override
    public void sendMobileCode(OAuth2MobileCodeSendDTO sendDTO) {
        if (!ValidationUtil.isMobile(sendDTO.getMobile())) {
            throw ServiceExceptionUtil.exception(SysErrorCodeEnum.VALIDATION_REQUEST_PARAM_ERROR.getCode(), "手机格式不正确"); // TODO 有点搓
        }
        // 校验是否可以发送验证码
        OAuth2MobileCodeDO lastMobileCodePO = oauth2MobileCodeMapper.selectLastByMobile(sendDTO.getMobile());
        if (lastMobileCodePO != null) {
            if (lastMobileCodePO.getTodayIndex() >= sendMaximumQuantityPerDay) { // 超过当天发送的上限。
                throw ServiceExceptionUtil.exception(SystemErrorCodeEnum.OAUTH2_MOBILE_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_DAY.getCode());
            }
            if (System.currentTimeMillis() - lastMobileCodePO.getCreateTime().getTime() < sendFrequency) { // 发送过于频繁
                throw ServiceExceptionUtil.exception(SystemErrorCodeEnum.OAUTH2_MOBILE_CODE_SEND_TOO_FAST.getCode());
            }
            // TODO 提升，每个 IP 每天可发送数量
            // TODO 提升，每个 IP 每小时可发送数量
        }
        // 创建验证码记录
        OAuth2MobileCodeDO newMobileCodePO = new OAuth2MobileCodeDO().setMobile(sendDTO.getMobile())
                .setCode("9999") // TODO 芋艿，随机 4 位验证码 or 6 位验证码
                .setTodayIndex(lastMobileCodePO != null ? lastMobileCodePO.getTodayIndex() : 1)
                .setCreateIp(sendDTO.getIp())
                .setUsed(false);
        newMobileCodePO.setCreateTime(new Date());
        oauth2MobileCodeMapper.insert(newMobileCodePO);
        // TODO 发送验证码短信
    }

}