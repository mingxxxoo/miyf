-- 菜品封面等展示图通过 @FileAccess 签名 URL（/r/{id}?exp=&sig=）访问，无需强制 PUBLIC。
-- HMAC 密钥：app.file-storage.access-sign-secret / FILE_STORAGE_ACCESS_SIGN_SECRET。
-- 本迁移为空操作（若此前已将 dish-cover 设为 PUBLIC，可继续保持，不影响签名方案）。

SELECT 1 WHERE FALSE;
