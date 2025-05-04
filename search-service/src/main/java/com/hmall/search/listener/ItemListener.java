package com.hmall.search.listener;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.hmall.search.domain.po.Item;
import com.hmall.search.domain.po.ItemDoc;
import com.hmall.search.service.ISearchService;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHost;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ItemListener {
    RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(
            HttpHost.create("http://172.22.204.134:9200")
    ));

    private final ISearchService searchService;

    // 监听新增商品接口
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "item.save.queue", durable = "true"),
            exchange = @Exchange(name = "item.direct"),
            key = "item.save"
    ))
    public void listenerSaveItem(Long itemId) throws IOException {
        esUpdate(itemId);
    }

    // 监听更新商品状态接口
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "item.status.update.queue", durable = "true"),
            exchange = @Exchange(name = "item.direct"),
            key = "item.status.update"
    ))
    public void listenerUpdateItemStatus(Long itemId) throws IOException {
        esUpdate(itemId);
    }

    // 监听更新商品接口
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "item.update.queue", durable = "true"),
            exchange = @Exchange(name = "item.direct"),
            key = "item.update"
    ))
    public void listenerUpdateItem(Long itemId) throws IOException {
        esUpdate(itemId);
    }

    // 根据id删除商品
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "item.delete.queue", durable = "true"),
            exchange = @Exchange(name = "item.direct"),
            key = "item.delete"
    ))
    public void listenerDeleteItem(Long itemId) throws IOException {
        DeleteRequest request = new DeleteRequest("items", itemId.toString());
        client.delete(request, RequestOptions.DEFAULT);
    }

    // 批量减少库存
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "item.deduct.queue", durable = "true"),
            exchange = @Exchange(name = "item.direct"),
            key = "item.deduct.stock"
    ))
    public void listenerDeductStock(List<Long> itemIds) throws IOException {
        for (Long itemId : itemIds) {
            esUpdate(itemId);
        }
    }

    private void esUpdate (Long itemId) throws IOException {
            // 拿到id去查商品
            Item item = searchService.getById(itemId);
            ItemDoc itemDoc = BeanUtil.copyProperties(item, ItemDoc.class);

            // 1.准备Request
            IndexRequest request = new IndexRequest("items").id(item.getId().toString());
            // 2.准备请求参数
            request.source(JSONUtil.toJsonStr(itemDoc), XContentType.JSON);
            // 3.发送请求
            client.index(request, RequestOptions.DEFAULT);
        }

}

