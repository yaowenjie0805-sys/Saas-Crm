import { useState, useEffect, useCallback, useRef } from 'react';
import {
  List,
  Comment,
  Avatar,
  Input,
  Button,
  Space,
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
  TagOutlined,
  SendOutlined
} from '@ant-design/icons';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/zh-cn';
import { api } from '../../../shared';

dayjs.extend(relativeTime);
dayjs.locale('zh-cn');

const { TextArea } = Input;
const PAGE_SIZE = 20;
const REQUEST_CANCEL_ERRORS = new Set(['AbortError', 'CanceledError']);

const commentActionStyle = {
  cursor: 'pointer',
  display: 'inline-flex',
  alignItems: 'center',
  gap: 4
};

const replyCommentStyle = {
  marginLeft: 24,
  background: '#fafafa',
  padding: 12,
  borderRadius: 8
};

const commentBodyStyle = { lineHeight: 1.8 };
const replyBodyStyle = { lineHeight: 1.6 };
const inlineTagStyle = {
  background: '#e6f7ff',
  color: '#1890ff',
  padding: '0 6px',
  borderRadius: 4,
  margin: '0 2px'
};
const inlineTagStyleString = Object.entries(inlineTagStyle)
  .map(([key, value]) => `${key.replace(/[A-Z]/g, match => `-${match.toLowerCase()}`)}: ${value}`)
  .join('; ');

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

const isRequestCanceled = (error) => REQUEST_CANCEL_ERRORS.has(error?.name);

const escapeHtml = (text) => {
  if (!text) return '';

  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
};

const renderInlineFormatting = (content) => {
  if (!content) return null;

  const safe = escapeHtml(content);
  const withMentions = safe.replace(/@([a-zA-Z0-9_]+)/g, (_, username) => {
    return `<span style="color: #1890ff; cursor: pointer;">@${username}</span>`;
  });

  const withTags = withMentions.replace(/#([^#]+)#/g, (_, tag) => {
    return `<span style="${inlineTagStyleString}">#${tag}#</span>`;
  });

  return <span dangerouslySetInnerHTML={{ __html: withTags }} />;
};

const makeAction = (icon, label, onClick, extraStyle = {}) => (
  <span onClick={onClick} style={{ ...commentActionStyle, ...extraStyle }}>
    {icon}
    <span>{label}</span>
  </span>
);

const getInitial = (name) => name?.[0] || 'U';

