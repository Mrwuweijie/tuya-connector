package com.tuya.connector.ability.acl.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * @author benguan.zhou@tuya.com
 * @description
 * @date 2021/05/24
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AclUser {

    /**
     * 用户uid
     */
    String uid;

    /**
     * 外部用户名
     */
    String username;

    /**
     * 备注
     */
    String remark;

    String spaceId;

    String roleCode;

    String roleName;

    String gmtCreate;

}
