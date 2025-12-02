package org.drinkless.robots.beans.view.search;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.drinkless.robots.database.enums.AuditStatusEnum;

import java.util.List;

@Setter
@Getter
@Accessors(chain = true)
public class AuditRequest {
    private AuditStatusEnum operation;
    private List<String> ids;
    private String remark;
}

