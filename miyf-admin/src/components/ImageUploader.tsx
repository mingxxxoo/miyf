import { useState } from 'react';
import { Upload, message, Image } from 'antd';
import { PlusOutlined, LoadingOutlined } from '@ant-design/icons';
import type { UploadRequestOption } from 'rc-upload/lib/interface';
import { uploadFile } from '@/api/upload';
import { notifyError } from '@/api/errors';

interface ImageUploaderProps {
  value?: string;
  onChange?: (url: string) => void;
  /** 产品应用编码，决定存储分区，如 kitchen / health */
  appCode: string;
  source?: string;
  /**
   * 访问权限。默认可不传（OWNER）。
   * 列表/详情展示依赖后端 `@FileAccess` 改写的签名 URL；仅当资源需永久匿名直链时再传 PUBLIC。
   */
  accessPermission?: 'PUBLIC' | 'AUTHENTICATED' | 'OWNER' | 'ADMIN' | 'DENY';
  maxSizeMB?: number;
  accept?: string;
}

/**
 * 图片选择器：上传至 /api/upload，失败明确报错（不再回退 DataURL）。
 */
export default function ImageUploader({
  value,
  onChange,
  appCode,
  source,
  accessPermission,
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
      const data = await uploadFile(file, { appCode, source, accessPermission });
      onChange?.(data.url);
      options.onSuccess?.(data);
      message.success('图片上传成功');
    } catch (err) {
      options.onError?.(err instanceof Error ? err : new Error('upload failed'));
      notifyError(err, '图片上传失败');
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
