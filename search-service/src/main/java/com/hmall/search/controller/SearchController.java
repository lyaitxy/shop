package com.hmall.search.controller;


import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.common.domain.PageDTO;
import com.hmall.search.domain.dto.ItemDTO;
import com.hmall.search.domain.po.Item;
import com.hmall.search.domain.po.ItemDoc;
import com.hmall.search.domain.query.ItemPageQuery;
import com.hmall.search.service.ISearchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.naming.directory.SearchResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Api(tags = "搜索相关接口")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final ISearchService itemService;
    RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(
            HttpHost.create("http://172.22.204.134:9200")
    ));

    @ApiOperation("搜索商品")
    @GetMapping("/list")
    public PageDTO<ItemDTO> search(ItemPageQuery query) throws IOException {
        log.info("查询商品,参数为:{}", query);
        // 在es中进行查询
        // 1.创建Request
        SearchRequest request = new SearchRequest("items");
        // 2.组织请求参数
        // 2.1 准备bool查询
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        // 2.2 关键字搜索
        if(StrUtil.isNotBlank(query.getKey())) {
            boolQuery.must(QueryBuilders.matchQuery("name", query.getKey()));
        }
        // 2.3 品牌,种类过滤
        if(StrUtil.isNotBlank(query.getBrand())) {
            boolQuery.filter(QueryBuilders.termQuery("brand", query.getBrand()));
        }
        if (StrUtil.isNotBlank(query.getCategory())) {
            boolQuery.filter(QueryBuilders.termQuery("category", query.getCategory()));
        }
        if(query.getMinPrice() != null && query.getMaxPrice() != null) {
            // 2.4 价格过滤
            boolQuery.filter(QueryBuilders.rangeQuery("price").lte(query.getMaxPrice()).gte(query.getMinPrice()));
        }
        // query条件
        request.source().query(boolQuery);
        // 排序
        request.source().sort("sold", SortOrder.DESC);
        // 分页
        request.source().from(query.from());

        // 3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 4.解析响应
        SearchHits searchHits = response.getHits();
        long total = searchHits.getTotalHits().value;
        long pages = total / query.getPageSize() + 1;
        List<ItemDTO> itemDTOList= new ArrayList<>();
        SearchHit[] hits = searchHits.getHits();
        for(SearchHit hit : hits) {
            String source = hit.getSourceAsString();
            itemDTOList.add(JSONUtil.toBean(source, ItemDTO.class));
        }
        return new PageDTO<>(total, pages, itemDTOList);
    }
}
