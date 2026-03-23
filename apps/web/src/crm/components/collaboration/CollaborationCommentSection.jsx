import { useState, useEffect, useCallback, useRef } from 'react';
import {
  List,
  Comment,
  Avatar,
  Input,
  Button,
  Space,
  Dropdown,
  Modal,
  message,
  Tag,
  Tooltip,
  Spin,
  Empty
} from 'antd';
import {
  LikeOutlined,
  LikeFilled,
  MessageOutlined,
  EditOutlined,
  DeleteOutlined,
  MoreOutlined,
  AtSignOutlined,
  TagOutlined,
  SendOutlined
} from '@ant-design/icons';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/zh-cn';

dayjs.extend(relativeTime);
dayjs.locale('zh-cn');

const { TextArea } = Input;

const parseCommentTags = (tags) => {
  if (!tags) {
    return [];
  }

  if (Array.isArray(tags)) {
    return tags;
  }

  try {
    const parsed = JSON.parse(tags);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
};

/**
 * 协作评论组件
 * 支持评论、回复、@提及、#标签#、点赞等功能
 */
export function CollaborationCommentSection({
  entityType,
  entityId,
  currentUser,
  onCommentAdded,
  onCommentDeleted,
  showStats = true
}) {
  const [comments, setComments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [mentioning, setMentioning] = useState(false);
  const [stats, setStats] = useState(null);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [editingComment, setEditingComment] = useState(null);
  const commentInputRef = useRef(null);

  const pageSize = 20;

  // 加载评论
  const loadComments = useCallback(async (pageNum = 0, append = false) => {
    if (!entityType || !entityId) return;

    setLoading(true);
    try {
      const response = await fetch(
        `/api/v2/collaboration/entities/${entityType}/${entityId}/comments?page=${pageNum}&size=${pageSize}&includeReplies=true`,
        {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
            'X-Tenant-Id': localStorage.getItem('tenantId')
          }
        }
      );
      const data = await response.json();

      if (data.success) {
        if (append) {
          setComments(prev => [...prev, ...data.comments]);
        } else {
          setComments(data.comments);
        }
        setHasMore((pageNum + 1) * pageSize < data.total);
        setPage(pageNum);

        if (showStats && data.total !== undefined) {
          loadStats();
        }
      }
    } catch (error) {
      console.error('Failed to load comments:', error);
    } finally {
      setLoading(false);
    }
  }, [entityType, entityId, showStats]);

  // 加载统计
  const loadStats = async () => {
    try {
      const response = await fetch(
        `/api/v2/collaboration/entities/${entityType}/${entityId}/comments/stats`,
        {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
            'X-Tenant-Id': localStorage.getItem('tenantId')
          }
        }
      );
      const data = await response.json();
      if (data.success) {
        setStats(data.stats);
      }
    } catch (error) {
      console.error('Failed to load stats:', error);
    }
  };

  useEffect(() => {
    loadComments();
  }, [loadComments]);

  // 提交评论
  const handleSubmit = async (values) => {
    if (!values.content?.trim()) {
      message.warning('请输入评论内容');
      return;
    }

    setSubmitting(true);
    try {
      const response = await fetch('/api/v2/collaboration/comments', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'X-Tenant-Id': localStorage.getItem('tenantId')
        },
        body: JSON.stringify({
          entityType,
          entityId,
          authorId: currentUser?.id,
          authorName: currentUser?.name,
          content: values.content
        })
      });

      const data = await response.json();
      if (data.success) {
        message.success('评论成功');
        loadComments();
        onCommentAdded?.(data.comment);
      } else {
        message.error(data.message || '评论失败');
      }
    } catch (error) {
      message.error('评论失败');
    } finally {
      setSubmitting(false);
    }
  };

  // 回复评论
  const handleReply = async (parentCommentId, content) => {
    if (!content?.trim()) {
      message.warning('请输入回复内容');
      return;
    }

    setSubmitting(true);
    try {
      const response = await fetch(`/api/v2/collaboration/comments/${parentCommentId}/reply`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'X-Tenant-Id': localStorage.getItem('tenantId')
        },
        body: JSON.stringify({
          authorId: currentUser?.id,
          authorName: currentUser?.name,
          content
        })
      });

      const data = await response.json();
      if (data.success) {
        message.success('回复成功');
        loadComments();
      } else {
        message.error(data.message || '回复失败');
      }
    } catch (error) {
      message.error('回复失败');
    } finally {
      setSubmitting(false);
    }
  };

  // 点赞评论
  const handleLike = async (commentId) => {
    try {
      const response = await fetch(
        `/api/v2/collaboration/comments/${commentId}/like?userId=${currentUser?.id}`,
        {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
            'X-Tenant-Id': localStorage.getItem('tenantId')
          }
        }
      );

      const data = await response.json();
      if (data.success) {
        // 更新本地状态
        setComments(prev => prev.map(c => {
          if (c.id === commentId) {
            return {
              ...c,
              likeCount: data.isLiked ? c.likeCount + 1 : c.likeCount - 1,
              isLiked: data.isLiked
            };
          }
          return c;
        }));
      }
    } catch (error) {
      message.error('操作失败');
    }
  };

  // 删除评论
  const handleDelete = async (commentId) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除这条评论吗？',
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          const response = await fetch(
            `/api/v2/collaboration/comments/${commentId}?userId=${currentUser?.id}`,
            {
              method: 'DELETE',
              headers: {
                'Authorization': `Bearer ${localStorage.getItem('token')}`,
                'X-Tenant-Id': localStorage.getItem('tenantId')
              }
            }
          );

          const data = await response.json();
          if (data.success) {
            message.success('删除成功');
            loadComments();
            onCommentDeleted?.(commentId);
          } else {
            message.error(data.message || '删除失败');
          }
        } catch (error) {
          message.error('删除失败');
        }
      }
    });
  };

  // 编辑评论
  const handleEdit = async (commentId, newContent) => {
    try {
      const response = await fetch(`/api/v2/collaboration/comments/${commentId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'X-Tenant-Id': localStorage.getItem('tenantId')
        },
        body: JSON.stringify({
          userId: currentUser?.id,
          newContent
        })
      });

      const data = await response.json();
      if (data.success) {
        message.success('编辑成功');
        setEditingComment(null);
        loadComments();
      } else {
        message.error(data.message || '编辑失败');
      }
    } catch (error) {
      message.error('编辑失败');
    }
  };

  // 解析评论内容，渲染@提及和#标签#
  const renderContent = (content) => {
    if (!content) return null;

    // 替换@提及
    let rendered = content.replace(/@([a-zA-Z0-9_]+)/g, (match, username) => {
      return `<span style="color: #1890ff; cursor: pointer;">@${username}</span>`;
    });

    // 替换#标签#
    rendered = rendered.replace(/#([^#]+)#/g, (match, tag) => {
      return `<span style="background: #e6f7ff; color: #1890ff; padding: 0 6px; border-radius: 4px; margin: 0 2px;">#${tag}#</span>`;
    });

    return <span dangerouslySetInnerHTML={{ __html: rendered }} />;
  };

  // 渲染单个评论
  const renderComment = (comment) => (
    <Comment
      key={comment.id}
      actions={[
        <span onClick={() => handleLike(comment.id)} style={{ cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: 4 }}>
          {comment.isLiked ? <LikeFilled style={{ color: '#eb2f96' }} /> : <LikeOutlined />}
          <span>{comment.likeCount || 0}</span>
        </span>,
        <span onClick={() => setMentioning(comment.id)} style={{ cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: 4 }}>
          <MessageOutlined />
          <span>回复</span>
        </span>,
        comment.authorId === currentUser?.id && (
          <span onClick={() => setEditingComment(comment)} style={{ cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: 4 }}>
            <EditOutlined />
            <span>编辑</span>
          </span>
        ),
        comment.authorId === currentUser?.id && (
          <span onClick={() => handleDelete(comment.id)} style={{ cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: 4, color: '#ff4d4f' }}>
            <DeleteOutlined />
            <span>删除</span>
          </span>
        )
      ]}
      author={
        <Space>
          <span style={{ fontWeight: 500 }}>{comment.authorName}</span>
          {comment.editedAt && <Tag color="default" style={{ fontSize: 10 }}>已编辑</Tag>}
        </Space>
      }
      avatar={<Avatar style={{ backgroundColor: '#1890ff' }}>{comment.authorName?.[0] || 'U'}</Avatar>}
      content={
        editingComment?.id === comment.id ? (
          <div>
            <TextArea
              defaultValue={comment.content}
              rows={3}
              id={`edit-${comment.id}`}
            />
            <Space style={{ marginTop: 8 }}>
              <Button
                type="primary"
                size="small"
                onClick={() => {
                  const newContent = document.getElementById(`edit-${comment.id}`).value;
                  handleEdit(comment.id, newContent);
                }}
              >
                保存
              </Button>
              <Button size="small" onClick={() => setEditingComment(null)}>
                取消
              </Button>
            </Space>
          </div>
        ) : (
          <div style={{ lineHeight: 1.8 }}>
            {renderContent(comment.content)}
          </div>
        )
      }
      datetime={
        <Tooltip title={dayjs(comment.createdAt).format('YYYY-MM-DD HH:mm:ss')}>
          <span>{dayjs(comment.createdAt).fromNow()}</span>
        </Tooltip>
      }
    >
      {/* 渲染标签 */}
      {parseCommentTags(comment.tags).map(tag => (
        <Tag key={tag} color="blue" style={{ marginTop: 8 }}>
          <TagOutlined /> {tag}
        </Tag>
      ))}

      {/* 渲染回复 */}
      {comment.replies?.map(reply => (
        <Comment
          key={reply.id}
          style={{ marginLeft: 24, background: '#fafafa', padding: 12, borderRadius: 8 }}
          actions={[
            <span onClick={() => handleLike(reply.id)} style={{ cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: 4 }}>
              {reply.isLiked ? <LikeFilled style={{ color: '#eb2f96' }} /> : <LikeOutlined />}
              <span>{reply.likeCount || 0}</span>
            </span>
          ]}
          author={<span style={{ fontWeight: 500 }}>{reply.authorName}</span>}
          avatar={<Avatar size="small" style={{ backgroundColor: '#52c41a' }}>{reply.authorName?.[0] || 'U'}</Avatar>}
          content={<div style={{ lineHeight: 1.6 }}>{renderContent(reply.content)}</div>}
          datetime={<span>{dayjs(reply.createdAt).fromNow()}</span>}
        />
      ))}

      {/* 回复输入框 */}
      {mentioning === comment.id && (
        <div style={{ marginTop: 12, marginLeft: 24 }}>
          <TextArea
            placeholder={`回复 @${comment.authorName}...（输入 @ 提及用户）`}
            rows={2}
            id={`reply-${comment.id}`}
          />
          <Space style={{ marginTop: 8 }}>
            <Button
              type="primary"
              size="small"
              icon={<SendOutlined />}
              loading={submitting}
              onClick={() => {
                const content = document.getElementById(`reply-${comment.id}`).value;
                handleReply(comment.id, content);
                setMentioning(false);
              }}
            >
              发送
            </Button>
            <Button size="small" onClick={() => setMentioning(false)}>
              取消
            </Button>
          </Space>
        </div>
      )}
    </Comment>
  );

  return (
    <div className="collaboration-comment-section" style={{ padding: '0 16px' }}>
      {/* 统计信息 */}
      {showStats && stats && (
        <div style={{ marginBottom: 16, padding: '12px 16px', background: '#fafafa', borderRadius: 8 }}>
          <Space size="large">
            <Tooltip title="总评论数">
              <Tag icon={<MessageOutlined />}>{stats.totalComments} 评论</Tag>
            </Tooltip>
            <Tooltip title="总点赞数">
              <Tag icon={<LikeOutlined />}>{stats.totalLikes} 点赞</Tag>
            </Tooltip>
            <Tooltip title="参与人数">
              <Tag>{stats.uniqueParticipants} 人参与</Tag>
            </Tooltip>
          </Space>
        </div>
      )}

      {/* 评论输入 */}
      <div style={{ marginBottom: 24 }}>
        <Comment
          avatar={<Avatar style={{ backgroundColor: '#1890ff' }}>{currentUser?.name?.[0] || 'U'}</Avatar>}
          content={
            <form onSubmit={(e) => {
              e.preventDefault();
              const content = commentInputRef.current?.resizableTextArea?.textArea?.value;
              if (content) {
                handleSubmit({ content });
                if (commentInputRef.current) {
                  commentInputRef.current.resizableTextArea.textArea.value = '';
                }
              }
            }}>
              <TextArea
                ref={commentInputRef}
                rows={3}
                placeholder="发表评论...（使用 @ 提及用户，#标签# 添加标签）"
              />
              <div style={{ marginTop: 8, display: 'flex', justifyContent: 'flex-end' }}>
                <Button
                  type="primary"
                  htmlType="submit"
                  icon={<SendOutlined />}
                  loading={submitting}
                >
                  发表
                </Button>
              </div>
            </form>
          }
        />
      </div>

      {/* 评论列表 */}
      <Spin spinning={loading && page === 0}>
        {comments.length === 0 && !loading ? (
          <Empty description="暂无评论" style={{ margin: '40px 0' }} />
        ) : (
          <>
            <List
              dataSource={comments}
              renderItem={renderComment}
              locale={{ emptyText: '暂无评论' }}
            />

            {hasMore && (
              <div style={{ textAlign: 'center', marginTop: 16 }}>
                <Button onClick={() => loadComments(page + 1, true)} loading={loading}>
                  加载更多
                </Button>
              </div>
            )}
          </>
        )}
      </Spin>
    </div>
  );
}

export default CollaborationCommentSection;
