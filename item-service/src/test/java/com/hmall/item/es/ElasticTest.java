package com.hmall.item.es;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.common.utils.CollUtils;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.service.IItemService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

@SpringBootTest(properties = "spring.profiles.active=local")
@Slf4j
public class ElasticTest {

    private RestHighLevelClient client;
    @Autowired
    private IItemService itemService;
    @Test
    void testConnection() {
        System.out.println("client = " + client);
    }

    @Test
    void testIndexDoc() throws IOException {
        // 准备文档数据
        Item item = itemService.getById(100_00_0011127L);
        ItemDoc itemDoc = BeanUtil.copyProperties(item, ItemDoc.class);

        IndexRequest request = new IndexRequest("items").id(itemDoc.getId());
        request.source(JSONUtil.toJsonStr(itemDoc), XContentType.JSON);
        client.index(request, RequestOptions.DEFAULT);
    }

    @Test
    void testBulkDoc() throws IOException {
        // 分页查询商品数据
        int pageNo = 1;
        int size = 1000;
        while(true) {
            Page<Item> page = itemService.lambdaQuery().eq(Item::getStatus, 1).page(new Page<Item>(pageNo, size));
            // 非空检验
            List<Item> items = page.getRecords();
            if (CollUtils.isEmpty(items)) {
                return;
            }
            log.info("加载第{}页数据，共{}条", pageNo, items.size());
            // 1. 创建Request
            BulkRequest request = new BulkRequest("items");
            // 2. 准备参数，添加多个新增的Request
            for(Item item : items) {
                ItemDoc itemDoc = BeanUtil.copyProperties(item, ItemDoc.class);
                request.add(new IndexRequest().
                        id(itemDoc.getId())
                        .source(JSONUtil.toJsonStr(itemDoc), XContentType.JSON));
            }
            // 发送请求
            client.bulk(request, RequestOptions.DEFAULT);
            // 翻页
            pageNo++;
        }
    }

    @Test
    void testCreateIndex() throws IOException {
        // 1. 准备Request对象
        CreateIndexRequest request = new CreateIndexRequest("items");
        // 2. 准备请求参数
        request.source(MAPPING_TEMPLATE, XContentType.JSON);
        // 3. 发起请求
        client.indices().create(request, RequestOptions.DEFAULT);
    }

    @Test
    void testGetIndex() throws IOException {
        // 1. 准备Request对象
        GetIndexRequest items = new GetIndexRequest("items");
        // 2. 发起请求
        boolean exists = client.indices().exists(items, RequestOptions.DEFAULT);
        System.out.println("exists = " + exists);
    }

    @BeforeEach
    void setup() {
        client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://172.22.204.134:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        if (client != null) {
            client.close();
        }
    }

    private static final String MAPPING_TEMPLATE = "{\n" +
            "  \"mappings\": {\n" +
            "    \"properties\": {\n" +
            "      \"id\": {\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"name\":{\n" +
            "        \"type\": \"text\",\n" +
            "        \"analyzer\": \"ik_max_word\"\n" +
            "      },\n" +
            "      \"price\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"image\":{\n" +
            "        \"type\": \"keyword\",\n" +
            "        \"index\": false\n" +
            "      },\n" +
            "      \"category\":{\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"brand\":{\n" +
            "        \"type\": \"keyword\"\n" +
            "      },\n" +
            "      \"sold\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"commentCount\":{\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"isAD\":{\n" +
            "        \"type\": \"boolean\"\n" +
            "      },\n" +
            "      \"updateTime\":{\n" +
            "        \"type\": \"date\"\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";
}
