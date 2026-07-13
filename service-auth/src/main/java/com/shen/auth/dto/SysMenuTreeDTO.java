package com.shen.auth.dto;

import lombok.Data;

import java.util.List;

/**
 * 菜单树形结构DTO
 */
@Data
public class SysMenuTreeDTO {

    private Long id;

    private Long parentId;

    private String menuName;

    private String permissionCode;

    private String menuType;

    private String path;

    private String icon;

    private Integer sort;

    private Integer status;

    private List<SysMenuTreeDTO> children;
}