import { useState } from 'react';
import { Upload, message, Image } from 'antd';
import { PlusOutlined, LoadingOutlined } from '@ant-design/icons';
import type { UploadRequestOption } from 'rc-upload/lib/interface';
import { getToken } from '@/api/http';

interface ImageUploaderProps {
  value?: string;
  onChange?: (url: string) => void;
  maxSizeMB?: number;
  accept?: string;
}

/**
 * 图片选择器：上传至 /api/upload，失败明确报错（不再回退 DataURL）。
 */
export default function ImageUploader({
  value,
  onChange,
  maxSizeMB = 5,
  accept = 'image/jpeg,image/png,image/webp',
}: ImageUploaderProps) {
  const [loading, setLoading] = useState(false);
  const [previewOpen, setPreviewOpen] = useState(false);

  const beforeUpload = (file: File) => {
    const isValidType = accept.split(',').some((t) => file.type === t.trim());
    if (!isValidType) {
      message.error('仅支持 JPG、PNG、WebP 格式');
      return Upload.LIST_IGNORE;
    }
    const isValidSize = file.size / 1024 / 1024 < maxSizeMB;
    if (!isValidSize) {
      message.error(`图片大小不能超过 ${maxSizeMB}MB`);
      return Upload.LIST_IGNORE;
    }
    return true;
  };

  const customRequest = async (options: UploadRequestOption) => {
    const file = options.file as File;
    setLoading(true);
    try {
      const form = new FormData();
      form.append('file', file);
      const res = await fetch('/api/upload', {
        method: 'POST',
        headers: {
          ...(getToken() ? { Authorization: `Bearer ${getToken()}` } : {}),
        },
        body: form,
      });

      let body: {
        code?: number;
        message?: string;
        data?: { url?: string };
        url?: string;
      } = {};
      try {
        body = (await res.json()) as typeof body;
      } catch {
        body = {};
      }

      if (res.status === 401 || body.code === 40100) {
        const err = new Error(body.message || '未登录或登录已过期');
        options.onError?.(err);
        message.error(err.message);
        return;
      }

      if (!res.ok || (body.code != null && body.code !== 0)) {
        const err = new Error(body.message || `上传失败 (${res.status})`);
        options.onError?.(err);
        message.error(err.message);
        return;
      }

      const url = body.data?.url || body.url;
      if (!url) {
        const err = new Error('上传成功但未返回图片地址');
        options.onError?.(err);
        message.error(err.message);
        return;
      }

      onChange?.(url);
      options.onSuccess?.(body);
      message.success('图片上传成功');
    } catch (err) {
      options.onError?.(err instanceof Error ? err : new Error('upload failed'));
      message.error('图片上传失败');
    } finally {
      setLoading(false);
    }
  };

  const uploadButton = (
    <div>
      {loading ? <LoadingOutlined /> : <PlusOutlined />}
      <div style={{ marginTop: 8, color: 'var(--ck-muted)' }}>上传图片</div>
    </div>
  );

  return (
    <>
      <Upload
        name="file"
        listType="picture-card"
        showUploadList={false}
        accept={accept}
        beforeUpload={beforeUpload}
        customRequest={customRequest}
      >
        {value ? (
          <img
            src={value}
            alt="upload"
            style={{ width: '100%', height: '100%', objectFit: 'cover' }}
            onClick={(e) => {
              e.stopPropagation();
              setPreviewOpen(true);
            }}
          />
        ) : (
          uploadButton
        )}
      </Upload>
      {value && (
        <Image
          style={{ display: 'none' }}
          preview={{
            visible: previewOpen,
            onVisibleChange: (visible) => setPreviewOpen(visible),
          }}
          src={value}
        />
      )}
    </>
  );
}
