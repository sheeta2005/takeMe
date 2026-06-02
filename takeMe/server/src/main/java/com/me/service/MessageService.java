package com.me.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.me.dto.PageQueryDTO;
import com.me.entity.Message;
import com.me.vo.MessageVO;
import com.me.vo.PageResultVO;

import java.util.List;

public interface MessageService {

    /**
     * 分页查询用户消息列表
     */
    PageResultVO<MessageVO> list(Long receiverId, Integer receiverType, Integer type, Integer isRead, PageQueryDTO pageQueryDTO);

    /**
     * 获取消息详情
     */
    MessageVO getById(Long id);

    /**
     * 标记消息为已读
     */
    void markRead(Long id);
    
    /**
     * 管理员分页查询消息列表
     * @param page 页码
     * @param pageSize 每页数量
     * @param receiverType 接收者类型
     * @param type 消息类型
     * @return 分页结果
     */
    Page<Message> getAdminMessagePage(Integer page, Integer pageSize, Integer receiverType, Integer type);
    
    /**
     * 管理员发送消息
     * @param message 消息对象
     * @return 是否成功
     */
    boolean sendMessage(Message message);
    
    /**
     * 管理员批量发送消息
     * @param message 消息模板
     * @param receiverIds 接收者ID列表
     * @return 发送数量
     */
    int sendBatchMessage(Message message, List<Long> receiverIds);
    
    /**
     * 删除消息
     * @param id 消息ID
     * @return 是否成功
     */
    boolean deleteMessage(Long id);
    
    /**
     * 统计消息数量
     * @param wrapper 查询条件
     * @return 消息数量
     */
    Long countMessages(LambdaQueryWrapper<Message> wrapper);
}