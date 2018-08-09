/*
 * <<
 * Davinci
 * ==
 * Copyright (C) 2016 - 2018 EDP
 * ==
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * >>
 */

package edp.davinci.dto.projectDto;

import edp.davinci.dto.userDto.UserBaseInfo;
import edp.davinci.model.User;
import lombok.Data;

@Data
public class ProjectInfo {
    private Long id;

    private String name;

    private String description;

    private String pic;

    private Long orgId;

    private Boolean visibility;

    private ProjectPermission permission = new ProjectPermission();

    private UserBaseInfo createBy;
}
