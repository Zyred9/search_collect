package org.drinkless.robots.database.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.drinkless.robots.beans.view.base.PageResult;
import org.drinkless.robots.beans.view.search.AuditRequest;
import org.drinkless.robots.beans.view.search.SearchBean;
import org.drinkless.robots.database.enums.AuditStatusEnum;
import org.drinkless.robots.database.enums.SourceTypeEnum;
import org.drinkless.robots.database.service.AccountService;
import org.drinkless.robots.database.service.SearchService;
import org.drinkless.robots.database.service.TdService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TdController.class)
public class TdControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private TdService tdService;
    @MockBean private SearchService searchService;
    @MockBean private AccountService accountService;

    @Test
    void testPageSearch() throws Exception {
        SearchBean bean = new SearchBean()
                .setId("1")
                .setType(SourceTypeEnum.TEXT)
                .setSourceName("测试记录");
        PageResult<SearchBean> page = new PageResult<>(Collections.singletonList(bean), 1, 1, 2);
        Mockito.when(searchService.pageSearch(1, 2, "test", null)).thenReturn(page);

        mockMvc.perform(get("/td/search/page")
                        .param("pageNum", "1")
                        .param("pageSize", "2")
                        .param("keyword", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.pageNum").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(2))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value("1"));
    }

    @Test
    void testAudit() throws Exception {
        AuditRequest req = new AuditRequest()
                .setOperation(AuditStatusEnum.APPROVED)
                .setIds(java.util.Arrays.asList("1","2"))
                .setRemark("批量通过");
        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(req);

        mockMvc.perform(post("/td/search/audit")
                        .header("Authorization", "Bearer test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Mockito.verify(searchService).batchAudit(Mockito.anyList(), Mockito.eq(AuditStatusEnum.APPROVED), Mockito.eq("批量通过"));
    }
}