const shouldShowError = (error) => !isRequestCanceled(error);

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
  const [newCommentContent, setNewCommentContent] = useState('');
  const [editContent, setEditContent] = useState('');
  const [replyContent, setReplyContent] = useState('');

  const abortControllerRef = useRef(null);
  const requestSeqRef = useRef(0);

  const loadStats = useCallback(async () => {
    if (!entityType || !entityId) return;

    try {
      const data = await api(`/v2/collaboration/entities/${entityType}/${entityId}/comments/stats`);
      if (data.success) {
        setStats(data.stats);
      }
    } catch (error) {
      if (shouldShowError(error)) {
        console.error('Failed to load stats:', error);
      }
    }
  }, [entityType, entityId]);

  const loadComments = useCallback(
    async (pageNum = 0, append = false) => {
      if (!entityType || !entityId) return;

      const requestId = ++requestSeqRef.current;

      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }

      const controller = new AbortController();
      abortControllerRef.current = controller;

      setLoading(true);

      try {
        const data = await api(
          `/v2/collaboration/entities/${entityType}/${entityId}/comments?page=${pageNum}&size=${PAGE_SIZE}&includeReplies=true`,
          { signal: controller.signal }
        );

        if (!data.success || requestSeqRef.current !== requestId) {
          return;
        }

        setComments(prev => (append ? [...prev, ...(data.comments || [])] : (data.comments || [])));
        setHasMore((pageNum + 1) * PAGE_SIZE < data.total);
        setPage(pageNum);

        if (showStats && data.total !== undefined) {
          loadStats();
        }
      } catch (error) {
        if (shouldShowError(error)) {
          console.error('Failed to load comments:', error);
        }
      } finally {
        if (requestSeqRef.current === requestId) {
          setLoading(false);
        }
      }
    },
    [entityType, entityId, loadStats, showStats]
  );

  useEffect(() => {
    loadComments();

    return () => {
      abortControllerRef.current?.abort();
    };
  }, [loadComments]);

  const postComment = useCallback(
    async (url, payload, successMessage, failureMessage, onSuccess) => {
      setSubmitting(true);

      try {
        const data = await api(url, {
          method: 'POST',
          body: JSON.stringify(payload)
        });

        if (data.success) {
          message.success(successMessage);
          onSuccess?.(data);
        } else {
          message.error(data.message || failureMessage);
        }

        return data;
      } catch {
        message.error(failureMessage);
        return null;
      } finally {
        setSubmitting(false);
      }
    },
    []
  );

  const handleSubmit = async (content) => {
    const trimmedContent = content?.trim();
    if (!trimmedContent) {
      message.warning('请输入评论内容');
      return;
    }

    await postComment(
      '/v2/collaboration/comments',
      {
        entityType,
        entityId,
        authorId: currentUser?.id,
        authorName: currentUser?.name,
        content
      },
      '评论成功',
      '评论失败',
      (data) => {
        setNewCommentContent('');
        loadComments();
        onCommentAdded?.(data.comment);
      }
    );
  };

  const handleReply = async (parentCommentId, content) => {
    const trimmedContent = content?.trim();
    if (!trimmedContent) {
      message.warning('请输入回复内容');
      return;
    }

    await postComment(
      `/v2/collaboration/comments/${parentCommentId}/reply`,
      {
        authorId: currentUser?.id,
        authorName: currentUser?.name,
        content
      },
      '回复成功',
      '回复失败',
      () => {
        setReplyContent('');
        setMentioning(null);
        loadComments();
      }
    );
  };

  const handleLike = async (commentId) => {
    try {
      const data = await api(`/v2/collaboration/comments/${commentId}/like`, {
        method: 'POST',
        body: JSON.stringify({ userId: currentUser?.id })
      });

      if (data.success) {
        setComments(prev =>
          prev.map(comment => {
            if (comment.id !== commentId) {
              return comment;
            }

            return {
              ...comment,
              likeCount: data.isLiked ? comment.likeCount + 1 : comment.likeCount - 1,
              isLiked: data.isLiked
            };
          })
        );
      }
    } catch {
      message.error('操作失败');
    }
  };

  const handleDelete = async (commentId) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除这条评论吗？',
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          const data = await api(`/v2/collaboration/comments/${commentId}`, {
            method: 'DELETE',
            body: JSON.stringify({ userId: currentUser?.id })
          });

          if (data.success) {
            message.success('删除成功');
            loadComments();
            onCommentDeleted?.(commentId);
          } else {
            message.error(data.message || '删除失败');
          }
        } catch {
          message.error('删除失败');
        }
      }
    });
  };

  const handleEdit = async (commentId, newContent) => {
    if (!newContent?.trim()) {
      message.warning('评论内容不能为空');
      return;
    }

    try {
      const data = await api(`/v2/collaboration/comments/${commentId}`, {
        method: 'PUT',
        body: JSON.stringify({
          userId: currentUser?.id,
          newContent
        })
      });

      if (data.success) {
        message.success('编辑成功');
        setEditingComment(null);
        setNewCommentContent('');
        loadComments();
      } else {
        message.error(data.message || '编辑失败');
      }
    } catch {
      message.error('编辑失败');
    }
  };

  const startEditing = (comment) => {
    setEditingComment(comment);
    setEditContent(comment.content);
  };

  const cancelEditing = () => {
    setEditingComment(null);
    setEditContent('');
  };

  const startReplying = (comment) => {
    setMentioning(comment.id);
    setReplyContent('');
  };

  const cancelReplying = () => {
    setMentioning(null);
    setReplyContent('');
  };

  const renderCommentActions = (comment, isReply = false) => {
    const items = [
      makeAction(
        comment.isLiked ? <LikeFilled style={{ color: '#eb2f96' }} /> : <LikeOutlined />,
        comment.likeCount || 0,
        () => handleLike(comment.id)
      )
    ];

    if (!isReply) {
      items.push(
        makeAction(<MessageOutlined />, '回复', () => startReplying(comment)),
        comment.authorId === currentUser?.id &&
          makeAction(<EditOutlined />, '编辑', () => startEditing(comment)),
        comment.authorId === currentUser?.id &&
          makeAction(<DeleteOutlined />, '删除', () => handleDelete(comment.id), { color: '#ff4d4f' })
      );
    }

    return items.filter(Boolean);
  };

  const renderComment = (comment) => (
    <Comment
      key={comment.id}
      actions={renderCommentActions(comment)}
      author={
        <Space>
          <span style={{ fontWeight: 500 }}>{comment.authorName}</span>
          {comment.editedAt && (
            <Tag color="default" style={{ fontSize: 10 }}>
              已编辑
            </Tag>
          )}
        </Space>
      }
      avatar={<Avatar style={{ backgroundColor: '#1890ff' }}>{getInitial(comment.authorName)}</Avatar>}
      content={
        editingComment?.id === comment.id ? (
          <div>
            <TextArea value={editContent} onChange={(e) => setEditContent(e.target.value)} rows={3} />
            <Space style={{ marginTop: 8 }}>
              <Button type="primary" size="small" onClick={() => handleEdit(comment.id, editContent)}>
                保存
              </Button>
              <Button size="small" onClick={cancelEditing}>
                取消
              </Button>
            </Space>
          </div>
        ) : (
          <div style={commentBodyStyle}>{renderInlineFormatting(comment.content)}</div>
        )
      }
      datetime={
        <Tooltip title={dayjs(comment.createdAt).format('YYYY-MM-DD HH:mm:ss')}>
          <span>{dayjs(comment.createdAt).fromNow()}</span>
        </Tooltip>
      }
    >
      {parseCommentTags(comment.tags).map(tag => (
        <Tag key={tag} color="blue" style={{ marginTop: 8 }}>
          <TagOutlined /> {tag}
        </Tag>
      ))}

      {comment.replies?.map(reply => (
        <Comment
          key={reply.id}
          style={replyCommentStyle}
          actions={renderCommentActions(reply, true)}
          author={<span style={{ fontWeight: 500 }}>{reply.authorName}</span>}
          avatar={<Avatar size="small" style={{ backgroundColor: '#52c41a' }}>{getInitial(reply.authorName)}</Avatar>}
          content={<div style={replyBodyStyle}>{renderInlineFormatting(reply.content)}</div>}
          datetime={<span>{dayjs(reply.createdAt).fromNow()}</span>}
        />
      ))}

      {mentioning === comment.id && (
        <div style={{ marginTop: 12, marginLeft: 24 }}>
          <TextArea
            value={replyContent}
            onChange={(e) => setReplyContent(e.target.value)}
            placeholder={`回复 @${comment.authorName}...（输入 @ 提及用户）`}
            rows={2}
          />
          <Space style={{ marginTop: 8 }}>
            <Button
              type="primary"
              size="small"
              icon={<SendOutlined />}
              loading={submitting}
              onClick={() => handleReply(comment.id, replyContent)}
            >
              发送
            </Button>
            <Button size="small" onClick={cancelReplying}>
              取消
            </Button>
          </Space>
        </div>
      )}
    </Comment>
  );

  return (
    <div className="collaboration-comment-section" style={{ padding: '0 16px' }}>
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

      <div style={{ marginBottom: 24 }}>
        <Comment
          avatar={<Avatar style={{ backgroundColor: '#1890ff' }}>{getInitial(currentUser?.name)}</Avatar>}
          content={
            <form
              onSubmit={(e) => {
                e.preventDefault();
                handleSubmit(newCommentContent);
              }}
            >
              <TextArea
                value={newCommentContent}
                onChange={(e) => setNewCommentContent(e.target.value)}
                rows={3}
                placeholder="发表评论...（使用 @ 提及用户，#标签# 添加标签）"
              />
              <div style={{ marginTop: 8, display: 'flex', justifyContent: 'flex-end' }}>
                <Button
                  type="primary"
                  htmlType="submit"
                  icon={<SendOutlined />}
                  loading={submitting}
                  disabled={!newCommentContent.trim()}
                >
                  发表
                </Button>
              </div>
            </form>
          }
        />
      </div>

      <Spin spinning={loading && page === 0}>
        {comments.length === 0 && !loading ? (
          <Empty description="暂无评论" style={{ margin: '40px 0' }} />
        ) : (
          <>
            <List dataSource={comments} renderItem={renderComment} locale={{ emptyText: '暂无评论' }} />

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
